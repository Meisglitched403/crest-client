package com.crest.client.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MusicPlayer {
    public enum RepeatMode { OFF, ONE, ALL }

    private final AudioPlayerManager manager;
    private final AudioPlayer player;

    private volatile AudioTrack currentTrack;
    private volatile AudioPlaylist currentPlaylist;
    private volatile List<AudioTrack> queue = new ArrayList<>();
    private volatile int queueIndex = -1;
    private volatile boolean paused;
    private volatile float sliderVolume = 100f;

    private volatile RepeatMode repeatMode = RepeatMode.OFF;
    private volatile boolean shuffle = false;

    private Thread playbackThread;
    private final Object pauseLock = new Object();

    private Process audioProcess;
    private OutputStream audioOutput;
    private String audioBackend;
    private String audioBackendPath;
    private volatile boolean backendAvailable;
    private final Object outputLock = new Object();

    private volatile OnStateChange onTrackStart;
    private volatile OnStateChange onTrackEnd;
    private volatile OnStateChange onQueueChange;
    private volatile OnError onError;

    public interface OnStateChange {
        void run(MusicPlayer mp);
    }
    public interface OnError {
        void run(String msg);
    }
    public interface SearchCallback {
        void onResults(List<AudioTrack> tracks);
        void onError(String msg);
    }

    public MusicPlayer() {
        manager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(manager);
        AudioSourceManagers.registerLocalSource(manager);

        manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_LE);
        player = manager.createPlayer();
        player.setFrameBufferDuration(500);
        player.addListener(new AudioEventAdapter() {
            @Override
            public void onPlayerPause(AudioPlayer p) {
                paused = true;
            }

            @Override
            public void onPlayerResume(AudioPlayer p) {
                paused = false;
                synchronized (pauseLock) { pauseLock.notifyAll(); }
            }

            @Override
            public void onTrackStart(AudioPlayer p, AudioTrack track) {
                System.out.println("[Crest Music] Track started: " + track.getInfo().title + " by " + track.getInfo().author);
                currentTrack = track;
                paused = false;
                startPlaybackThread();
                if (onTrackStart != null) onTrackStart.run(MusicPlayer.this);
            }

            @Override
            public void onTrackEnd(AudioPlayer p, AudioTrack track, AudioTrackEndReason reason) {
                System.out.println("[Crest Music] Track ended: " + reason);
                currentTrack = null;
                stopPlaybackThread();
                if (reason == AudioTrackEndReason.FINISHED) {
                    // Auto-advance unless we're on repeat-one (handled below) or stopped.
                    if (repeatMode == RepeatMode.ONE && queueIndex >= 0) {
                        player.startTrack(queue.get(queueIndex).makeClone(), false);
                    } else {
                        boolean hadNext = next();
                        if (!hadNext && onTrackEnd != null) onTrackEnd.run(MusicPlayer.this);
                    }
                } else if (onTrackEnd != null) {
                    onTrackEnd.run(MusicPlayer.this);
                }
            }

            @Override
            public void onTrackException(AudioPlayer p, AudioTrack track, FriendlyException ex) {
                System.err.println("[Crest Music] Error playing " + track.getInfo().title + ": " + ex.getMessage());
                if (onError != null) onError.run(ex.getMessage());
            }

            @Override
            public void onTrackStuck(AudioPlayer p, AudioTrack track, long thresholdMs) {
                System.err.println("[Crest Music] Track stuck: " + track.getInfo().title);
                if (onError != null) onError.run("Track stuck: " + track.getInfo().title);
            }
        });

        selectBackend();
    }

    private void selectBackend() {
        String[] backends = {"paplay", "pw-play", "aplay", "ffplay"};
        String[][] args = {
            {"paplay", "--raw", "--rate=44100", "--channels=2", "--format=s16le"},
            {"pw-play", "--raw", "--rate=44100", "--channels=2", "--format=s16le"},
            {"aplay", "-f", "S16_LE", "-r", "44100", "-c", "2"},
            {"ffplay", "-f", "s16le", "-ar", "44100", "-ac", "2", "-nodisp", "-autoexit", "-"}
        };
        String[] candidates = {
            "/usr/bin/paplay", "/bin/paplay",
            "/usr/bin/pw-play", "/bin/pw-play",
            "/usr/bin/aplay", "/bin/aplay",
            "/usr/bin/ffplay", "/bin/ffplay"
        };
        for (int i = 0; i < backends.length; i++) {
            try {
                // Only run binaries from known absolute system locations to avoid
                // executing attacker-controlled binaries from the ambient PATH.
                String resolved = null;
                for (String c : candidates) {
                    if (c.endsWith("/" + backends[i]) && new java.io.File(c).canExecute()) {
                        resolved = c;
                        break;
                    }
                }
                if (resolved == null) continue;
                Process p = new ProcessBuilder(resolved, args[i][1], args[i][2], args[i][3], args[i][4], args[i][5]).start();
                if (p.isAlive()) {
                    p.destroy();
                    audioBackend = backends[i];
                    audioBackendPath = resolved;
                    backendAvailable = true;
                    System.out.println("[Crest Music] Using audio backend: " + audioBackend + " (" + resolved + ")");
                    return;
                }
            } catch (Exception e) {
                // try next backend
            }
        }
        System.err.println("[Crest Music] No audio backend found!");
        audioBackend = null;
        audioBackendPath = null;
        backendAvailable = false;
    }

    public boolean isBackendAvailable() { return backendAvailable; }

    public static boolean isUrlAllowed(String url) {
        if (url == null || url.isBlank()) return false;
        String trimmed = url.trim();
        // Only allow http/https remote URLs. Block file:, ftp:, and private/loopback hosts
        // to avoid local file disclosure and SSRF against internal services.
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false;
        try {
            java.net.URI uri = new java.net.URI(trimmed);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();
            if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")) return false;
            if (host.equals("0.0.0.0") || host.endsWith(".0.0.0.0")) return false;
            // Reject IPv4 private/loopback/link-local ranges.
            if (host.matches("\\d+(\\.\\d+){3}")) {
                String[] parts = host.split("\\.");
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                if (a == 10) return false;
                if (a == 127) return false;
                if (a == 169 && b == 254) return false;
                if (a == 172 && b >= 16 && b <= 31) return false;
                if (a == 192 && b == 168) return false;
            }
        } catch (java.net.URISyntaxException e) {
            return false;
        }
        return true;
    }

    public void search(String query, SearchCallback callback) {
        if (query == null || query.isBlank()) return;
        String sq = "scsearch:" + query;
        System.out.println("[Crest Music] Searching: " + sq);
        manager.loadItem(sq, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                callback.onResults(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                callback.onResults(playlist.getTracks());
            }

            @Override
            public void noMatches() {
                callback.onError("No results found for \"" + query + "\"");
            }

            @Override
            public void loadFailed(FriendlyException ex) {
                callback.onError("Search failed: " + ex.getMessage());
            }
        });
    }

    public void loadAndPlay(String url) {
        if (url == null || url.isBlank()) return;
        if (!url.contains("://") || !isUrlAllowed(url)) {
            System.err.println("[Crest Music] Refused to load disallowed URL: " + url);
            if (onError != null) onError.run("URL not allowed: " + url);
            return;
        }
        System.out.println("[Crest Music] Loading: " + url);
        manager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                System.out.println("[Crest Music] Track loaded: " + track.getInfo().title);
                playList(List.of(track), 0);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    System.out.println("[Crest Music] Playlist is empty");
                    if (onError != null) onError.run("Playlist is empty");
                    return;
                }
                System.out.println("[Crest Music] Playlist loaded, " + playlist.getTracks().size() + " tracks");
                currentPlaylist = playlist;
                playList(playlist.getTracks(), 0);
            }

            @Override
            public void noMatches() {
                System.out.println("[Crest Music] No matches for: " + url);
                if (onError != null) onError.run("No results for URL");
            }

            @Override
            public void loadFailed(FriendlyException ex) {
                System.err.println("[Crest Music] Load failed for " + url + ": " + ex.getMessage());
                if (onError != null) onError.run("Load failed: " + ex.getMessage());
            }
        });
    }

    /** Load all playable audio files from a local folder (absolute path). */
    public void loadLocalFolder(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) return;
        java.io.File dir = new java.io.File(folderPath);
        if (!dir.isDirectory()) {
            if (onError != null) onError.run("Not a folder: " + folderPath);
            return;
        }
        System.out.println("[Crest Music] Loading folder: " + folderPath);
        manager.loadItem("folder://" + folderPath, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                playList(List.of(track), 0);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    if (onError != null) onError.run("No audio files found in folder");
                    return;
                }
                currentPlaylist = playlist;
                playList(playlist.getTracks(), 0);
            }

            @Override
            public void noMatches() {
                if (onError != null) onError.run("No audio files found in folder");
            }

            @Override
            public void loadFailed(FriendlyException ex) {
                if (onError != null) onError.run("Folder load failed: " + ex.getMessage());
            }
        });
    }

    /** Replace the queue and start playing at the given index. */
    public void playList(List<AudioTrack> tracks, int startIndex) {
        if (tracks == null || tracks.isEmpty()) return;
        player.stopTrack();
        stopPlaybackThread();
        currentPlaylist = null;
        List<AudioTrack> list = new ArrayList<>(tracks);
        if (shuffle) shuffleQueue(list);
        queue = list;
        queueIndex = Math.max(0, Math.min(startIndex, list.size() - 1));
        player.startTrack(list.get(queueIndex), false);
        notifyQueueChange();
    }

    /** Append tracks to the end of the queue (does not interrupt current playback). */
    public void enqueue(List<AudioTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        List<AudioTrack> list = new ArrayList<>(queue);
        if (list.isEmpty()) {
            playList(tracks, 0);
            return;
        }
        list.addAll(tracks);
        queue = list;
        notifyQueueChange();
    }

    private void shuffleQueue(List<AudioTrack> list) {
        if (list.size() <= 1) return;
        Random r = new Random();
        for (int i = list.size() - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            AudioTrack t = list.get(i); list.set(i, list.get(j)); list.set(j, t);
        }
    }

    /** Advance to the next track in the queue. Returns false if nothing to play. */
    public boolean next() {
        if (queue.isEmpty()) return false;
        int n = queueIndex + 1;
        if (n >= queue.size()) {
            if (repeatMode == RepeatMode.ALL) n = 0;
            else return false;
        }
        queueIndex = n;
        player.startTrack(queue.get(queueIndex), false);
        notifyQueueChange();
        return true;
    }

    /** Go back to the previous track in the queue. Returns false if at start. */
    public boolean previous() {
        if (queue.isEmpty()) return false;
        int p = queueIndex - 1;
        if (p < 0) {
            if (repeatMode == RepeatMode.ALL) p = queue.size() - 1;
            else return false;
        }
        queueIndex = p;
        player.startTrack(queue.get(queueIndex), false);
        notifyQueueChange();
        return true;
    }

    /** Jump directly to a queue index and play it. */
    public void playQueueItem(int index) {
        if (index < 0 || index >= queue.size()) return;
        queueIndex = index;
        player.startTrack(queue.get(queueIndex), false);
        notifyQueueChange();
    }

    public List<AudioTrack> getQueue() { return queue; }
    public int getQueueIndex() { return queueIndex; }

    public void setRepeatMode(RepeatMode mode) { repeatMode = mode; }
    public RepeatMode getRepeatMode() { return repeatMode; }
    public void cycleRepeatMode() {
        repeatMode = switch (repeatMode) {
            case OFF -> RepeatMode.ALL;
            case ALL -> RepeatMode.ONE;
            case ONE -> RepeatMode.OFF;
        };
    }

    public void setShuffle(boolean s) {
        if (s == shuffle) return;
        shuffle = s;
        if (s && !queue.isEmpty()) {
            // Reshuffle keeping the current track first.
            AudioTrack cur = queueIndex >= 0 && queueIndex < queue.size() ? queue.get(queueIndex) : null;
            List<AudioTrack> rest = new ArrayList<>(queue);
            if (cur != null) rest.remove(cur);
            shuffleQueue(rest);
            if (cur != null) rest.add(0, cur);
            queue = rest;
            queueIndex = cur != null ? 0 : queueIndex;
            notifyQueueChange();
        }
    }
    public boolean isShuffle() { return shuffle; }

    private void notifyQueueChange() {
        if (onQueueChange != null) onQueueChange.run(this);
    }

    public void setOnQueueChange(OnStateChange cb) { onQueueChange = cb; }

    public void play() {
        if (currentTrack != null) {
            paused = false;
            player.setPaused(false);
        }
    }

    public void pause() {
        paused = true;
        player.setPaused(true);
    }

    public void stop() {
        paused = false;
        player.stopTrack();
        stopPlaybackThread();
        closeOutput();
    }

    public void togglePause() {
        if (currentTrack == null) return;
        if (paused) play();
        else pause();
    }

    public void seek(long positionMs) {
        if (currentTrack != null) {
            currentTrack.setPosition(positionMs);
        }
    }

    public boolean isPlaying() {
        return currentTrack != null && !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean hasTrack() {
        return currentTrack != null;
    }

    public AudioTrack getCurrentTrack() {
        return currentTrack;
    }

    public AudioPlaylist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public long getPosition() {
        return currentTrack != null ? currentTrack.getPosition() : 0;
    }

    public long getDuration() {
        return currentTrack != null ? currentTrack.getDuration() : 0;
    }

    public float getProgress() {
        long dur = getDuration();
        return dur > 0 ? (float) getPosition() / dur : 0;
    }

    public void setSliderVolume(float vol) {
        sliderVolume = Math.max(0, Math.min(100, vol));
        player.setVolume((int) sliderVolume);
    }

    public float getSliderVolume() {
        return sliderVolume;
    }

    public void setOnTrackStart(OnStateChange cb) { onTrackStart = cb; }
    public void setOnTrackEnd(OnStateChange cb) { onTrackEnd = cb; }
    public void setOnError(OnError cb) { onError = cb; }

    public void destroy() {
        stop();
        player.destroy();
    }

    private void startPlaybackThread() {
        stopPlaybackThread();
        playbackThread = new Thread(this::playbackLoop, "crest-music-playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void stopPlaybackThread() {
        Thread old = playbackThread;
        playbackThread = null;
        if (old != null) {
            old.interrupt();
            try { old.join(3000); } catch (InterruptedException ignored) {}
        }
        closeOutput();
    }

    private boolean openOutput() {
        synchronized (outputLock) {
            closeOutput();
            if (audioBackendPath == null) {
                System.err.println("[Crest Music] No audio backend available");
                return false;
            }
            try {
                String[][] cmds = {
                    {"--raw", "--rate=44100", "--channels=2", "--format=s16le"},
                    {"--raw", "--rate=44100", "--channels=2", "--format=s16le"},
                    {"-f", "S16_LE", "-r", "44100", "-c", "2"},
                    {"-f", "s16le", "-ar", "44100", "-ac", "2", "-nodisp", "-autoexit", "-"}
                };
                int idx = switch (audioBackend) {
                    case "paplay" -> 0;
                    case "pw-play" -> 1;
                    case "aplay" -> 2;
                    case "ffplay" -> 3;
                    default -> -1;
                };
                if (idx < 0) return false;
                String[] base = {audioBackendPath, cmds[idx][0], cmds[idx][1], cmds[idx][2], cmds[idx][3]};
                ProcessBuilder pb = new ProcessBuilder(base);
                pb.redirectErrorStream(false);
                audioProcess = pb.start();
                audioOutput = audioProcess.getOutputStream();
                System.out.println("[Crest Music] Audio output opened via " + audioBackend);
                return true;
            } catch (Exception e) {
                System.err.println("[Crest Music] Failed to open audio output: " + e);
                return false;
            }
        }
    }

    private void writeOutput(byte[] data) {
        synchronized (outputLock) {
            OutputStream out = audioOutput;
            if (out != null && data != null) {
                try {
                    out.write(data);
                } catch (Exception e) {
                    if (!Thread.interrupted()) {
                        System.err.println("[Crest Music] Write error: " + e);
                    }
                }
            }
        }
    }

    private void closeOutput() {
        synchronized (outputLock) {
            try {
                if (audioOutput != null) {
                    audioOutput.flush();
                    audioOutput.close();
                }
            } catch (Exception e) { }
            audioOutput = null;
            if (audioProcess != null) {
                try { audioProcess.destroy(); } catch (Exception e) { }
                audioProcess = null;
            }
        }
    }

    private void playbackLoop() {
        int frameCount = 0;
        try {
            if (!openOutput()) {
                System.err.println("[Crest Music] Failed to open audio output");
                return;
            }
            if (currentTrack != null) {
                System.out.println("[Crest Music] Playback thread started, track=" + currentTrack.getInfo().title
                    + ", duration=" + currentTrack.getDuration() + "ms");
            }

            long lastLog = 0;
            int nullCount = 0;

            while (!Thread.interrupted()) {
                if (player.isPaused()) {
                    synchronized (pauseLock) {
                        try { pauseLock.wait(100); } catch (InterruptedException e) { return; }
                    }
                    continue;
                }

                AudioFrame frame = player.provide();
                if (frame == null) {
                    nullCount++;
                    long now = System.currentTimeMillis();
                    if (now - lastLog > 2000) {
                        long pos = currentTrack != null ? currentTrack.getPosition() : 0;
                        System.out.println("[Crest Music] provide() null x" + nullCount
                            + ", pos=" + pos + "/" + (currentTrack != null ? currentTrack.getDuration() : 0)
                            + ", paused=" + player.isPaused()
                            + ", activeTrack=" + (player.getPlayingTrack() != null ? player.getPlayingTrack().getInfo().title : "null"));
                        lastLog = now;
                    }
                    try { Thread.sleep(5); } catch (InterruptedException e) { return; }
                    continue;
                }

                if (frame.isTerminator()) {
                    System.out.println("[Crest Music] Terminator frame received");
                    break;
                }

                byte[] data = frame.getData();
                if (data != null && data.length > 0) {
                    frameCount++;
                    if (frameCount == 1) {
                        AudioDataFormat fmt = frame.getFormat();
                        System.out.println("[Crest Music] First frame: " + data.length + " bytes, format="
                            + fmt.channelCount + "ch " + fmt.sampleRate + "Hz "
                            + fmt.codecName() + " chunk=" + fmt.chunkSampleCount);
                    }
                    writeOutput(data);
                }

                if (frameCount % 100 == 1 && frameCount > 1) {
                    long pos = currentTrack != null ? currentTrack.getPosition() : 0;
                    System.out.println("[Crest Music] Frames: " + frameCount + ", pos=" + pos + "ms");
                }
            }
        } catch (Exception e) {
            if (!(e instanceof InterruptedException)) {
                System.err.println("[Crest Music] Playback error: " + e);
                e.printStackTrace();
            }
        } finally {
            System.out.println("[Crest Music] Playback thread ending, frames played=" + frameCount);
            closeOutput();
        }
    }
}

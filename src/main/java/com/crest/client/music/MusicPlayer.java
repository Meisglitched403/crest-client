package com.crest.client.music;

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


public class MusicPlayer {
    private final AudioPlayerManager manager;
    private final AudioPlayer player;

    private volatile AudioTrack currentTrack;
    private volatile AudioPlaylist currentPlaylist;
    private volatile boolean paused;
    private volatile float sliderVolume = 100f;

    private Thread playbackThread;
    private final Object pauseLock = new Object();

    private Process audioProcess;
    private OutputStream audioOutput;
    private String audioBackend;

    private volatile OnStateChange onTrackStart;
    private volatile OnStateChange onTrackEnd;
    private volatile OnError onError;

    public interface OnStateChange {
        void run(MusicPlayer mp);
    }
    public interface OnError {
        void run(String msg);
    }

    public MusicPlayer() {
        manager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(manager);
        AudioSourceManagers.registerLocalSource(manager);

        manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_PCM_S16_LE);
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
                if (onTrackEnd != null) onTrackEnd.run(MusicPlayer.this);
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
            {"paplay", "--raw", "--rate=48000", "--channels=2", "--format=s16le"},
            {"pw-play", "--raw", "--rate=48000", "--channels=2", "--format=s16le"},
            {"aplay", "-f", "S16_LE", "-r", "48000", "-c", "2"},
            {"ffplay", "-f", "s16le", "-ar", "48000", "-ac", "2", "-nodisp", "-autoexit", "-"}
        };
        for (int i = 0; i < backends.length; i++) {
            try {
                Process p = new ProcessBuilder(args[i]).start();
                if (p.isAlive()) {
                    p.destroy();
                    audioBackend = backends[i];
                    System.out.println("[Crest Music] Using audio backend: " + audioBackend);
                    return;
                }
            } catch (Exception e) {
                // try next backend
            }
        }
        System.err.println("[Crest Music] No audio backend found!");
        audioBackend = null;
    }

    public void loadAndPlay(String url) {
        if (url == null || url.isBlank()) return;
        System.out.println("[Crest Music] Loading: " + url);
        manager.loadItem(url, new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                System.out.println("[Crest Music] Track loaded: " + track.getInfo().title);
                player.stopTrack();
                stopPlaybackThread();
                currentPlaylist = null;
                player.startTrack(track, false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    System.out.println("[Crest Music] Playlist is empty");
                    if (onError != null) onError.run("Playlist is empty");
                    return;
                }
                System.out.println("[Crest Music] Playlist loaded, first track: " + playlist.getTracks().get(0).getInfo().title);
                currentPlaylist = playlist;
                player.stopTrack();
                stopPlaybackThread();
                player.startTrack(playlist.getTracks().get(0), false);
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
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        closeOutput();
    }

    private boolean openOutput() {
        closeOutput();
        if (audioBackend == null) {
            System.err.println("[Crest Music] No audio backend available");
            return false;
        }
        try {
            String[][] cmds = {
                {"paplay", "--raw", "--rate=48000", "--channels=2", "--format=s16le"},
                {"pw-play", "--raw", "--rate=48000", "--channels=2", "--format=s16le"},
                {"aplay", "-f", "S16_LE", "-r", "48000", "-c", "2"},
                {"ffplay", "-f", "s16le", "-ar", "48000", "-ac", "2", "-nodisp", "-autoexit", "-"}
            };
            int idx = switch (audioBackend) {
                case "paplay" -> 0;
                case "pw-play" -> 1;
                case "aplay" -> 2;
                case "ffplay" -> 3;
                default -> -1;
            };
            if (idx < 0) return false;
            ProcessBuilder pb = new ProcessBuilder(cmds[idx]);
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

    private void writeOutput(byte[] data) {
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

    private void closeOutput() {
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

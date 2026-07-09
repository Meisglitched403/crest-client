package com.crest.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FifoStreamer {
    private static final Path VIDEO_FIFO = Path.of("/tmp/crest_video_fifo");

    private static VideoEncoderThread encoderThread;
    private static FileChannel fifoChannel;
    private static Process ffmpegProcess;
    private static Thread errorMonitor;

    public static void startStream(String url, int fps, int width, int height, int bitrate) throws IOException {
        start(fps, width, height, buildFfmpegArgs(url, fps, width, height, bitrate, true));
    }

    public static void startRecording(String filePath, int fps, int width, int height) throws IOException {
        int bitrate = Math.max(6000, width * height * 4 / 1000);
        start(fps, width, height, buildFfmpegArgs(filePath, fps, width, height, bitrate, false));
    }

    private static void start(int fps, int width, int height, List<String> ffmpegArgs) throws IOException {
        cleanupFifos();
        mkfifo(VIDEO_FIFO);

        ffmpegProcess = launchFfmpeg(ffmpegArgs);

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        fifoChannel = FileChannel.open(VIDEO_FIFO, StandardOpenOption.WRITE);

        FrameCapture.start(fps, width, height);

        encoderThread = new VideoEncoderThread(
            FrameCapture.getFilledQueue(),
            FrameCapture.getFreePool(),
            fifoChannel
        );
        encoderThread.start();
    }

    public static void stop() {
        FrameCapture.stop();

        if (encoderThread != null) {
            encoderThread.shutdown();
            try { encoderThread.join(3000); } catch (InterruptedException ignored) {}
            encoderThread = null;
        }

        if (fifoChannel != null) {
            try { fifoChannel.close(); } catch (IOException ignored) {}
            fifoChannel = null;
        }

        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
            try { ffmpegProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            if (ffmpegProcess.isAlive()) ffmpegProcess.destroyForcibly();
            ffmpegProcess = null;
        }

        if (errorMonitor != null) {
            try { errorMonitor.join(2000); } catch (InterruptedException ignored) {}
            errorMonitor = null;
        }

        cleanupFifos();
    }

    private static void mkfifo(Path path) throws IOException {
        Runtime.getRuntime().exec(new String[]{"mkfifo", path.toString()});
    }

    private static void cleanupFifos() {
        try { Files.deleteIfExists(VIDEO_FIFO); } catch (IOException ignored) {}
    }

    private static Process launchFfmpeg(List<String> args) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        Process p = pb.start();
        errorMonitor = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.err.println("[ffmpeg] " + line);
                }
            } catch (Exception ignored) {}
        }, "ffmpeg-stderr");
        errorMonitor.setDaemon(true);
        errorMonitor.start();
        return p;
    }

    private static List<String> buildFfmpegArgs(String output, int fps, int width, int height, int bitrate, boolean streaming) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-f"); cmd.add("rawvideo");
        cmd.add("-pix_fmt"); cmd.add("rgba");
        cmd.add("-s"); cmd.add(width + "x" + height);
        cmd.add("-r"); cmd.add(String.valueOf(fps));
        cmd.add("-i"); cmd.add(VIDEO_FIFO.toString());
        cmd.add("-f"); cmd.add("lavfi");
        cmd.add("-i"); cmd.add("anullsrc=r=44100:cl=stereo");
        cmd.add("-c:v"); cmd.add("libx264");
        cmd.add("-preset"); cmd.add("ultrafast");
        cmd.add("-pix_fmt"); cmd.add("yuv420p");
        cmd.add("-vf"); cmd.add("vflip");
        cmd.add("-g"); cmd.add(String.valueOf(fps * 2));
        if (streaming) {
            cmd.add("-b:v"); cmd.add(bitrate + "k");
            cmd.add("-maxrate"); cmd.add(bitrate + "k");
            cmd.add("-bufsize"); cmd.add((bitrate * 2) + "k");
        } else {
            cmd.add("-qp"); cmd.add("23");
        }
        cmd.add("-c:a"); cmd.add("aac");
        cmd.add("-b:a"); cmd.add("64k");
        cmd.add("-ar"); cmd.add("44100");
        cmd.add("-ac"); cmd.add("2");
        cmd.add("-shortest");
        if (streaming) {
            cmd.add("-f"); cmd.add("flv");
        } else {
            cmd.add("-f"); cmd.add("matroska");
        }
        cmd.add(output);
        return cmd;
    }
}

package com.crest.client.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ReplayBuffer {
    private static final int INTERNAL_W = 640;
    private static final int INTERNAL_H = 360;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int FRAME_SIZE = INTERNAL_W * INTERNAL_H * BYTES_PER_PIXEL;

    private static volatile boolean active;
    private static ByteBuffer[] ring;
    private static int capacity;
    private static int head;
    private static int count;
    private static int storeInterval = 2; // store every Nth frame
    private static int frameCounter;

    private static Thread saveThread;

    // ponytail: raw frames are handed off here from the render thread and
    // downscaled on a dedicated worker thread so the render thread never blocks
    // on the (CPU-heavy) bilinear downscale.
    private static final java.util.concurrent.ArrayBlockingQueue<RawFrame> pending =
        new java.util.concurrent.ArrayBlockingQueue<>(64);
    private static Thread downscaleThread;

    private static final class RawFrame {
        ByteBuffer src;
        int srcW, srcH;
    }

    public static void start(int sourceFps, int durationSec) {
        if (active) stop();
        storeInterval = Math.max(1, sourceFps / 30); // target ~30fps storage
        capacity = durationSec * sourceFps / storeInterval;
        if (capacity < 30) capacity = 30;
        if (capacity > 1800) capacity = 1800; // cap at 1800 frames (~14MB each = ~25GB)

        ring = new ByteBuffer[capacity];
        for (int i = 0; i < capacity; i++) {
            ring[i] = ByteBuffer.allocateDirect(FRAME_SIZE);
        }
        head = 0;
        count = 0;
        frameCounter = 0;
        active = true;

        downscaleThread = new Thread(() -> {
            while (active || !pending.isEmpty()) {
                try {
                    RawFrame f = pending.take();
                    ByteBuffer dst = ring[head];
                    dst.clear();
                    downscale(f.src, f.srcW, f.srcH, dst, INTERNAL_W, INTERNAL_H);
                    dst.flip();
                    head = (head + 1) % capacity;
                    if (count < capacity) count++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {}
            }
        }, "crest-replay-downscale");
        downscaleThread.setDaemon(true);
        downscaleThread.start();
    }

    public static void stop() {
        active = false;
        pending.clear();
        if (downscaleThread != null) {
            downscaleThread.interrupt();
            downscaleThread = null;
        }
        ring = null;
        capacity = 0;
    }

    public static boolean isActive() { return active; }

    public static void addFrame(ByteBuffer src, int srcW, int srcH) {
        if (!active) return;
        frameCounter++;
        if (frameCounter % storeInterval != 0) return;

        // ponytail: copy the frame data so the original buffer can be reused
        // immediately by the encoder or returned to the free pool. Without this
        // copy the downscale thread races with the encoder (read-vs-read) and
        // with the capture thread reusing the buffer for the next frame (read-vs-write).
        ByteBuffer copy = ByteBuffer.allocateDirect(src.limit());
        int pos = src.position();
        src.position(0);
        copy.put(src);
        copy.flip();
        src.position(pos);

        RawFrame f = new RawFrame();
        f.src = copy;
        f.srcW = srcW;
        f.srcH = srcH;
        if (!pending.offer(f)) {
            // queue full — drop this frame rather than block the render thread
        }
    }

    public static int getDurationSec() {
        return count * storeInterval / 30; // approximate
    }

    public static void saveAsync(String filePath, int sourceFps) {
        if (count < 15) return; // need at least 0.5s

        Thread t = new Thread(() -> saveSync(filePath, sourceFps), "replay-save");
        t.setDaemon(true);
        t.start();
        saveThread = t;
    }

    public static boolean isSaving() { return saveThread != null && saveThread.isAlive(); }

    private static void saveSync(String filePath, int sourceFps) {
        Path fifo = null;
        try {
            fifo = Files.createTempFile("replay_fifo_", ".raw");
            Files.delete(fifo);
            Runtime.getRuntime().exec(new String[]{"mkfifo", fifo.toString()}).waitFor(2, java.util.concurrent.TimeUnit.SECONDS);

            int actualFps = sourceFps / storeInterval;
            if (actualFps < 1) actualFps = 1;

            Process ffmpeg = new ProcessBuilder(
                "ffmpeg", "-y",
                "-f", "rawvideo",
                "-pix_fmt", "rgba",
                "-s", INTERNAL_W + "x" + INTERNAL_H,
                "-r", String.valueOf(actualFps),
                "-i", fifo.toString(),
                "-c:v", "libx264",
                "-preset", "fast",
                "-vf", "vflip,format=nv12",
                "-pix_fmt", "yuv420p",
                "-crf", "23",
                "-movflags", "+faststart",
                filePath
            ).start();

            try (FileChannel ch = FileChannel.open(fifo, StandardOpenOption.WRITE)) {
                int start = count < capacity ? 0 : head;
                int total = Math.min(count, capacity);
                for (int i = 0; i < total; i++) {
                    int idx = (start + i) % capacity;
                    ByteBuffer buf = ring[idx];
                    buf.position(0);
                    ch.write(buf);
                }
            }

            ffmpeg.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (ffmpeg.isAlive()) ffmpeg.destroyForcibly();
        } catch (Exception e) {
            System.err.println("[ReplayBuffer] Save failed: " + e);
        } finally {
            if (fifo != null) try { Files.deleteIfExists(fifo); } catch (Exception ignored) {}
        }
    }

    private static void downscale(ByteBuffer src, int srcW, int srcH,
                                    ByteBuffer dst, int dstW, int dstH) {
        float xRatio = (float) srcW / dstW;
        float yRatio = (float) srcH / dstH;
        int maxX = srcW - 1;
        int maxY = srcH - 1;

        for (int y = 0; y < dstH; y++) {
            int srcY = (int) (y * yRatio);
            int srcYNext = srcY < maxY ? srcY + 1 : srcY;
            float yFrac = (y * yRatio) - srcY;
            int rowY = srcY * srcW;
            int rowYNext = srcYNext * srcW;

            for (int x = 0; x < dstW; x++) {
                int srcX = (int) (x * xRatio);
                int srcXNext = srcX < maxX ? srcX + 1 : srcX;
                float xFrac = (x * xRatio) - srcX;

                int i00 = (rowY + srcX) * 4;
                int i10 = (rowY + srcXNext) * 4;
                int i01 = (rowYNext + srcX) * 4;
                int i11 = (rowYNext + srcXNext) * 4;

                // Indices are clamped above so no per-byte bounds check is needed.
                int r = bilerp(
                    src.get(i00) & 0xFF, src.get(i10) & 0xFF,
                    src.get(i01) & 0xFF, src.get(i11) & 0xFF, xFrac, yFrac);
                int g = bilerp(
                    src.get(i00 + 1) & 0xFF, src.get(i10 + 1) & 0xFF,
                    src.get(i01 + 1) & 0xFF, src.get(i11 + 1) & 0xFF, xFrac, yFrac);
                int b = bilerp(
                    src.get(i00 + 2) & 0xFF, src.get(i10 + 2) & 0xFF,
                    src.get(i01 + 2) & 0xFF, src.get(i11 + 2) & 0xFF, xFrac, yFrac);
                int a = bilerp(
                    src.get(i00 + 3) & 0xFF, src.get(i10 + 3) & 0xFF,
                    src.get(i01 + 3) & 0xFF, src.get(i11 + 3) & 0xFF, xFrac, yFrac);

                dst.put((byte) r);
                dst.put((byte) g);
                dst.put((byte) b);
                dst.put((byte) a);
            }
        }
    }

    private static int bilerp(int c00, int c10, int c01, int c11, float xf, float yf) {
        int top = (int) (c00 + (c10 - c00) * xf);
        int bot = (int) (c01 + (c11 - c01) * xf);
        return (int) (top + (bot - top) * yf);
    }
}

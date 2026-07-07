package com.crest.client.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder {
    private static final int STAGING_COUNT = 3;
    private static final AtomicBoolean recording = new AtomicBoolean(false);
    private static long startTimeMs;
    private static long lastCaptureNs;
    private static int captureIntervalNs;

    private static int fbWidth, fbHeight;
    private static GpuBuffer[] staging;
    private static final boolean[] stagingBusy = new boolean[STAGING_COUNT];
    private static final boolean[] stagingReady = new boolean[STAGING_COUNT];
    private static int stagingHead;

    private static RandomAccessFile fileOutput;
    private static FileChannel fileChannel;
    private static Thread writerThread;
    private static final BlockingQueue<FrameData> frameQueue = new LinkedBlockingQueue<>(120);
    private static volatile String currentFilePath;

    private record FrameData(long timestampMicros, byte[] pixels) {}

    public static boolean isRecording() { return recording.get(); }
    public static long getStartTime() { return startTimeMs; }
    public static String getCurrentFilePath() { return currentFilePath; }

    public static void start(int fps) {
        if (recording.get()) return;
        captureIntervalNs = 1_000_000_000 / fps;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        fbWidth = main.width;
        fbHeight = main.height;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String home = System.getProperty("user.home", ".");
        java.io.File dir = new java.io.File(home, "Videos");
        dir.mkdirs();
        currentFilePath = new java.io.File(dir, "crest-recording-" + timestamp + ".crest").getAbsolutePath();

        try {
            fileOutput = new RandomAccessFile(currentFilePath, "rw");
            fileChannel = fileOutput.getChannel();
            writeHeader(fps, fbWidth, fbHeight);
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
            return;
        }

        long bufSize = (long) fbWidth * fbHeight * 4;
        GpuDevice dev = RenderSystem.getDevice();
        if (dev == null) { cleanup(); return; }
        staging = new GpuBuffer[STAGING_COUNT];
        for (int i = 0; i < STAGING_COUNT; i++) {
            int idx = i;
            staging[i] = dev.createBuffer(() -> "CrestStaging" + idx,
                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, bufSize);
        }
        for (int i = 0; i < STAGING_COUNT; i++) stagingBusy[i] = false;
        for (int i = 0; i < STAGING_COUNT; i++) stagingReady[i] = false;
        stagingHead = 0;
        frameQueue.clear();

        writerThread = new Thread(Recorder::writerLoop, "Crest-Recorder");
        writerThread.setDaemon(true);
        writerThread.start();

        recording.set(true);
        startTimeMs = System.currentTimeMillis();
        lastCaptureNs = System.nanoTime();
    }

    private static void writeHeader(int fps, int w, int h) throws Exception {
        ByteBuffer header = ByteBuffer.allocate(20);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x43455243);
        header.putInt(2);
        header.putInt(fps);
        header.putInt(w);
        header.putInt(h);
        header.flip();
        fileChannel.write(header);
    }

    public static void onFrame() {
        if (!recording.get()) return;

        long now = System.nanoTime();
        if (now - lastCaptureNs < captureIntervalNs) return;
        lastCaptureNs = now;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;

        int w = main.width;
        int h = main.height;

        long requiredSize = (long) w * h * 4;
        if (w != fbWidth || h != fbHeight) {
            fbWidth = w;
            fbHeight = h;
            GpuDevice dev = RenderSystem.getDevice();
            if (dev == null) return;
            for (int i = 0; i < STAGING_COUNT; i++) {
                int idx = i;
                if (staging[idx] != null) staging[idx].close();
                staging[idx] = dev.createBuffer(() -> "CrestStaging" + idx,
                    GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, requiredSize);
            }
        }

        // Read any completed staging buffers
        for (int i = 0; i < STAGING_COUNT; i++) {
            if (stagingReady[i]) {
                stagingReady[i] = false;
                stagingBusy[i] = false;
                GpuDevice dev = RenderSystem.getDevice();
                if (dev == null) return;
                CommandEncoder enc2 = dev.createCommandEncoder();
                try {
                    GpuBuffer.MappedView view = enc2.mapBuffer(staging[i], true, false);
                    ByteBuffer data = view.data();
                    byte[] pixels = new byte[data.remaining()];
                    data.get(pixels);
                    view.close();

                    long elapsed = (System.currentTimeMillis() - startTimeMs) * 1000;
                    frameQueue.offer(new FrameData(elapsed, pixels));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Find a free buffer to submit the next copy
        int slot = -1;
        for (int i = 0; i < STAGING_COUNT; i++) {
            if (!stagingBusy[i] && !stagingReady[i]) { slot = i; break; }
        }
        if (slot == -1) return;

        GpuTexture mainTex = main.getColorTexture();
        if (mainTex == null) return;

        GpuDevice dev = RenderSystem.getDevice();
        if (dev == null) return;

        stagingBusy[slot] = true;
        CommandEncoder enc = dev.createCommandEncoder();
        try {
            int s = slot;
            enc.copyTextureToBuffer(mainTex, staging[s], 0, () -> {
                stagingReady[s] = true;
            }, 0, 0, 0, w, h);
        } catch (Exception e) {
            stagingBusy[slot] = false;
        }
    }

    private static void writerLoop() {
        byte[] prevFrame = null;
        int frameIndex = 0;
        try {
            while (recording.get() || !frameQueue.isEmpty()) {
                FrameData frame = frameQueue.poll(100, TimeUnit.MILLISECONDS);
                if (frame == null) continue;

                boolean keyframe = prevFrame == null || frameIndex % CrestCodec.KEYFRAME_INTERVAL == 0;
                byte[] compressed;
                if (keyframe) {
                    compressed = CrestCodec.compressKeyframe(frame.pixels);
                } else {
                    compressed = CrestCodec.compressDelta(prevFrame, frame.pixels);
                }

                byte flags = (byte) (keyframe ? 1 : 0);
                ByteBuffer buf = ByteBuffer.allocate(8 + 1 + 4 + compressed.length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.putLong(frame.timestampMicros);
                buf.put(flags);
                buf.putInt(compressed.length);
                buf.put(compressed);
                buf.flip();
                fileChannel.write(buf);

                prevFrame = frame.pixels;
                frameIndex++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        recording.set(false);
        if (writerThread != null) {
            try { writerThread.join(3000); } catch (InterruptedException ignored) {}
            writerThread = null;
        }
        cleanup();
    }

    private static void cleanup() {
        if (staging != null) {
            for (GpuBuffer buf : staging) {
                if (buf != null) try { buf.close(); } catch (Exception ignored) {}
            }
            staging = null;
        }
        for (int i = 0; i < STAGING_COUNT; i++) {
            stagingBusy[i] = false;
            stagingReady[i] = false;
        }
        try { if (fileChannel != null) fileChannel.close(); } catch (Exception ignored) {}
        try { if (fileOutput != null) fileOutput.close(); } catch (Exception ignored) {}
        fileOutput = null;
        fileChannel = null;
    }
}

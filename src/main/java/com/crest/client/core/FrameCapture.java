package com.crest.client.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FrameCapture {
    private static volatile boolean capturing;
    private static int targetFps;
    private static long lastCaptureNanos;

    // ponytail: pooled GPU staging buffer — allocated once per (w,h) instead of
    // every frame, since per-frame createBuffer()/close() was capping capture at ~53fps
    private static GpuBuffer stagingBuffer;
    private static int stagingW = -1;
    private static int stagingH = -1;

    // ponytail: 16 buffers — enough headroom for async GPU capture at 120fps without depleting free pool
    private static final ArrayBlockingQueue<ByteBuffer> freePool = new ArrayBlockingQueue<>(16);
    private static final ArrayBlockingQueue<ByteBuffer> filledQueue = new ArrayBlockingQueue<>(16);

    public static void start(int fps, int width, int height) {
        targetFps = fps;
        lastCaptureNanos = 0;
        int bufSize = width * height * 4;

        freePool.clear();
        filledQueue.clear();
        for (int i = 0; i < 16; i++) {
            freePool.offer(ByteBuffer.allocateDirect(bufSize));
        }

        capturing = true;
    }

    public static void stop() {
        capturing = false;
        if (stagingBuffer != null) {
            try { stagingBuffer.close(); } catch (Exception ignored) {}
            stagingBuffer = null;
            stagingW = -1;
            stagingH = -1;
        }
    }

    // ponytail: reuse a single GPU staging buffer across frames; only reallocate
    // when the capture resolution changes. Avoids per-frame GPU alloc (53fps cap).
    private static synchronized GpuBuffer getStagingBuffer(int w, int h, int bufSize) {
        if (stagingBuffer != null && stagingW == w && stagingH == h) {
            return stagingBuffer;
        }
        if (stagingBuffer != null) {
            try { stagingBuffer.close(); } catch (Exception ignored) {}
            stagingBuffer = null;
        }
        try {
            stagingBuffer = RenderSystem.getDevice().createBuffer(
                () -> "crest-capture", 9, bufSize);
            stagingW = w;
            stagingH = h;
        } catch (Exception e) {
            stagingBuffer = null;
        }
        return stagingBuffer;
    }

    public static boolean isCapturing() { return capturing; }
    public static ArrayBlockingQueue<ByteBuffer> getFilledQueue() { return filledQueue; }
    public static ArrayBlockingQueue<ByteBuffer> getFreePool() { return freePool; }

    public static void onRenderEnd() {
        if (!capturing) return;

        long now = System.nanoTime();
        // ponytail: ACFB — only capture when there is spare frame budget. Protects
        // gameplay FPS by skipping the capture/readback tax when the game is busy.
        int displayHz = 60;
        try {
            Minecraft mc = Minecraft.getInstance();
            int rr = mc.getWindow().getRefreshRate();
            if (rr > 0) displayHz = rr;
        } catch (Throwable ignored) {}
        if (!FrameBudget.shouldCapture(now, targetFps, displayHz)) return;
        lastCaptureNanos = now;

        ByteBuffer dst;
        try {
            // ponytail: wait up to 5ms for a free buffer instead of dropping immediately
            dst = freePool.poll(5, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (dst == null) {
            Streamer.addDropped();
            return;
        }
        dst.clear();

        try {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget target = mc.getMainRenderTarget();
            GpuTexture texture = target.getColorTexture();
            if (texture == null) {
                freePool.offer(dst);
                return;
            }

            int pixSize = texture.getFormat().pixelSize();
            int bufSize = target.width * target.height * pixSize;

            GpuBuffer staging = getStagingBuffer(target.width, target.height, bufSize);
            if (staging == null) {
                freePool.offer(dst);
                return;
            }

            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();

            enc.copyTextureToBuffer(texture, staging, 0L, () -> {
                try (var view = enc.mapBuffer(staging, true, false)) {
                    ByteBuffer src = view.data().slice(0, Math.min((int)staging.size(), dst.capacity()));
                    dst.put(src);
                    dst.flip();
                    Streamer.addCaptured();
                    ReplayBuffer.addFrame(dst, target.width, target.height);
                    // ponytail: blocking offer — wait up to 10ms for queue space instead of dropping
                    if (!filledQueue.offer(dst, 10, TimeUnit.MILLISECONDS)) {
                        Streamer.addDropped();
                        freePool.offer(dst);
                    }
                } catch (Exception e) {
                    freePool.offer(dst);
                }
            }, 0);
        } catch (Exception e) {
            freePool.offer(dst);
        }
    }
}

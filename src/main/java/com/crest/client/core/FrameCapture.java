package com.crest.client.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class FrameCapture {
    private static volatile boolean capturing;
    private static int targetFps;
    private static long lastCaptureNanos;

    private static final ArrayBlockingQueue<ByteBuffer> freePool = new ArrayBlockingQueue<>(4);
    private static final ArrayBlockingQueue<ByteBuffer> filledQueue = new ArrayBlockingQueue<>(4);

    public static void start(int fps, int width, int height) {
        targetFps = fps;
        lastCaptureNanos = 0;
        int bufSize = width * height * 4;

        freePool.clear();
        filledQueue.clear();
        for (int i = 0; i < 4; i++) {
            freePool.offer(ByteBuffer.allocateDirect(bufSize));
        }

        capturing = true;
    }

    public static void stop() {
        capturing = false;
    }

    public static boolean isCapturing() { return capturing; }
    public static ArrayBlockingQueue<ByteBuffer> getFilledQueue() { return filledQueue; }
    public static ArrayBlockingQueue<ByteBuffer> getFreePool() { return freePool; }

    public static void onRenderEnd() {
        if (!capturing) return;

        long now = System.nanoTime();
        if (now - lastCaptureNanos < 1_000_000_000L / targetFps) return;
        lastCaptureNanos = now;

        ByteBuffer dst = freePool.poll();
        if (dst == null) return;
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
            GpuBuffer staging = RenderSystem.getDevice().createBuffer(
                () -> "crest-capture", 9, bufSize);
            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();

            enc.copyTextureToBuffer(texture, staging, 0L, () -> {
                try (var view = enc.mapBuffer(staging, true, false)) {
                    ByteBuffer src = view.data().slice(0, Math.min((int)staging.size(), dst.capacity()));
                    dst.put(src);
                    dst.flip();
                    if (!filledQueue.offer(dst)) {
                        freePool.offer(dst);
                    }
                } catch (Exception e) {
                    freePool.offer(dst);
                } finally {
                    staging.close();
                }
            }, 0);
        } catch (Exception e) {
            freePool.offer(dst);
        }
    }
}

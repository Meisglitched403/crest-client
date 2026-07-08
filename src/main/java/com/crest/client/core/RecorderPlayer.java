package com.crest.client.core;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import com.crest.client.ui.Theme;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class RecorderPlayer extends Screen {
    private final String filePath;
    private int fps, width, height, frameCount, frameStride, version;
    private RandomAccessFile file;
    private FileChannel channel;
    private int currentFrame;
    private boolean paused;
    private DynamicTexture texture;
    private long playStartMs;
    private int frameAtPlayStart;
    private ByteBuffer frameBuf;
    private long[] frameOffsets;
    private byte[] previousFrame;

    public RecorderPlayer(String filePath) {
        super(Component.literal("Recording Player"));
        this.filePath = filePath;
        this.frameBuf = ByteBuffer.allocateDirect(8); // minimal, reallocated after header read
    }

    @Override
    protected void init() {
        loadHeader();
        if (width > 0 && height > 0) {
            texture = new DynamicTexture(() -> "crest-player", width, height, false);
            playStartMs = System.currentTimeMillis();
            previousFrame = null;
            if (version >= 2) {
                buildFrameIndex();
            }
            frameBuf = ByteBuffer.allocateDirect(width * height * 4);
        }
        loadFrame(0);
    }

    private void buildFrameIndex() {
        try {
            int pixSize = width * height * 4;
            int maxCompSize = pixSize + 4096;
            java.util.ArrayList<Long> offsets = new java.util.ArrayList<>();
            long pos = 20;
            while (pos < file.length()) {
                offsets.add(pos);
                channel.position(pos + 8);
                ByteBuffer meta = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
                int r = channel.read(meta);
                if (r < 5) break;
                meta.flip();
                meta.get();
                int compSize = meta.getInt();
                if (compSize <= 0 || compSize > maxCompSize) break;
                pos += 8 + 1 + 4 + compSize;
            }
            frameOffsets = new long[offsets.size()];
            for (int i = 0; i < offsets.size(); i++) frameOffsets[i] = offsets.get(i);
            frameCount = offsets.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadHeader() {
        try {
            file = new RandomAccessFile(filePath, "r");
            channel = file.getChannel();

            ByteBuffer hdr = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(hdr);
            hdr.flip();
            int magic = hdr.getInt();
            if (magic != 0x43455243) { close(); return; }
            version = hdr.getInt();
            fps = hdr.getInt();
            width = hdr.getInt();
            height = hdr.getInt();

            if (version < 2) {
                frameStride = 8 + width * height * 4;
                long dataSize = file.length() - 20;
                frameCount = (int) (dataSize / frameStride);
            }
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
    }

    private void loadFrame(int index) {
        if (channel == null || width <= 0 || height <= 0) return;
        int safe = Math.max(0, Math.min(index, frameCount - 1));
        if (safe != index) return;

        try {
            byte[] pixels;
            if (version >= 2) {
                long offset = frameOffsets[safe] + 8;
                channel.position(offset);
                ByteBuffer meta = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
                if (channel.read(meta) < 5) return;
                meta.flip();
                byte flags = meta.get();
                int compSize = meta.getInt();
                int pixSize = width * height * 4;
                if (compSize <= 0 || compSize > pixSize + 4096) return;
                byte[] compressed = new byte[compSize];
                if (channel.read(ByteBuffer.wrap(compressed)) < compSize) return;
                boolean keyframe = (flags & 1) != 0;
                pixels = CrestCodec.decompress(compressed, pixSize, keyframe ? null : previousFrame);
                if (pixels == null) return;
            } else {
                long offset = 20 + (long) safe * frameStride + 8;
                channel.position(offset);
                frameBuf.clear();
                int read = channel.read(frameBuf);
                if (read < width * height * 4) return;
                frameBuf.flip();
                pixels = new byte[width * height * 4];
                frameBuf.get(pixels);
            }

            previousFrame = pixels;

            if (texture != null) {
                frameBuf.clear();
                frameBuf.put(pixels);
                frameBuf.flip();
                GpuTexture tex = texture.getTexture();
                CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
                enc.writeToTexture(tex, frameBuf, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        if (paused || frameCount <= 0) return;

        long elapsed = System.currentTimeMillis() - playStartMs;
        int target = (int) (elapsed * fps / 1000L) + frameAtPlayStart;
        if (target >= frameCount) {
            target = frameCount - 1;
            paused = true;
        }
        if (target != currentFrame) {
            currentFrame = target;
            loadFrame(currentFrame);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        int accent = Theme.getAnimatedAccent();
        super.extractRenderState(g, mx, my, delta);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF000000);

        if (texture != null && frameCount > 0) {
            int gw = g.guiWidth();
            int gh = g.guiHeight();
            float scale = Math.min((float) gw / width, (float) gh / height);
            int vw = (int) (width * scale);
            int vh = (int) (height * scale);
            int vx = (gw - vw) / 2;
            int vy = (gh - vh) / 2;

            g.blit(texture.getTextureView(), texture.getSampler(), vx, vy, vx + vw, vy + vh, 0, 1, 1, 0);
        }

        if (frameCount > 0) {
            int barX = 10;
            int barW = g.guiWidth() - 20;
            int barY = g.guiHeight() - 50;

            g.fill(barX, barY, barX + barW, barY + 4, 0xFF333333);
            int fill = frameCount > 0 ? currentFrame * barW / frameCount : 0;
            g.fill(barX, barY, barX + fill, barY + 4, accent);

            String time = fmt(currentFrame) + " / " + fmt(frameCount - 1);
            String info = time + "  " + width + "x" + height + "  " + fps + " fps  [" + frameCount + " frames]";
            g.text(minecraft.font, Component.literal(info), 10, barY - 12, 0xFFAAAAAA);

            String ctrl = paused ? "SPACE: Play  ← →: Seek  ESC: Close" : "SPACE: Pause  ← →: Seek  ESC: Close";
            g.text(minecraft.font, Component.literal(ctrl), 10, barY - 27, 0xFF666666);
            g.text(minecraft.font, Component.literal("E: Export to MP4"), 10, barY - 42, 0xFF444488);
        }
    }

    private String fmt(int frame) {
        int s = fps > 0 ? frame / fps : 0;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_SPACE) {
            if (paused && currentFrame >= frameCount - 1) {
                currentFrame = 0;
                frameAtPlayStart = 0;
                playStartMs = System.currentTimeMillis();
            }
            paused = !paused;
            if (!paused) {
                frameAtPlayStart = currentFrame;
                playStartMs = System.currentTimeMillis();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            currentFrame = Math.min(currentFrame + (fps > 0 ? fps : 30), frameCount - 1);
            frameAtPlayStart = currentFrame;
            playStartMs = System.currentTimeMillis();
            paused = true;
            loadFrame(currentFrame);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            currentFrame = Math.max(currentFrame - (fps > 0 ? fps : 30), 0);
            frameAtPlayStart = currentFrame;
            playStartMs = System.currentTimeMillis();
            paused = true;
            loadFrame(currentFrame);
            return true;
        }
        if (key == GLFW.GLFW_KEY_E) {
            exportToMp4();
            return true;
        }
        return super.keyPressed(event);
    }

    private void exportToMp4() {
        new Thread(() -> RecorderExporter.export(filePath), "Crest-Export").start();
    }

    @Override
    public void onClose() {
        super.onClose();
        close();
    }

    private void close() {
        try { if (channel != null) channel.close(); } catch (Exception ignored) {}
        try { if (file != null) file.close(); } catch (Exception ignored) {}
        channel = null;
        file = null;
        previousFrame = null;
    }
}

package com.crest.client.core;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecorderExporter {
    public static void export(String crestPath) {
        int fps = 30, width = 0, height = 0, version = 1;

        try (RandomAccessFile file = new RandomAccessFile(crestPath, "r");
             FileChannel ch = file.getChannel()) {

            ByteBuffer hdr = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
            ch.read(hdr);
            hdr.flip();
            int magic = hdr.getInt();
            if (magic != 0x43455243) {
                System.err.println("[CrestExport] Invalid .crest file");
                return;
            }
            version = hdr.getInt();
            fps = hdr.getInt();
            width = hdr.getInt();
            height = hdr.getInt();

            System.out.println("[CrestExport] Exporting " + width + "x" + height + " @ " + fps + "fps");

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String home = System.getProperty("user.home", ".");
            String mp4Path = home + "/Videos/crest-export-" + timestamp + ".mp4";

            var cmd = new java.util.ArrayList<String>();
            cmd.add("ffmpeg");
            cmd.add("-y");
            cmd.add("-f"); cmd.add("rawvideo");
            cmd.add("-pix_fmt"); cmd.add("rgba");
            cmd.add("-s"); cmd.add(width + "x" + height);
            cmd.add("-r"); cmd.add(String.valueOf(fps));
            cmd.add("-i"); cmd.add("pipe:0");
            cmd.add("-vf"); cmd.add("vflip");
            cmd.add("-c:v"); cmd.add("libx264");
            cmd.add("-preset"); cmd.add("medium");
            cmd.add("-crf"); cmd.add("23");
            cmd.add("-pix_fmt"); cmd.add("yuv420p");
            cmd.add(mp4Path);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process ffmpeg = pb.start();
            OutputStream out = ffmpeg.getOutputStream();

            long lastLog = System.currentTimeMillis();

            if (version >= 2) {
                byte[] prevFrame = null;
                long pos = 20;
                int pixSize = width * height * 4;
                while (pos < file.length()) {
                    ch.position(pos);
                    ByteBuffer meta = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
                    if (ch.read(meta) < 13) break;
                    meta.flip();
                    meta.getLong();
                    byte flags = meta.get();
                    int compSize = meta.getInt();
                    byte[] compressed = new byte[compSize];
                    ch.read(ByteBuffer.wrap(compressed));
                    boolean keyframe = (flags & 1) != 0;
                    byte[] pixels = CrestCodec.decompress(compressed, pixSize, keyframe ? null : prevFrame);
                    out.write(pixels);
                    prevFrame = pixels;
                    pos += 8 + 1 + 4 + compSize;
                }
            } else {
                int frameStride = 8 + width * height * 4;
                long dataSize = file.length() - 20;
                int totalFrames = (int) (dataSize / frameStride);
                ByteBuffer frameBuf = ByteBuffer.allocateDirect(width * height * 4);
                for (int i = 0; i < totalFrames; i++) {
                    ch.position(20 + (long) i * frameStride + 8);
                    frameBuf.clear();
                    int read = ch.read(frameBuf);
                    if (read < width * height * 4) break;
                    frameBuf.flip();
                    byte[] pixels = new byte[width * height * 4];
                    frameBuf.get(pixels);
                    out.write(pixels);
                    long now = System.currentTimeMillis();
                    if (now - lastLog > 5000) {
                        System.out.println("[CrestExport] " + (i + 1) + "/" + totalFrames + " frames");
                        lastLog = now;
                    }
                }
            }

            out.flush();
            out.close();
            ffmpeg.waitFor();
            System.out.println("[CrestExport] Done: " + mp4Path);

        } catch (Exception e) {
            System.err.println("[CrestExport] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

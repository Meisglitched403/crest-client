package com.crest.client.core;

import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;

public class CrestCodec {
    public static final int KEYFRAME_INTERVAL = 60;

    public static byte[] compressKeyframe(byte[] pixels) {
        return deflate(pixels, 3, Deflater.DEFAULT_STRATEGY);
    }

    public static byte[] compressDelta(byte[] prev, byte[] curr) {
        byte[] delta = new byte[curr.length];
        for (int i = 0; i < curr.length; i++) {
            delta[i] = (byte) (curr[i] ^ prev[i]);
        }
        return deflate(delta, 1, Deflater.HUFFMAN_ONLY);
    }

    private static byte[] deflate(byte[] input, int level, int strategy) {
        Deflater def = new Deflater(level, true);
        def.setStrategy(strategy);
        def.setInput(input);
        def.finish();
        byte[] buf = new byte[Math.max(input.length / 4, 1024)];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
        while (!def.finished()) {
            int n = def.deflate(buf);
            if (n > 0) baos.write(buf, 0, n);
        }
        def.end();
        return baos.toByteArray();
    }

    public static byte[] decompress(byte[] compressed, int expectedSize, byte[] prev) {
        Inflater inf = new Inflater(true);
        inf.setInput(compressed);
        byte[] result = new byte[expectedSize];
        try {
            inf.inflate(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        inf.end();
        if (prev != null) {
            for (int i = 0; i < result.length; i++) {
                result[i] ^= prev[i];
            }
        }
        return result;
    }
}

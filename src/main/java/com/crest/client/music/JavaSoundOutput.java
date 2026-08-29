package com.crest.client.music;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Self-contained audio output using the JDK's javax.sound.sampled (Java Sound).
 * No system audio CLI and no separate OpenAL context required — it's part of the
 * JRE. SourceDataLine.write() blocks when the device buffer is full, which paces
 * playback to real time automatically, so consumption tracks real audio output.
 */
public class JavaSoundOutput {
    private final Object lock = new Object();
    private SourceDataLine line;
    private int sampleRate = 48000;
    private int channels = 2;
    private boolean ready;

    private SourceDataLine openLine(int rate, int ch) throws LineUnavailableException {
        AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        if (!AudioSystem.isLineSupported(info)) return null;
        SourceDataLine l = (SourceDataLine) AudioSystem.getLine(info);
        l.open(fmt);
        l.start();
        return l;
    }

    /** Probe whether a Java Sound output line can be opened. */
    public boolean init() {
        synchronized (lock) {
            if (line != null) return true;
            try {
                line = openLine(sampleRate, channels);
                ready = line != null;
                return ready;
            } catch (Throwable t) {
                System.err.println("[Crest Music] JavaSound init failed: " + t);
                line = null;
                ready = false;
                return false;
            }
        }
    }

    public boolean open() {
        synchronized (lock) {
            if (line == null) {
                try {
                    line = openLine(sampleRate, channels);
                } catch (Throwable t) {
                    System.err.println("[Crest Music] JavaSound open failed: " + t);
                    line = null;
                }
            }
            ready = line != null;
            return ready;
        }
    }

    public void write(byte[] data, int ch, int rate) {
        if (data == null || data.length == 0) return;
        synchronized (lock) {
            if (line == null) return;
            if (rate != sampleRate || ch != channels) {
                try { line.close(); } catch (Exception ignored) { }
                sampleRate = rate;
                channels = ch;
                try {
                    line = openLine(rate, ch);
                } catch (Throwable t) {
                    line = null;
                }
                if (line == null) return;
            }
            try {
                line.write(data, 0, data.length);
            } catch (Exception e) {
                if (!Thread.interrupted()) System.err.println("[Crest Music] JavaSound write error: " + e);
            }
        }
    }

    public void setVolume(float percent) {
        float gain = Math.max(0f, Math.min(100f, percent)) / 100f;
        synchronized (lock) {
            if (line == null) return;
            try {
                FloatControl c = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float min = c.getMinimum();
                float max = c.getMaximum();
                float db = (gain <= 0.0001f) ? min : (float) (Math.log10(gain) * 20.0);
                db = Math.max(min, Math.min(max, db));
                c.setValue(db);
            } catch (Exception ignored) { }
        }
    }

    public void flush() {
        synchronized (lock) {
            if (line != null) {
                try { line.flush(); } catch (Exception ignored) { }
            }
        }
    }

    public void close() {
        flush();
    }

    public void destroy() {
        synchronized (lock) {
            if (line != null) {
                try { line.stop(); line.close(); } catch (Exception ignored) { }
                line = null;
            }
            ready = false;
        }
    }
}

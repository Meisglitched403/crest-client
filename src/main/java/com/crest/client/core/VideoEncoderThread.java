package com.crest.client.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoEncoderThread extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ArrayBlockingQueue<ByteBuffer> filledQueue;
    private final ArrayBlockingQueue<ByteBuffer> freePool;
    private final FileChannel fifoChannel;

    public VideoEncoderThread(ArrayBlockingQueue<ByteBuffer> filledQueue,
                               ArrayBlockingQueue<ByteBuffer> freePool,
                               FileChannel fifoChannel) {
        super("crest-video-encoder");
        setDaemon(true);
        this.filledQueue = filledQueue;
        this.freePool = freePool;
        this.fifoChannel = fifoChannel;
    }

    public void shutdown() {
        running.set(false);
        this.interrupt();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                ByteBuffer buf = filledQueue.take();
                if (!running.get()) {
                    freePool.offer(buf);
                    break;
                }
                buf.position(0);
                fifoChannel.write(buf);
                Streamer.addEncoded();
                freePool.offer(buf);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running.get()) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}

package com.crest.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.UnboundProtocol;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records gameplay as STATE (network packets + client ticks + world meta) instead of
 * pixels. This is the gameplay half of the hybrid recorder: tiny, lossless, and
 * perfectly smooth on playback (re-simulated at display fps). GUI/HUD periods are
 * still captured as pixels by {@link Recorder}.
 *
 * Everything here is defensive: any failure is swallowed so recording can NEVER
 * crash the game or interfere with the pixel recorder.
 */
public class StateRecorder {
    private static final int TYPE_META = 0;
    private static final int TYPE_PKT_IN = 1;
    private static final int TYPE_PKT_OUT = 2;
    private static final int TYPE_TICK = 3;
    private static final int TYPE_KEY = 4;
    private static final int TYPE_MOUSE_BTN = 5;
    private static final int TYPE_MOUSE_SCROLL = 6;
    private static final int TYPE_MOUSE_MOVE = 7;

    private static final AtomicBoolean active = new AtomicBoolean(false);
    private static final AtomicBoolean metaWritten = new AtomicBoolean(false);
    private static final AtomicInteger currentTick = new AtomicInteger(0);

    private static long recordStartMs;
    private static RandomAccessFile file;
    private static FileChannel channel;
    private static Thread writerThread;
    private static final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    // ponytail: reuse scratch buffers instead of allocating a Netty buffer per
    // packet/event. The recorder is single-threaded (client thread), so reuse is safe.
    private static io.netty.buffer.ByteBuf scratchBacking;
    private static FriendlyByteBuf scratchFbb;
    private static ByteBuffer scratchRec;

    public static boolean isActive() { return active.get(); }

    public static void start(String crestPath) {
        if (active.getAndSet(true)) return;
        recordStartMs = System.currentTimeMillis();
        metaWritten.set(false);
        currentTick.set(0);
        queue.clear();

        String statePath = crestPath + ".state";
        try {
            file = new RandomAccessFile(statePath, "rw");
            channel = file.getChannel();
            writerThread = new Thread(StateRecorder::writerLoop, "Crest-StateRecorder");
            writerThread.setDaemon(true);
            writerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            active.set(false);
        }
    }

    public static void stop() {
        if (!active.getAndSet(false)) return;
        if (writerThread != null) {
            try { writerThread.join(3000); } catch (InterruptedException ignored) {}
            writerThread = null;
        }
        try { if (channel != null) channel.close(); } catch (Exception ignored) {}
        try { if (file != null) file.close(); } catch (Exception ignored) {}
        channel = null;
        file = null;
    }

    public static void onPacketIn(Packet<?> packet) {
        if (!active.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        byte[] enc = encodePacket(packet, true);
        enqueue(TYPE_PKT_IN, enc);
    }

    public static void onPacketOut(Packet<?> packet) {
        if (!active.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        byte[] enc = encodePacket(packet, false);
        enqueue(TYPE_PKT_OUT, enc);
    }

    public static void onTick() {
        if (!active.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (metaWritten.compareAndSet(false, true)) {
            Level level = mc.level;
            // This client version does not expose the world seed directly; it is only
            // needed for Phase 3 re-simulation (and will be read from the live world then).
            long seed = 0L;
            String dim = level.dimension().toString();
            UUID uuid = mc.player.getUUID();
            BlockPos p = mc.player.blockPosition();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            enqueue(TYPE_META, buildMeta(seed, dim.toString(), uuid, p.getX(), p.getY(), p.getZ(), w, h));
        }
        enqueue(TYPE_TICK, buildTick(currentTick.getAndIncrement()));
    }

    public static void onKey(int key, int scancode, int action, int modifiers) {
        if (!active.get()) return;
        byte[] enc = buildKey(key, scancode, action, modifiers);
        enqueue(TYPE_KEY, enc);
    }

    public static void onMouseButton(int button, int action, int mods) {
        if (!active.get()) return;
        byte[] enc = buildMouseButton(button, action, mods);
        enqueue(TYPE_MOUSE_BTN, enc);
    }

    public static void onMouseScroll(float horizontal, float vertical) {
        if (!active.get()) return;
        byte[] enc = buildMouseScroll(horizontal, vertical);
        enqueue(TYPE_MOUSE_SCROLL, enc);
    }

    public static void onMouseMove(float deltaX, float deltaY) {
        if (!active.get()) return;
        byte[] enc = buildMouseMove(deltaX, deltaY);
        enqueue(TYPE_MOUSE_MOVE, enc);
    }

    private static RegistryAccess liveRegistry() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        } catch (Throwable ignored) {
        }
        return RegistryAccess.EMPTY;
    }

    private static byte[] encodePacket(Packet<?> packet, boolean clientbound) {
        try {
            final RegistryAccess access = liveRegistry();
            ProtocolInfo<?> info;
            if (clientbound) {
                SimpleUnboundProtocol<?, RegistryFriendlyByteBuf> t = GameProtocols.CLIENTBOUND_TEMPLATE;
                info = t.bind(b -> new RegistryFriendlyByteBuf(b, access));
            } else {
                UnboundProtocol<?, RegistryFriendlyByteBuf, GameProtocols.Context> t = GameProtocols.SERVERBOUND_TEMPLATE;
                info = t.bind(b -> new RegistryFriendlyByteBuf(b, access), (GameProtocols.Context) () -> false);
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            StreamCodec<ByteBuf, Packet<?>> codec = (StreamCodec) info.codec();
            if (scratchBacking == null) scratchBacking = Unpooled.buffer();
            else scratchBacking.clear();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(scratchBacking, access);
            codec.encode(buf, packet);
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    // ponytail: write into the reused FriendlyByteBuf and copy out the readable
    // bytes into a freshly-sized byte[] (only the small fixed builders use this).
    private static byte[] buildFromScratch(java.util.function.Consumer<FriendlyByteBuf> writer) {
        if (scratchFbb == null) scratchFbb = new FriendlyByteBuf(Unpooled.buffer());
        else scratchFbb.clear();
        writer.accept(scratchFbb);
        byte[] out = new byte[scratchFbb.readableBytes()];
        scratchFbb.readBytes(out);
        return out;
    }

    private static byte[] buildMeta(long seed, String dim, UUID uuid, int x, int y, int z, int w, int h) {
        return buildFromScratch(b -> {
            b.writeLong(seed);
            b.writeUtf(dim, 32767);
            b.writeUUID(uuid);
            b.writeInt(x); b.writeInt(y); b.writeInt(z);
            b.writeInt(w); b.writeInt(h);
        });
    }

    private static byte[] buildKey(int key, int scancode, int action, int modifiers) {
        return buildFromScratch(b -> {
            b.writeInt(key);
            b.writeInt(scancode);
            b.writeInt(action);
            b.writeInt(modifiers);
        });
    }

    private static byte[] buildMouseButton(int button, int action, int mods) {
        return buildFromScratch(b -> {
            b.writeInt(button);
            b.writeInt(action);
            b.writeInt(mods);
        });
    }

    private static byte[] buildMouseScroll(float horizontal, float vertical) {
        return buildFromScratch(b -> {
            b.writeFloat(horizontal);
            b.writeFloat(vertical);
        });
    }

    private static byte[] buildMouseMove(float deltaX, float deltaY) {
        return buildFromScratch(b -> {
            b.writeFloat(deltaX);
            b.writeFloat(deltaY);
        });
    }

    private static byte[] buildTick(int tick) {
        return buildFromScratch(b -> b.writeInt(tick));
    }

    private static void enqueue(int type, byte[] payload) {
        if (payload == null || payload.length == 0) return;
        int total = 1 + 8 + 4 + payload.length;
        if (scratchRec == null || scratchRec.capacity() < total) {
            scratchRec = ByteBuffer.allocate(Math.max(total, 4096)).order(ByteOrder.LITTLE_ENDIAN);
        }
        scratchRec.clear();
        scratchRec.put((byte) type);
        scratchRec.putLong((System.currentTimeMillis() - recordStartMs) * 1000L);
        scratchRec.putInt(payload.length);
        scratchRec.put(payload);
        scratchRec.flip();
        byte[] out = new byte[scratchRec.remaining()];
        scratchRec.get(out);
        queue.offer(out);
    }

    private static void writerLoop() {
        try {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(0x43525331); // "CRS1"
            header.putInt(1);
            header.flip();
            channel.write(header);

            while (active.get() || !queue.isEmpty()) {
                byte[] rec = queue.poll(100, TimeUnit.MILLISECONDS);
                if (rec == null) continue;
                channel.write(ByteBuffer.wrap(rec));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (channel != null) channel.close(); } catch (Exception ignored) {}
            try { if (file != null) file.close(); } catch (Exception ignored) {}
        }
    }
}

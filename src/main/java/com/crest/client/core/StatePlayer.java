package com.crest.client.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.UnboundProtocol;
import net.minecraft.network.protocol.game.GameProtocols;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads a .crest.state sidecar and prepares records for replay re-simulation.
 *
 * Playback works by seeking to the target timestamp (driven by the replay clock in
 * a mixin) and yielding every record up to that point: packets injected into the
 * live ClientPacketListener, input events applied into KeyboardHandler/MouseHandler,
 * and ticks driving the client simulation forward in lockstep with the original recording.
 */
public class StatePlayer {
    public static final long MAGIC = 0x43525331L; // "CRS1"

    public static class Meta {
        public long seed;
        public String dimension;
        public UUID playerUuid;
        public int spawnX, spawnY, spawnZ;
        public int fbWidth, fbHeight;
    }

    public static class StateRecord {
        public int type;
        public long timestampUs;
        public byte[] payload;
    }

    private final List<StateRecord> records;
    private Meta meta;
    private int recordPos;

    public StatePlayer(String statePath) {
        records = new ArrayList<>();
        load(statePath);
    }

    public Meta meta() { return meta; }
    public int recordCount() { return records.size(); }

    public long lastTimestampUs() {
        if (records.isEmpty()) return 0;
        return records.get(records.size() - 1).timestampUs;
    }

    public boolean eof() {
        return recordPos >= records.size();
    }

    public void reset() {
        recordPos = 0;
    }

    /**
     * Returns all records whose timestamp is <= targetUs (inclusive), consuming them
     * from the internal cursor. Call once per replay frame. Returns empty list when
     * EOF is reached.
     */
    public List<StateRecord> pollUpTo(long targetUs) {
        List<StateRecord> due = new ArrayList<>();
        while (recordPos < records.size() && records.get(recordPos).timestampUs <= targetUs) {
            due.add(records.get(recordPos));
            recordPos++;
        }
        return due;
    }

    /**
     * Decode a clientbound packet from a record payload. Uses the same live codec
     * path as StateRecorder.encodePacket — only now in reverse. The caller must have
     * a valid RegistryAccess (obtained from mc.getConnection().registryAccess()).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Packet<?> decodeClientbound(byte[] payload, RegistryAccess access) {
        try {
            SimpleUnboundProtocol<?, RegistryFriendlyByteBuf> t = GameProtocols.CLIENTBOUND_TEMPLATE;
            ProtocolInfo<?> info = t.bind(b -> new RegistryFriendlyByteBuf(b, access));
            StreamCodec<ByteBuf, Packet<?>> codec = (StreamCodec) info.codec();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), access);
            Packet<?> pkt = codec.decode(buf);
            return pkt;
        } catch (Throwable t2) {
            return null;
        }
    }

    /**
     * Decode a serverbound packet from a record payload.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Packet<?> decodeServerbound(byte[] payload, RegistryAccess access) {
        try {
            UnboundProtocol<?, RegistryFriendlyByteBuf, GameProtocols.Context> t = GameProtocols.SERVERBOUND_TEMPLATE;
            ProtocolInfo<?> info = t.bind(b -> new RegistryFriendlyByteBuf(b, access), (GameProtocols.Context) () -> false);
            StreamCodec<ByteBuf, Packet<?>> codec = (StreamCodec) info.codec();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), access);
            Packet<?> pkt = codec.decode(buf);
            return pkt;
        } catch (Throwable t2) {
            return null;
        }
    }

    // --- Loading ---

    private void load(String statePath) {
        try (RandomAccessFile file = new RandomAccessFile(statePath, "r");
             FileChannel ch = file.getChannel()) {

            ByteBuffer hdr = ByteBuffer.allocate(8);
            hdr.order(ByteOrder.LITTLE_ENDIAN);
            ch.read(hdr);
            hdr.flip();
            long magic = hdr.getInt() & 0xFFFFFFFFL;
            if (magic != MAGIC) throw new Exception("Bad magic: " + magic);
            int version = hdr.getInt();

            while (ch.position() < ch.size()) {
                ByteBuffer recHdr = ByteBuffer.allocate(13);
                recHdr.order(ByteOrder.LITTLE_ENDIAN);
                int read = ch.read(recHdr);
                if (read < 13) break;
                recHdr.flip();
                int type = recHdr.get() & 0xFF;
                long ts = recHdr.getLong();
                int len = recHdr.getInt();

                ByteBuffer body = ByteBuffer.allocate(len);
                ch.read(body);
                body.flip();
                byte[] payload = new byte[len];
                body.get(payload);

                StateRecord rec = new StateRecord();
                rec.type = type;
                rec.timestampUs = ts;
                rec.payload = payload;
                records.add(rec);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
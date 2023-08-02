package io.th0rgal.oraxen.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.th0rgal.oraxen.protocol.packet.Packet;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ProtocolUtil {

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;
    private static String ENCODER_CONTEXT;

    public static void writePacket(Channel channel, Packet packet) {
        ChannelHandlerContext context =
                ENCODER_CONTEXT != null
                        ? channel.pipeline().context(ENCODER_CONTEXT)
                        : null;

        if (context == null) {
            Iterator<Map.Entry<String, ChannelHandler>> handlerIterator = channel.pipeline().iterator();
            do {
                if (!handlerIterator.hasNext()) {
                    throw new IllegalStateException("Could not find encoder handler.");
                }
            } while (!handlerIterator.next().getKey().equals("oraxen:encoder"));

            ENCODER_CONTEXT = handlerIterator.next().getKey();

            writePacket(channel, packet);
            return;
        }

        context.writeAndFlush(packet);
    }

    public static String readString(ByteBuf buf) {
        byte[] bytes = new byte[readVarInt(buf)];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static int readVarInt(ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = buf.readByte();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                buf.writeByte(value);
                return;
            }

            buf.writeByte((value & SEGMENT_BITS) | CONTINUE_BIT);
            value >>>= 7;
        }
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }
}

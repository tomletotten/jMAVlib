package me.drton.jmavlib.mavlink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkStream {
    private final MAVLinkSchema schema;
    private byte rxSeq = 0;
    private byte txSeq = 0;
    private ByteBuffer buffer = ByteBuffer.allocate(8192);

    public MAVLinkStream(MAVLinkSchema schema) {
        this.schema = schema;
        buffer.flip();
    }

    public ByteBuffer write(MAVLinkMessage msg) throws IOException {
        return msg.encode(txSeq++);
    }

    public void write(MAVLinkMessage msg, ByteChannel channel) throws IOException {
        channel.write(msg.encode(txSeq++));
    }

    public MAVLinkMessage read(ByteBuffer buffer) {
        while (buffer.remaining() >= 8) {
            // Got header, try to parse message
            buffer.mark();
            try {
                return new MAVLinkMessage(schema, buffer);
            } catch (MAVLinkProtocolException e) {
                // Message is completely corrupted, try to sync on the next byte
                buffer.reset();
                buffer.get();
            } catch (MAVLinkUnknownMessage mavLinkUnknownMessage) {
                // Message looks ok but with another protocol, skip it
                mavLinkUnknownMessage.printStackTrace();
            }
        }
        return null;
    }

    public MAVLinkMessage read(ByteChannel channel) throws IOException {
        buffer.compact();
        channel.read(buffer);
        buffer.flip();
        while (buffer.remaining() >= 8) {
            // Got header, try to parse message
            buffer.mark();
            try {
                return new MAVLinkMessage(schema, buffer);
            } catch (MAVLinkProtocolException e) {
                buffer.reset();
                buffer.get();
            } catch (MAVLinkUnknownMessage mavLinkUnknownMessage) {
                mavLinkUnknownMessage.printStackTrace();
            }
        }
        return null;
    }
}

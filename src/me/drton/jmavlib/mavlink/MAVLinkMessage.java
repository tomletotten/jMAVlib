package me.drton.jmavlib.mavlink;

import java.nio.ByteBuffer;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkMessage {
    private final static int MSG_ID_OFFSET = 5;
    public final static int DATA_OFFSET = 6;
    private final MAVLinkSchema schema;
    private final MAVLinkMessageDefinition definition;
    private int msgID;
    private final byte[] payload;
    private final ByteBuffer payloadBB;
    private byte sequence = 0;
    private int systemID = 0;
    private int componentID = 0;
    private int crc = -1;

    /**
     * Create empty message by message ID (for filling and sending)
     *
     * @param schema
     * @param msgID
     */
    public MAVLinkMessage(MAVLinkSchema schema, int msgID, int systemID, int componentID) {
        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgID);
        if (definition == null) {
            throw new RuntimeException("Unknown mavlink message ID: " + msgID);
        }
        this.payload = new byte[definition.payloadLength];
        this.payloadBB = ByteBuffer.wrap(payload);
        this.systemID = systemID;
        this.componentID = componentID;
        this.msgID = msgID;
    }

    /**
     * Create empty message by message name (for filling and sending)
     *
     * @param schema
     * @param msgName
     */
    public MAVLinkMessage(MAVLinkSchema schema, String msgName, int systemID, int componentID) {
        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgName);
        if (definition == null) {
            throw new RuntimeException("Unknown mavlink message name: " + msgName);
        }
        this.payload = new byte[definition.payloadLength];
        this.payloadBB = ByteBuffer.wrap(payload);
        this.systemID = systemID;
        this.componentID = componentID;
        this.msgID = definition.id;
    }

    /**
     * Create message from buffer (for parsing)
     *
     * @param schema
     */
    public MAVLinkMessage(MAVLinkSchema schema, ByteBuffer buffer)
            throws MAVLinkProtocolException, MAVLinkUnknownMessage {
        int startPos = buffer.position();
        byte startSign = buffer.get();
        if (startSign != schema.getStartSign()) {
            throw new MAVLinkProtocolException(
                    String.format("Invalid start sign: %02x, should be %02x", startSign, schema.getStartSign()));
        }
        int payloadLen = buffer.get() & 0xff;
        sequence = buffer.get();
        systemID = buffer.get() & 0xff;
        componentID = buffer.get() & 0xff;
        int msgID = buffer.get() & 0xff;
        this.schema = schema;
        this.definition = schema.getMessageDefinition(msgID);
        if (definition == null) {
            // Unknown message skip it
            buffer.position(buffer.position() + payloadLen + 2);
            throw new MAVLinkUnknownMessage(String.format("Unknown message: %02x", msgID));
        }
        if (payloadLen != definition.payloadLength) {
            throw new MAVLinkUnknownMessage(
                    String.format("Invalid payload len for msgID %s (%s): %02x, should be %02x", definition.name, msgID,
                            payloadLen, definition.payloadLength));
        }
        this.payload = new byte[definition.payloadLength];
        buffer.get(payload);
        crc = Short.reverseBytes(buffer.getShort()) & 0xffff;
        int endPos = buffer.position();
        buffer.position(startPos);
        int crcCalc = calculateCRC(buffer);
        buffer.position(endPos);
        if (crc != crcCalc) {
            throw new MAVLinkUnknownMessage(
                    String.format("CRC error for msgID %s (%s): %02x, should be %02x", definition.name, msgID, crc,
                            crcCalc));
        }
        this.payloadBB = ByteBuffer.wrap(payload);
    }

    public ByteBuffer encode(byte sequence) {
        this.sequence = sequence;
        ByteBuffer buf = ByteBuffer.allocate(payload.length + 8);
        buf.put(schema.getStartSign());
        buf.put((byte) definition.payloadLength);
        buf.put(sequence);
        buf.put((byte) systemID);
        buf.put((byte) componentID);
        buf.put((byte) msgID);
        buf.put(payload);
        buf.flip();
        crc = calculateCRC(buf);
        buf.limit(buf.capacity());
        buf.put((byte) crc);
        buf.put((byte) (crc >> 8));
        buf.flip();
        return buf;
    }

    /**
     * Calculate CRC of the message, buffer position should be set to start of the message.
     *
     * @param buf
     * @return CRC
     */
    private int calculateCRC(ByteBuffer buf) {
        buf.get();  // Skip start sign
        int c = 0xFFFF;
        for (int i = 0; i < definition.payloadLength + 5; i++) {
            c = MAVLinkCRC.accumulateCRC(buf.get(), c);
        }
        c = MAVLinkCRC.accumulateCRC(definition.extraCRC, c);
        return c;
    }

    public int getMsgType() {
        return definition.id;
    }

    public String getMsgName() {
        return definition.name;
    }

    public Object get(MAVLinkField field) {
        if (field.arraySize > 1) {
            Object[] res = new Object[field.arraySize];
            int offs = field.offset;
            for (int i = 0; i < field.arraySize; i++) {
                res[i] = getValue(field.type, offs);
                offs += field.type.size;
            }
            return res;
        } else {
            return getValue(field.type, field.offset);
        }
    }

    private Object getValue(MAVLinkDataType type, int offset) {
        switch (type) {
            case CHAR:
                return (char) payloadBB.get(offset);
            case UINT8:
                return payloadBB.get(offset) & 0xFF;
            case INT8:
                return (int) payloadBB.get(offset);
            case UINT16:
                return payloadBB.getShort(offset) & 0xFFFF;
            case INT16:
                return (int) payloadBB.getShort(offset);
            case UINT32:
                return payloadBB.getInt(offset) & 0xFFFFFFFFl;
            case INT32:
                return payloadBB.getInt(offset);
            case UINT64:
                return payloadBB.getLong(offset);
            case INT64:
                return payloadBB.getLong(offset);
            case FLOAT:
                return payloadBB.getFloat(offset);
            case DOUBLE:
                return payloadBB.getDouble(offset);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    public void set(MAVLinkField field, Object value) {
        switch (field.type) {
            case INT8:
            case UINT8:
                payloadBB.put(field.offset, ((Number) value).byteValue());
                break;
            case INT16:
            case UINT16:
                payloadBB.putShort(field.offset, ((Number) value).shortValue());
                break;
            case INT32:
            case UINT32:
                payloadBB.putInt(field.offset, ((Number) value).intValue());
                break;
            case FLOAT:
                payloadBB.putFloat(field.offset, ((Number) value).floatValue());
                break;
            default:
                throw new RuntimeException("Unknown type: " + field.type);
        }
    }

    public Object get(String fieldName) {
        return get(definition.fieldsByName.get(fieldName));
    }

    public void set(String fieldName, Object value) {
        set(definition.fieldsByName.get(fieldName), value);
    }

    public Object get(int fieldID) {
        return get(definition.fields[fieldID]);
    }

    public void set(int fieldID, Object value) {
        set(definition.fields[fieldID], value);
    }

    public int getInt(String fieldName) {
        return ((Number) get(fieldName)).intValue();
    }

    public int getInt(int fieldID) {
        return ((Number) get(fieldID)).intValue();
    }

    public long getLong(String fieldName) {
        return ((Number) get(fieldName)).longValue();
    }

    public long getLong(int fieldID) {
        return ((Number) get(fieldID)).longValue();
    }

    public float getFloat(String fieldName) {
        return ((Number) get(fieldName)).floatValue();
    }

    public float getFloat(int fieldID) {
        return ((Number) get(fieldID)).floatValue();
    }

    public double getDouble(String fieldName) {
        return ((Number) get(fieldName)).doubleValue();
    }

    public double getDouble(int fieldID) {
        return ((Number) get(fieldID)).doubleValue();
    }

    public String getString(int fieldID) {
        return new String((byte[]) get(fieldID));
    }

    public String getString(String fieldName) {
        return new String((byte[]) get(fieldName));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MAVLinkField field : definition.fields) {
            sb.append(field.name);
            sb.append("=");
            sb.append(get(field));
            sb.append(" ");
        }
        return String.format("<MAVLinkMessage %s seq=%s sysID=%s compID=%s ID=%s CRC=%04x %s/>", definition.name,
                sequence & 0xff, systemID, componentID, msgID, crc, sb.toString());
    }
}

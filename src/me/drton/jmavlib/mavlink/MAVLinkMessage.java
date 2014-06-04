package me.drton.jmavlib.mavlink;

import java.nio.ByteBuffer;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkMessage {
    private final static int MSG_ID_OFFSET = 5;
    public final static int DATA_OFFSET = 6;
    private final MAVLinkMessageDefinition definition;
    private final ByteBuffer data;

    /**
     * Create empty message by message ID (for filling and sending)
     *
     * @param schema
     * @param msgID
     */
    public MAVLinkMessage(MAVLinkSchema schema, int msgID, int systemID, int componentID) {
        this.definition = schema.getMessageDefinition(msgID);
        this.data = ByteBuffer.allocate(definition.length);
        writeHeader(systemID, componentID);
    }

    /**
     * Create empty message by message name (for filling and sending)
     *
     * @param schema
     * @param msgName
     */
    public MAVLinkMessage(MAVLinkSchema schema, String msgName, int systemID, int componentID) {
        this.definition = schema.getMessageDefinition(msgName);
        if (definition == null) {
            throw new RuntimeException("Unknown mavlink message: " + msgName);
        }
        this.data = ByteBuffer.allocate(definition.length + 8);
        writeHeader(systemID, componentID);
    }

    /**
     * Create message from buffer (for parsing)
     *
     * @param schema
     */
    public MAVLinkMessage(MAVLinkSchema schema, ByteBuffer buffer) {
        int msgID = buffer.get(MSG_ID_OFFSET);
        this.definition = schema.getMessageDefinition(msgID);
        this.data = buffer;
    }

    private void writeHeader(int systemID, int componentID) {
        data.put(0, definition.startSign);
        data.put(1, (byte) definition.length);
        data.put(3, (byte) systemID);
        data.put(4, (byte) componentID);
        data.put(5, (byte) definition.id);
    }

    public byte[] encode(byte sequence) {
        data.put(2, sequence);
        // Calculate CRC
        int crc = 0xFFFF;
        for (int i = 1; i < definition.length + 6; i++) {
            byte b = data.get(i);
            crc = MAVLinkCRC.accumulateCRC(b, crc);
        }
        crc = MAVLinkCRC.accumulateCRC(definition.extraCRC, crc);
        data.put(definition.length + 6, (byte) crc);
        data.put(definition.length + 7, (byte) (crc >> 8));
        return data.array();
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
            case INT8:
                return (int) data.get(offset);
            case UINT8:
                return data.get(offset) & 0xFF;
            case INT16:
                return (int) data.getShort(offset);
            case UINT16:
                return data.getShort(offset) & 0xFFFF;
            case INT32:
                return data.getInt(offset);
            case UINT32:
                return data.getInt(offset) & 0xFFFFFFFFl;
            case FLOAT:
                return data.getFloat(offset);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    public void set(MAVLinkField field, Object value) {
        switch (field.type) {
            case INT8:
            case UINT8:
                data.put(field.offset, ((Number) value).byteValue());
                break;
            case INT16:
            case UINT16:
                data.putShort(field.offset, ((Number) value).shortValue());
                break;
            case INT32:
            case UINT32:
                data.putInt(field.offset, ((Number) value).intValue());
                break;
            case FLOAT:
                data.putFloat(field.offset, ((Number) value).floatValue());
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
        return (Integer) get(fieldName);
    }

    public int getInt(int fieldID) {
        return (Integer) get(fieldID);
    }

    public long getLong(String fieldName) {
        return (Long) get(fieldName);
    }

    public long getLong(int fieldID) {
        return (Long) get(fieldID);
    }

    public float getFloat(String fieldName) {
        return (Float) get(fieldName);
    }

    public float getFloat(int fieldID) {
        return (Float) get(fieldID);
    }
}

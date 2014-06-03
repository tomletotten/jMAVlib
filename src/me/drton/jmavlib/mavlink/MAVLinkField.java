package me.drton.jmavlib.mavlink;

/**
 * User: ton Date: 03.06.14 Time: 12:54
 */
public class MAVLinkField {
    public final String name;
    public final MAVLinkDataType type;
    public final int offset;
    public final int size;
    public final int arraySize;

    public MAVLinkField(MAVLinkDataType type, String name, int offset) {
        this(type, 1, name, offset);
    }

    public MAVLinkField(MAVLinkDataType type, int arraySize, String name, int offset) {
        this.name = name;
        this.type = type;
        this.offset = offset;
        this.size = type.size * arraySize;
        this.arraySize = arraySize;
    }

    public boolean isArray() {
        return arraySize > 1;
    }
}

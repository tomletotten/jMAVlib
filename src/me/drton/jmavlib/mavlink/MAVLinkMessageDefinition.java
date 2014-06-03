package me.drton.jmavlib.mavlink;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.14 Time: 12:33
 */
public class MAVLinkMessageDefinition {
    public final byte startSign = (byte) 0xFE;
    public final int id;
    public final String name;
    public final byte extraCRC;
    public final Map<String, MAVLinkField> fieldsByName;
    public final MAVLinkField[] fields;
    public final int length;

    public MAVLinkMessageDefinition(int id, String name, MAVLinkField[] fields) {
        this.id = id;
        this.name = name;
        this.fields = fields;
        this.fieldsByName = new HashMap<String, MAVLinkField>(fields.length);
        int len = 0;
        for (MAVLinkField field : fields) {
            fieldsByName.put(field.name, field);
            len += field.size;
        }
        this.length = len;
        this.extraCRC = calculateExtraCRC();
    }

    private byte calculateExtraCRC() {
        String extraCrcBuffer = name + " ";
        MAVLinkField[] fieldsSorted = fields.clone();
        Arrays.sort(fieldsSorted, new Comparator<MAVLinkField>() {
            @Override
            public int compare(MAVLinkField field2, MAVLinkField field1) {
                // Sort on type size
                if (field1.size > field2.size) {
                    return 1;
                } else if (field1.size < field2.size) {
                    return -1;
                }
                return 0;
            }
        });
        for (MAVLinkField field : fieldsSorted) {
            extraCrcBuffer += field.type.ctype + " " + field.name + " ";
            // TODO arrays support
            //if (type.isArray) {
            //    extraCrcBuffer = extraCrcBuffer + (char) type.arrayLenth;
            //}
        }
        int extraCRCRaw = MAVLinkCRC.calculateCRC(extraCrcBuffer.getBytes(Charset.forName("latin1")));
        return (byte) ((extraCRCRaw & 0x00FF) ^ ((extraCRCRaw >> 8 & 0x00FF)));
    }
}

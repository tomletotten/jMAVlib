package me.drton.jmavlib.mavlink;

/**
 * User: ton Date: 03.06.14 Time: 15:35
 */
public class MAVLinkCRC {
    public final static int X25_INIT_CRC = 0xffff;

    /**
     * Accumulate the X.25 CRC by adding one char at a time. The checksum function adds the hash of one char at a time
     * to the 16 bit checksum
     *
     * @param data new char to hash
     * @param crc  the already accumulated checksum
     * @return the new accumulated checksum
     */
    public static int accumulateCRC(byte data, int crc) {
        // TODO optimize
        int tmp, tmpdata;
        int crcaccum = crc & 0x000000ff;
        tmpdata = data & 0x000000ff;
        tmp = tmpdata ^ crcaccum;
        tmp &= 0x000000ff;
        int tmp4 = tmp << 4;
        tmp4 &= 0x000000ff;
        tmp ^= tmp4;
        tmp &= 0x000000ff;
        int crch = crc >> 8;
        crch &= 0x0000ffff;
        int tmp8 = tmp << 8;
        tmp8 &= 0x0000ffff;
        int tmp3 = tmp << 3;
        tmp3 &= 0x0000ffff;
        tmp4 = tmp >> 4;
        tmp4 &= 0x0000ffff;
        int tmpa = crch ^ tmp8;
        tmpa &= 0x0000ffff;
        int tmpb = tmp3 ^ tmp4;
        tmpb &= 0x0000ffff;
        crc = tmpa ^ tmpb;
        crc &= 0x0000ffff;
        return crc;
    }

    public static int calculateCRC(byte[] data) {
        int crc = X25_INIT_CRC;
        for (byte b : data) {
            crc = accumulateCRC(b, crc);
        }
        return crc;
    }
}

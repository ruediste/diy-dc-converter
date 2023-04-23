package com.github.ruediste;

public class BlobMessage implements InterfaceMessage {
    @Datatype.array(0x8000) // 32k
    public byte[] data;

    public long readUint32(int idx) {
        long result = 0;
        for (int i = 3; i >= 0; i--) {
            result <<= 8;
            result |= ((long) data[idx + i]) & 0xFF;
        }
        return result;
    }

    public int readUint16(int idx) {
        int result = 0;
        for (int i = 1; i >= 0; i--) {
            result <<= 8;
            result |= ((long) data[idx + i]) & 0xFF;
        }
        return result;
    }
}

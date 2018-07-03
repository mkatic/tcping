package org.mkatic;

import java.nio.ByteBuffer;

class TcpingPacket {

    final int seqNumber;
    private long receivedTimestamp;
    final long sentTimestamp;

    TcpingPacket(final int sn, final long ts) {
        seqNumber = sn;
        sentTimestamp = ts;
    }

    long getReceivedTimestamp() {
        return receivedTimestamp;
    }

    void setReceivedTimestamp(long receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    //array is zero padded to packet size arg value
    final byte[] getBytes() {
        byte[] retVal = new byte[0];
        int padding = Cli.args.getPacketSize() - (Integer.BYTES + Long.BYTES + 4); //4 bytes for length prefix
        retVal = addAll(retVal, ByteBuffer.allocate(Integer.BYTES).putInt(Cli.args.getPacketSize()).array());
        retVal = addAll(retVal, ByteBuffer.allocate(Integer.BYTES).putInt(seqNumber).array());
        retVal = addAll(retVal, ByteBuffer.allocate(Long.BYTES).putLong(sentTimestamp).array());
        retVal = addAll(retVal, new byte[padding]);
        return retVal;
    }

    private byte[] addAll(byte[] array1, byte... array2) {
        byte[] joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }
}

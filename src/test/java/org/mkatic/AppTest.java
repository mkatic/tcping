package org.mkatic;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppTest {

    @Test
    public void testTcpingPacketArraySize() {
        int packetSize = 50;
        Cli.args.init(new String[]{"-c", "-port", "44444", "-size ", Integer.toString(packetSize)});
        TcpingPacket tp = new TcpingPacket(1, System.currentTimeMillis());
        assertTrue(tp.getBytes().length == packetSize);
    }
}

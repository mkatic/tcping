package org.mkatic;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import static java.math.RoundingMode.HALF_UP;

class Pitcher {

    private final ExecutorService executor;
    private final SynchronousQueue<TcpingPacket> responseQ;

    Pitcher() {
        responseQ = new SynchronousQueue<>();
        executor = Executors.newSingleThreadExecutor();
    }

    void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        LengthFieldBasedFrameDecoder decoder = new LengthFieldBasedFrameDecoder(Cli.args.getPacketSize(), 0, 4, -4, 4);
        InetSocketAddress isa = new InetSocketAddress(Cli.args.getBindAddress(), Cli.args.getPort());
        try {
            Bootstrap b = new Bootstrap();
            b.group(group);
            b.channel(NioSocketChannel.class);
            b.remoteAddress(isa);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addFirst(decoder);
                    ch.pipeline().addLast(new PitcherChannelHandler());
                }
            });
            b.option(ChannelOption.TCP_NODELAY, true);
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    private class PitcherChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            System.out.println("Catcher closed connection, exit");
            System.exit(0);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("Pitcher connected to catcher on: " + ctx.channel().remoteAddress() + ", starting ping spammer");
            executor.execute(new PingSpammer(ctx));
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf in) {
            TcpingPacket tp = new TcpingPacket(in.readInt(), in.readLong());
            try {
                responseQ.offer(tp, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.exit(-1);
        }
    }

    /**
     * Implementation of one way delay calculations is trivial on purpose.
     * One way delay measurements are notoriously difficult to do, if not even
     * impossible to do without extra hardware, like external clocks. And even
     * more so if we only have control of endpoints.
     *
     * One might try to sync catcher and pitcher via NTP, but
     * this will probably not work well in all situations, especially if you're dealing
     * with asymmetric latencies.
     */
    private class PingSpammer implements Runnable {

        private final ChannelHandlerContext ctx;
        private final List<TcpingPacket> receivedPacketList;

        private PingSpammer(final ChannelHandlerContext chc) {
            receivedPacketList = new ArrayList<>();
            ctx = chc;
        }

        @Override
        public void run() {
            int seqNo = 0;
            int sentCounter = 0;
            long loopStart = System.currentTimeMillis();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if ((System.currentTimeMillis() - loopStart) <= 1000) {

                        //Just busy wait when we hit the requested mps
                        if (receivedPacketList.size() >= Cli.args.getMps()) {
                            continue;
                        }

                        TcpingPacket requestOrResponse = new TcpingPacket(seqNo, System.currentTimeMillis());
                        ChannelFuture cf = ctx.writeAndFlush(Unpooled.copiedBuffer(requestOrResponse.getBytes()));
                        cf.syncUninterruptibly();
                        requestOrResponse = responseQ.poll(10, TimeUnit.SECONDS);
                        if (requestOrResponse == null) {
                            System.out.println("No response for packet: " + seqNo);
                        } else {
                            requestOrResponse.setReceivedTimestamp(System.currentTimeMillis());
                            receivedPacketList.add(requestOrResponse);
                            sentCounter++;
                        }
                        seqNo++;
                    } else { //Print stats, update loopCounter
                        List<Long> rttList = new ArrayList<>();
                        for (TcpingPacket tp : receivedPacketList) {
                            long delta = loopStart - tp.getReceivedTimestamp();
                            if ((delta <= 1000)) {
                                rttList.add(tp.getReceivedTimestamp() - tp.sentTimestamp);
                            }
                        }

                        long rttMax = rttList.stream().mapToLong(value -> value).max().orElse(0);
                        double rttAvg = rttList.stream().mapToDouble(value -> value).average().orElse(0.0);
                        double rttAvgSym = rttAvg / 2;

                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                        LocalDateTime time = LocalDateTime.now();

                        System.out.printf("%s |Total received: %8d|msg/s %6d|max RTT: %4dms|avg RTT: %4.2fms|avg RTT AB: %4.2fms|avg RTT BA: %4.2fms\n",
                                dtf.format(time), sentCounter, rttList.size(), rttMax, BigDecimal.valueOf(rttAvg).setScale(2, HALF_UP), rttAvgSym, rttAvgSym);
                        receivedPacketList.clear();
                        loopStart = System.currentTimeMillis();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }
}

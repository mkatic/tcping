package org.mkatic;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

enum Catcher {

  instance;

  public void start() throws InterruptedException {

    EventLoopGroup group = new NioEventLoopGroup();
    final InetSocketAddress isa = new InetSocketAddress(Cli.args.getBindAddress(), Cli.args.getPort());
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) {
          ch.pipeline().addLast(new CatcherHandler());
        }
      });
      b.channel(NioServerSocketChannel.class);
      b.localAddress(isa);
      b.group(group);
      ChannelFuture f = b.bind().sync();
      b.option(ChannelOption.TCP_NODELAY, true);
      System.out.println("Catcher listening on: " + Cli.args.getBindAddress() + ":" + Cli.args.getPort());
      f.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully().sync();
    }
  }

  @ChannelHandler.Sharable
  private class CatcherHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      System.out.println("pitcher disconnected :  " + ctx.channel().remoteAddress());
      ctx.fireChannelInactive();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      System.out.println("pitcher connected from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      ByteBuf in = (ByteBuf) msg;
      ChannelFuture cf = ctx.writeAndFlush(in);
      cf.syncUninterruptibly();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      ctx.close();
    }
  }
}

package org.waitlight.codememo.simple.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NettyHttpProxyServer {
    private static final Logger LOGGER = LogManager.getLogger(NettyHttpProxyServer.class);

    private final int port;
    private final String remoteHost;
    private final int remotePort;

    public NettyHttpProxyServer(int port, String remoteHost, int remotePort) {
        this.port = port;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String remoteHost = "localhost";
        int remotePort = 3000;
        new NettyHttpProxyServer(port, remoteHost, remotePort).run();
    }

    public void run() throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(1024 * 1024),
                                    new ChunkedWriteHandler(),
                                    new NettyHttpClientHandler(remoteHost, remotePort)
                            );
                        }
                    })
                    .bind(port).sync()
                    .channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class NettyHttpClientHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LogManager.getLogger(NettyHttpClientHandler.class);

    private final String remoteHost;
    private final int remotePort;

    public NettyHttpClientHandler(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest clientReq) {
        LOGGER.info("Received request: {} {}", clientReq.method(), clientReq.uri());

        final Channel clientChannel = ctx.channel();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientChannel.eventLoop())
                .channel(clientChannel.getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new HttpClientCodec(),
                                new HttpObjectAggregator(1024 * 1024),
                                new ChunkedWriteHandler(),
                                new NettyHttpRemoteHandler(clientChannel)
                        );
                    }
                });

        bootstrap.connect(remoteHost, remotePort).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                future.channel().writeAndFlush(clientReq);
            } else {

                future.channel().close();
            }
        });
    }
}

class NettyHttpRemoteHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private static final Logger LOGGER = LogManager.getLogger(NettyHttpRemoteHandler.class);

    private final Channel clientChannel;

    public NettyHttpRemoteHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
        LOGGER.info("Received response from remote server: {}", response.content());
        clientChannel.writeAndFlush(response);
    }
}
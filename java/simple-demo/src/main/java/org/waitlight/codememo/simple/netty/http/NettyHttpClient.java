package org.waitlight.codememo.simple.netty.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class NettyHttpClient {

    private static final Logger LOGGER = LogManager.getLogger(NettyHttpClient.class);

    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();

        URI uri = new URI("http://localhost:8080");

        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 80;
        String path = uri.getRawPath() != null ? uri.getRawPath() : "/";

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                        pipeline.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
                                LOGGER.info("Response Status: {}", response.status());
                                LOGGER.info("Response Body: {}", response.content().toString(io.netty.util.CharsetUtil.UTF_8));
                                ctx.close();
                            }
                        });
                    }
                });

        for (int i = 0; i < 100; i++) {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            int finalI = i;
            future.addListener((ChannelFutureListener) cf -> {
                if (cf.isSuccess()) {
                    LOGGER.info("Request {} sent successfully.", finalI);
                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
                    request.headers().set(HttpHeaderNames.HOST, host);
                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                    cf.channel().writeAndFlush(request);
                } else {
                    cf.cause().printStackTrace();
                }
            });
        }
    }
}
package com.karlsoft.binaryproxy.proxy;

import com.karlsoft.binaryproxy.handler.FrontendHandler;
import com.karlsoft.binaryproxy.handler.codec.UpstreamBase64Encoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.URI;

public class BinaryProxyInitializer extends ChannelInitializer<SocketChannel> {

    private final String uri;

    public BinaryProxyInitializer(String uri) {
        this.uri = uri;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("trafficLogger", new LoggingHandler(LogLevel.INFO));
        pipeline.addLast("idleStateHandler", new IdleStateHandler(300, 0, 0));
        // inbound
        pipeline.addLast("lengthDecoder", new LengthFieldBasedFrameDecoder(2 * 1024,
                0, 2, 0, 2));
        pipeline.addLast("base64Encoder", new UpstreamBase64Encoder(false));
        // outbound
        pipeline.addLast(new LengthFieldPrepender(2));
        // app
        pipeline.addLast("frontendHandler", new FrontendHandler(URI.create(uri)));
    }

}

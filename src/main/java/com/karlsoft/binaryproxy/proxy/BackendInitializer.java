package com.karlsoft.binaryproxy.proxy;

import com.karlsoft.binaryproxy.handler.BackendHandler;
import com.karlsoft.binaryproxy.handler.codec.DownstreamBase64Decoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 13.05.18.
 */
public class BackendInitializer extends ChannelInitializer<SocketChannel> {

    private final Channel inboundChannel;

    public BackendInitializer(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.INFO),
                new HttpClientCodec(),
                new HttpContentDecompressor(),
                new DownstreamBase64Decoder(),
                new BackendHandler(inboundChannel));
    }

}

package com.karlsoft.binaryproxy.handler;

import com.karlsoft.binaryproxy.proxy.BackendInitializer;
import com.karlsoft.binaryproxy.util.ProxyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class FrontendHandler extends ChannelInboundHandlerAdapter {

    private final Logger LOG = LoggerFactory.getLogger(FrontendHandler.class);

    private final URI uri;

    private Channel outboundChannel;

    public FrontendHandler(URI uri) {
        this.uri = uri;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();
        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new BackendInitializer(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);
        ChannelFuture f = b.connect(uri.getHost(), uri.getPort());
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    LOG.debug("listener. try to read");
                    inboundChannel.read();
                } else {
                    LOG.debug("listener. close");
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        LOG.debug("channelRead. out is active? =" + outboundChannel.isActive());
        if (outboundChannel.isActive()) {
            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, POST, uri.getPath(),
                    (ByteBuf) msg);
            request.headers().add(HttpHeaderNames.HOST, uri.getHost());
            request.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.BASE64);
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            outboundChannel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk
                        System.out.println("channelRead.outboundChannel. read");
                        ctx.channel().read();
                    } else {
                        System.out.println("channelRead. outboundChannel. close");
                        future.channel().close();
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.debug("channelInactive. outboundChannel =" + outboundChannel);
        if (outboundChannel != null) {
            ProxyUtil.closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception -{}", cause);
        cause.printStackTrace();
        ProxyUtil.closeOnFlush(ctx.channel());
    }

}

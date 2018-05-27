package com.karlsoft.binaryproxy.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;

import java.util.List;

/**
 * Encodes a {@link ByteBuf} into a Base64-encoded {@link ByteBuf}.
 * Default Netty Base64Encoder works with downstream.
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 16.05.18.
 */
@Sharable
public class UpstreamBase64Encoder extends MessageToMessageDecoder<ByteBuf> {

    private final boolean breakLines;
    private final Base64Dialect dialect;

    public UpstreamBase64Encoder() {
        this(true);
    }

    public UpstreamBase64Encoder(boolean breakLines) {
        this(breakLines, Base64Dialect.STANDARD);
    }

    public UpstreamBase64Encoder(boolean breakLines, Base64Dialect dialect) {
        if (dialect == null) {
            throw new IllegalArgumentException("dialect");
        }
        this.breakLines = breakLines;
        this.dialect = dialect;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(Base64.encode(in, in.readerIndex(), in.readableBytes(), breakLines, dialect));
    }

}

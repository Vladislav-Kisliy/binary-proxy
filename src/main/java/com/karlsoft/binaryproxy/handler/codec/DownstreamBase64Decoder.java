package com.karlsoft.binaryproxy.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.handler.codec.http.HttpContent;

import java.util.List;

/**
 * Decodes a {@link HttpContent} from Base64-encoded {@link String}.
 * Default Netty Base64Decoder works with upstream.
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 16.05.18.
 */
@Sharable
public class DownstreamBase64Decoder extends MessageToMessageDecoder<HttpContent> {

    private final Base64Dialect dialect;

    public DownstreamBase64Decoder() {
        this(Base64Dialect.STANDARD);
    }

    public DownstreamBase64Decoder(Base64Dialect dialect) {
        if (dialect == null) {
            throw new IllegalArgumentException("dialect");
        }
        this.dialect = dialect;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpContent msg, List<Object> out) throws Exception {
        ByteBuf content = msg.content();
        out.add(Base64.decode(content, content.readerIndex(), content.readableBytes(), dialect));
    }

}

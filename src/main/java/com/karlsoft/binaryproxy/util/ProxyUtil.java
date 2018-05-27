package com.karlsoft.binaryproxy.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.base64.Base64;

import java.nio.charset.StandardCharsets;

/**
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 15.05.18.
 */
public class ProxyUtil {

    /**
     * Closes the specified channel after all queued write requests are flushed.
     *
     * @param ch
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Returns a Base64-encoded string.
     *
     * @param byteBuf
     * @return
     */
    public static String getBase64EncodedString(ByteBuf byteBuf) {
        ByteBuf encodedByteBuf = null;
        try {
            encodedByteBuf = Base64.encode(byteBuf);
            return encodedByteBuf.toString(StandardCharsets.UTF_8);
        } finally {
            // The release is called to suppress the memory leak error messages raised by netty.
            if (byteBuf != null) {
                byteBuf.release();
                if (encodedByteBuf != null) {
                    encodedByteBuf.release();
                }
            }
        }
    }

}

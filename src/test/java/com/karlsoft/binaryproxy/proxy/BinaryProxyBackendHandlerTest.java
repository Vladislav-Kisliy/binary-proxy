package com.karlsoft.binaryproxy.proxy;

import com.karlsoft.binaryproxy.handler.BackendHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 14.04.18.
 */
public class BinaryProxyBackendHandlerTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test1() throws Exception {
        EmbeddedChannel inChannel = new EmbeddedChannel();

        ByteBuf byteBuf = Unpooled.wrappedBuffer("TEST MESSAGE".getBytes(StandardCharsets.UTF_8));

        EmbeddedChannel channel = new EmbeddedChannel(new BackendHandler(inChannel));
        channel.writeInbound(byteBuf);

        assertTrue(inChannel.inboundMessages().isEmpty());
        assertEquals(1, inChannel.outboundMessages().size());
        assertEquals(byteBuf, inChannel.outboundMessages().poll());
    }

}
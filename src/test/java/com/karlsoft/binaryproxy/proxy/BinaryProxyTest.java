package com.karlsoft.binaryproxy.proxy;

import com.github.dreamhead.moco.HttpServer;
import com.github.dreamhead.moco.Runner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dreamhead.moco.HttpProtocolVersion.VERSION_1_1;
import static com.github.dreamhead.moco.Moco.and;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.header;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.text;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Moco.version;
import static com.github.dreamhead.moco.Moco.with;
import static com.github.dreamhead.moco.Runner.runner;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 22.04.18.
 */
public class BinaryProxyTest {

    private static final String LISTEN_ADDRESS = "127.0.0.1";
    private static final int LISTEN_PORT = 19100;
    private static final URI TARGET_LINK = URI.create("http://127.0.0.1:12306/target");

    private static final String DEFAULT_REPLY = "Hello";
    private static final String DEFAULT_RESPONSE = "SGVsbG8="; // Hello
    private static final String TARGET_REPLY = "Waiting orders";
    private static final String TARGET_RESPONSE_BASE64 = "V2FpdGluZyBvcmRlcnM="; // Waiting orders
    private static final String BINARY_RESPONSE_BASE64 = "SBUcTA8g4P8iHAAADC7//w=="; // BINARY_MESSAGE without length

    // Message: length message - 0x0C(12) + HELLO WORLD.
    private static final byte[] TEST_MESSAGE = new byte[]{0x00, 0x0C,
            0x48, 0x45, 0x4C, 0x4C, 0x4F, 0x20, 0x57, 0x4F,
            0x52, 0x4C, 0x44, 0x2E};
    // Message: length message - 0x0f(16) + binary content
    private static final byte[] BINARY_MESSAGE = new byte[]{0x00, 0x10,
            0x48, 0x15, 0x1C, 0x4C, 0x0F, 0x20, (byte) 0xE0, (byte) 0xFF,
            0x22, 0x1c, 0x00, 0x00, 0x0C, 0x2E, (byte) 0xFF, (byte) 0xFF,
            0x00, 0x00};

    private Runner runner;

    @Before
    public void setUp() throws Exception {
        HttpServer server = httpServer(TARGET_LINK.getPort());
        server.request(and(by(uri("/target")), by(version(VERSION_1_1))))
                .response(with(text(TARGET_RESPONSE_BASE64)), header("Content-Type", "base64"));
        server.response(DEFAULT_RESPONSE);
        server.request(and(by(uri("/binary")), by(version(VERSION_1_1))))
                .response(with(text(BINARY_RESPONSE_BASE64)), header("Content-Type", "base64"));
        runner = runner(server);
        runner.start();
    }

    @After
    public void tearDown() {
        runner.stop();
    }

    @Test
    public void testTarget() throws Exception {
        BinaryProxy proxy = startProxy(TARGET_LINK.toASCIIString());

        byte[] response = getServerResponse();

        proxy.stop();

        assertArrayEquals(wrapLine(TARGET_REPLY), response);
    }

    @Test
    public void testDefault() throws Exception {
        BinaryProxy proxy = startProxy("http://127.0.0.1:12306/");

        byte[] response = getServerResponse();

        proxy.stop();

        assertArrayEquals(wrapLine(DEFAULT_REPLY), response);
    }


    @Test
    public void testBinary() throws Exception {
        BinaryProxy proxy = startProxy("http://127.0.0.1:12306/binary");

        byte[] bytes = getServerResponse();
        proxy.stop();
        assertArrayEquals(BINARY_MESSAGE, bytes);
    }

    @Test
    public void test50Clients() throws Exception {
        // Run server
        BinaryProxy proxy = startProxy(TARGET_LINK.toASCIIString());

        ExecutorService executor = Executors.newWorkStealingPool();

        int threads = 50;
        List<Callable<byte[]>> callables = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            callables.add(() -> getServerResponse());
        }

        final AtomicInteger counter = new AtomicInteger(0);
        executor.invokeAll(callables)
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .forEach((byte[] response) -> {
                    assertArrayEquals(wrapLine(TARGET_REPLY), response);
                    counter.incrementAndGet();
                });

        assertEquals(threads, counter.get());

        proxy.stop();

        try {
            System.out.println("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("tasks interrupted");
        } finally {
            if (!executor.isTerminated()) {
                System.err.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
        }
    }

    private String checkServer() throws IOException {
        Socket server = new Socket(LISTEN_ADDRESS, LISTEN_PORT);
        OutputStream out = server.getOutputStream();
        out.write(TEST_MESSAGE);
        out.flush();
        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(server.getInputStream()));

        return in.readLine();
    }

    private BinaryProxy startProxy(String link) throws InterruptedException {
        final BinaryProxy binaryProxy = new BinaryProxy(LISTEN_PORT, link);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    binaryProxy.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        TimeUnit.MILLISECONDS.sleep(400);

        return binaryProxy;
    }

    private byte[] getServerResponse() throws IOException {
        Socket server = new Socket(LISTEN_ADDRESS, LISTEN_PORT);
        OutputStream out = server.getOutputStream();
        out.write(TEST_MESSAGE);
        out.flush();

        InputStream is = server.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[512];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private byte[] wrapLine(String line) {
        if (line.length() > 65000) {
            throw new IllegalArgumentException("Too big line");
        }
        byte[] lineBytes = line.getBytes();
        byte[] result = new byte[lineBytes.length + 4];
        byte[] lengthInBytes = ByteBuffer.allocate(2).putShort((short) lineBytes.length).array();
        result[0] = lengthInBytes[0];
        result[1] = lengthInBytes[1];
        System.arraycopy(lineBytes, 0, result, 2, lineBytes.length);
        return result;
    }

}
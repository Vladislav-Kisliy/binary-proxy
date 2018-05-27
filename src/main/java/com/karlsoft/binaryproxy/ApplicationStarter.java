package com.karlsoft.binaryproxy;

import com.karlsoft.binaryproxy.proxy.BinaryProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Vladislav Kisliy<vladislav.kisliy@gmail.com> on 15.02.18.
 */
public class ApplicationStarter {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationStarter.class);

    private static final String DEFAULT_PORT = "19100";
    private static final String DEFAULT_URL = "http://127.0.0.1:12306/target";

    public static void main(String[] args) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream("application.properties")) {
            properties.load(resourceStream);
        } catch (IOException ex) {
            LOG.error("Can't read property file. Ex " + ex.getMessage());
        }

        BinaryProxy application = new BinaryProxy(
                Integer.parseInt(properties.getProperty("local.port", DEFAULT_PORT)),
                properties.getProperty("remote.uri", DEFAULT_URL));

        LOG.debug("ApplicationStarter start running");
        application.start();
    }

}

package com.bpcreates.ioclient;

import com.bpcreates.remoteclient.EchoServer;
import org.junit.*;

import java.io.IOException;
import java.net.InetAddress;

import static com.bpcreates.remoteclient.Util.now;
import static java.lang.String.format;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/22/12
 * Time: 5:25 PM
 */
public class IOClientIntegrationTest {
    private static final String TEST_HOST = "127.0.0.1";
    private static final Integer TEST_PORT = 10005;

    private static Thread mockServerThread;
    private static EchoServer mockServer;

    private IOClient testClient;
    private IOClientCallback callback;

    @Before
    public void startServer() throws IOException {
        log("startServer() called.");
        InetAddress address = InetAddress.getLocalHost();
        mockServer = new EchoServer(address, TEST_PORT);
        mockServerThread = new Thread(mockServer);
        mockServerThread.start();
        log("mock server thread started.");
    }

    @Before
    public void initTestClient() throws IOException {
        log("initTestClient() called.");
        this.callback = initCallback();
        this.testClient = IOClientFactory.i(TEST_HOST, TEST_PORT, callback);
    }

    // @Test
    public void pingServer() throws IOException, InterruptedException {
        log("pingServer() called.");
        Thread testClientThread = new Thread(testClient);
        testClientThread.start();
        testClient.sendMessage("test-message");
        Thread.sleep(60000);
    }

    @After
    public void shutdownTestClient() throws IOException {
        log("shutdownTestClient() called.");
        if(null != this.testClient) {
            testClient.shutdown();
        }
    }

    @AfterClass
    public static void stopServer() throws IOException {
        log("stopServer() called.");
        if(null != mockServer && mockServer.isStarted()) {
            mockServer.stopServer();
        }
    }

    private IOClientCallback initCallback() {
        IOClientCallback callback = new IOClientCallback() {
            @Override
            public void onClientError(Throwable throwable) {
                log(format("ERROR: %s", throwable.getMessage()));
                throwable.printStackTrace(System.out);
            }

            @Override
            public void onClientConnect() {
                log("onClientConnect() called.");
            }

            @Override
            public void onClientDisconnect() {
                log("onClientDisconnect() called.");
            }

            @Override
            public void onClientDataReceived(String dataReceived) {
                log(format("onClientDataReceived('%s')", dataReceived));
            }

            @Override
            public void onClientShutdown() {
                log("onClientShutdown() called.");
            }

            @Override
            public void onClientDataDelivered() {
                log("onClientDataDelivered() called.");
            }
        };
        return callback;
    }

    private static void log(String message) {
        System.out.println(format("[T] [%s] [IOClientIntegrationTest] %s", now(), message));
    }
}
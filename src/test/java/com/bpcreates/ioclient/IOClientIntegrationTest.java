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

    private static EchoServer mockServer;

    private IOClient testClient;
    private IOClientCallback callback;

    @BeforeClass
    public static void startServer() throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        mockServer = new EchoServer(address, TEST_PORT);
        Thread mockServerThread = new Thread(mockServer);
        mockServerThread.start();
    }

    @Before
    public void initTestClient() throws IOException {
        this.callback = initCallback();
        this.testClient = IOClientFactory.i(TEST_HOST, TEST_PORT, callback);
    }

    @Test
    public void pingServer() throws IOException, InterruptedException {
        Thread testClientThread = new Thread(testClient);
        testClientThread.start();
        this.callback.sendData();
        Thread.sleep(90000);
    }

    @After
    public void shutdownTestClient() throws IOException {
        if(null != this.testClient) {
            testClient.shutdown();
        }
    }

    @AfterClass
    public static void stopServer() throws IOException {
        if(null != mockServer && mockServer.isStarted()) {
            mockServer.stopServer();
        }
    }

    private IOClientCallback initCallback() {
        IOClientCallback callback = new IOClientCallback() {

            private String message = "test-message";

            @Override
            public void onError(Throwable throwable) {
                log(format("ERROR: %s", throwable.getMessage()));
                throwable.printStackTrace(System.out);
            }

            @Override
            public void onConnect() {
                log("onConnect() called.");
            }

            @Override
            public void onDisconnect() {
                log("onDisconnect() called.");
            }

            @Override
            public void onDataReceived(String dataReceived) {
                log(format("onDataReceived('%s')", dataReceived));
            }

            @Override
            public void onShutdown() {
                log("onShutdown() called.");
            }

            @Override
            public String sendData() {
                log("sendData() called.");
                return message;
            }

            @Override
            public void onDataDelivered() {
                log("onDataDelivered() called.");
                this.message = null;
            }
        };
        return callback;
    }

    private static void log(String message) {
        System.out.println(format("[T] [%s] [IOClientIntegrationTest] %s", now(), message));
    }
}
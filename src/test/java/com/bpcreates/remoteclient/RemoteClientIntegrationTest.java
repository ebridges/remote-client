package com.bpcreates.remoteclient;

import org.junit.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static junit.framework.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/18/12
 * Time: 11:37 AM
 */
public class RemoteClientIntegrationTest {
    private static final String TEST_HOST = "localhost";
    private static final Integer TEST_PORT = 10005;

    private static EchoServer mockServer;

    private RemoteClient testClientA;

    @BeforeClass
    public static void beforeClass() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress testHost = InetAddress.getByName(TEST_HOST);
                    mockServer = new EchoServer(testHost, TEST_PORT);
                    mockServer.startServer();
                } catch (UnknownHostException e) {
                    throw new ExceptionInInitializerError(e);
                } catch (IOException e) {
                    throw new ExceptionInInitializerError(e);
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    @Before
    public void initTestClient() throws IOException {
        testClientA = RemoteClientFactory.i("channel", TEST_HOST, TEST_PORT);
        testClientA.open();
    }

    @Test
    public void testPing() throws IOException {
        String expectedMessage = "echo:hello";
        Request request = new Request(expectedMessage);
        Response response = testClientA.submitRequest(request);
        String actualMessage = response.getPayload();
        assertEquals(expectedMessage+"\0", actualMessage);
    }

    @After
    public void shutdownTestClient() throws IOException {
        if(null != testClientA) {
            testClientA.close();
        }
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if(null != mockServer) {
            mockServer.stopServer();
        }
    }
}

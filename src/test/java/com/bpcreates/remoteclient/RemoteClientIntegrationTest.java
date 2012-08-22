package com.bpcreates.remoteclient;

import org.junit.*;

import java.io.*;
import java.nio.Buffer;

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

    private static Process mockServer;

    private RemoteClient testClientA;
    private RemoteClient testClientB;

 //   @BeforeClass
    /*
    public static void startServer() throws IOException {
        File wd = new File("src/main/pl");
        ProcessBuilder processBuilder = new ProcessBuilder("perl", "socket_test.cgi");
        processBuilder.directory(wd.getAbsoluteFile());
        mockServer = processBuilder.start();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String buffer = null;
                BufferedReader r = new BufferedReader(new InputStreamReader(mockServer.getInputStream()));
                try {
                    while( (buffer = r.readLine()) != null) {
                        System.out.println(buffer);
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        new Thread(r).start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    */

    @Before
    public void initTestClients() throws IOException {
        testClientA = RemoteClientFactory.i("channel", TEST_HOST, TEST_PORT);
        testClientA.open();

 //       testClientB = RemoteClientFactory.i("channel", TEST_HOST, TEST_PORT);
 //       testClientB.open();
    }

    @Test
    public void testPing() throws IOException {
        String expectedMessage = "abc\ndef\nghi";
        Request request = new Request(expectedMessage);
        Response response = testClientA.submitRequest(request);
        String actualMessage = response.getPayload();
        assertEquals(expectedMessage, actualMessage.trim());
    }

    @Test
    public void testPingXmit() throws IOException {
        String testClientAMessage = "testClientAMessage";
        Request requestA = new Request(testClientAMessage);
        Response responseA = testClientA.submitRequest(requestA);

        String testClientBMessage = "testClientBMessage";
        Request requestB = new Request(testClientBMessage);
        Response responseB = testClientB.submitRequest(requestB);

        String actualMessage = responseB.getPayload();

        String expectedMessage = testClientAMessage + testClientBMessage;
        assertEquals(expectedMessage.trim(), actualMessage.trim());
    }

    @After
    public void shutdownTestClient() throws IOException {
        if(null != testClientA) {
            testClientA.close();
        }

        if(null != testClientB) {
            testClientB.close();
        }
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if(null != mockServer) {
            log("shutting down server process.");
            mockServer.destroy();
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
}

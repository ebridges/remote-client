package com.bpcreates.ioclient;

import static com.bpcreates.common.Util.Loge;
import static com.bpcreates.common.Util.Logi;
import static com.bpcreates.common.Util.notEmpty;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import com.bpcreates.common.EchoServer;

public class IOClientTest {

  private static final Map<String, String> TEST_MESSAGES = new HashMap<String, String>();

  private static Thread clientThread;

  static {
 //   TEST_MESSAGES.put(null, "");
    TEST_MESSAGES.put("READY", "READY");
    TEST_MESSAGES.put("echo:hello", "echo:hello");
    TEST_MESSAGES.put("<policy-file-request/>", "<policy-file-request/>");
//    TEST_MESSAGES.put("<policy-file-request/>",
//            "<?xml version=\"1.0\"?>\n" + "\t\t\t\t\t\t\t\t\t<cross-domain-policy>\n"
//            + "\t\t\t\t\t\t\t\t\t  <allow-access-from domain=\"*\" to-ports=\"*\"/>\n"
//            + "\t\t\t\t\t\t\t\t\t</cross-domain-policy>");
  }

  public static void main(String[] args) throws Exception {
    String host = args.length > 0 && notEmpty(args[0]) ? args[0] : "127.0.0.1";
    Integer port = args.length > 0 && notEmpty(args[1]) ? Integer.parseInt(args[1]) : 10005;

    EchoServer server = null;
    try {
      server = new EchoServer(host, port);
      Thread serverThread = new Thread(server);
      serverThread.start();
      Thread.sleep(1000);


      for (final Map.Entry<String, String> messages : TEST_MESSAGES.entrySet()) {
        TestIOClientCallback callback = new TestIOClientCallback(messages.getValue());
        final IOClient client = IOClientFactory.i(host, port, callback);
        try {
          clientThread = new Thread(client);
          clientThread.start();

          Thread messenger = new Thread(new Runnable() {
            @Override
            public void run() {
              client.sendMessage(messages.getKey());
            }
          });
          messenger.start();
          /*
          Thread.sleep(1000);
          messenger.join();
          */
          synchronized (clientThread) {
            clientThread.wait();
          }
        } finally {
          if (null != client) {
            client.shutdown();
          }
        }
      }

//      serverThread.join();
    } finally {
      if(null != server) {
        server.stopServer();
      }
    }
  }

  private static class TestIOClientCallback implements IOClientCallback {

    private static final String TAG = TestIOClientCallback.class.getSimpleName();

    private String expected;

    private TestIOClientCallback(String expectedData) {
      this.expected = expectedData;
    }

    @Override
    public void onClientError(Throwable throwable) {
      Loge(TAG, "ClientError", throwable);
    }

    @Override
    public void onClientConnect() {
      Logi(TAG, "ClientConnect");
    }

    @Override
    public void onClientDisconnect() {
      Logi(TAG, "ClientDisconnect");
    }

    @Override
    public void onClientDataReceived(String dataReceived) {
      Logi(TAG, format("ClientDataReceived [%s]", dataReceived));
      if (!areEqual(expected, dataReceived)) {
        throw new AssertionError(format("expected [%s] actual [%s]", expected, dataReceived));
      }
      synchronized (clientThread) {
        clientThread.notify();
      }
    }

    @Override
    public void onClientShutdown() {
      Logi(TAG, "ClientShutdown");
    }

    @Override
    public void onClientDataDelivered() {
      Logi(TAG, "ClientDataDelivered");
    }
  }

  private static boolean areEqual(String L, String R) {
    char[] left = L.toCharArray();
    char[] right = R.toCharArray();
    if (left.length != right.length) {
      int idx = Math.max(left.length, right.length);
      System.out.print("[L]: ");
      for (int i = 0; i < idx; i++) {
        if (idx < left.length) {
          System.out.print(String.format("[%c]", left[i]));
        } else {
          System.out.print("[ ]");
        }
      }
      System.out.print("\n[R]: ");
      for (int i = 0; i < idx; i++) {
        if (idx < right.length) {
          System.out.print(String.format("[%c]", right[i]));
        } else {
          System.out.print("[ ]");
        }
      }
      System.out.println();
      return false;
    }

    int idx = left.length;
    System.out.print("[L]: ");
    //noinspection ForLoopReplaceableByForEach
    for (int ii = 0; ii < idx; ii++) {
      char c = left[ii];
      if (Character.isWhitespace(c)) {
        c = ' ';
      }
      System.out.print(String.format("[%c]", c));
    }
    System.out.print("\n[R]: ");
    for (int ii = 0; ii < idx; ii++) {
      char c = right[ii];
      if (Character.isWhitespace(c)) {
        c = ' ';
      }
      System.out.print(String.format("[%c]", c));
    }
    System.out.println();

    boolean matched = true;
    System.out.print("     ");
    for (int i = 0; i < idx; i++) {
      if (left[i] != right[i]) {
        System.out.print("[x]");
        matched = false;
      } else {
        System.out.print("[.]");
      }
    }
    System.out.println();
    return matched;
  }
}


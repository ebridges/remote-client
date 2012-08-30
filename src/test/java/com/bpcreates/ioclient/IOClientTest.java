package com.bpcreates.ioclient;

import java.io.IOException;

import com.bpcreates.common.Util;

public class IOClientTest {
  private final String host;
  private final Integer port;

  private IOClient ioClient;

  public IOClientTest(String host, Integer port) {
    this.host = host;
    this.port = port;
  }

  public void setup() throws IOException {
    IOClientCallback callback = new MockIOClientCallback();
    this.ioClient = IOClientFactory.i(host, port, callback);
  }

  public void teardown() throws IOException {
    if(null != this.ioClient) {
      this.ioClient.shutdown();
    }
  }
  public void testMessage(String message, String expected) throws IOException {
    this.ioClient.sendMessage(message);
/////// todo how to get response???

    String response="";

    String expectedResponse = Util.clean(expected);
    String actualResponse = Util.clean(response);

    String mesg = String.format("t:[%s] e:[%s] a:[%s]", message, expectedResponse, actualResponse);
    if(areEqual(expectedResponse, actualResponse)) {
      System.out.println("OK : " + mesg);
    } else {
      System.out.println("ERR: "+mesg);
    }
  }

  public static void main(String[] args) throws Exception {
    String host = args[0];
    Integer port = Integer.parseInt(args[1]);

    IOClientTest integrationTest = new IOClientTest(host, port);
    try {
      integrationTest.setup();

      // todo
      // this should loop over a set of individual messages, creating a separate new client for each to verify
      // the response in a special callback.


      integrationTest.testMessage("READY", "READY");
      integrationTest.testMessage("ping", "PONG");
      integrationTest.testMessage("PING", "PONG");
      integrationTest.testMessage("echo:harry", "echo:harry");
      integrationTest.testMessage("<policy-file-request/>", "<?xml version=\"1.0\"?>\n"
          + "\t\t\t\t\t\t\t\t\t<cross-domain-policy>\n"
          + "\t\t\t\t\t\t\t\t\t  <allow-access-from domain=\"*\" to-ports=\"*\"/>\n"
          + "\t\t\t\t\t\t\t\t\t</cross-domain-policy>");
      integrationTest.testMessage("asdfasdf", "asdfasdf");

    } finally {
      integrationTest.teardown();
    }
  }

  private static boolean areEqual(String L, String R){
    char[] left = L.toCharArray();
    char[] right = R.toCharArray();
    if(left.length != right.length){
      for(int i=0; i<left.length; i++) {
        System.out.println(String.format("L:[%d]:[%c]", i, left[i]));
      }
      for(int i=0; i<right.length; i++) {
        System.out.println(String.format("R:[%d]:[%c]", i, right[i]));
      }
      return false;
    }

    for(int i=0; i<left.length; i++) {
      String mesg = String.format("L[%c] R[%c]", left[i], right[i]);
      if(left[i] != right[i]) {
        System.out.println("ERR: "+mesg);
        return false;
      }else {
        System.out.println("OK : "+mesg);
      }
    }
    return true;
  }

  private class MockIOClientCallback implements IOClientCallback {

    @Override
    public void onClientError(Throwable throwable) {
    }

    @Override
    public void onClientConnect() {
    }

    @Override
    public void onClientDisconnect() {
    }

    @Override
    public void onClientDataReceived(String dataReceived) {
    }

    @Override
    public void onClientShutdown() {
    }

    @Override
    public void onClientDataDelivered() {
    }
  }
}


package com.bpcreates.remoteclient;

import static com.bpcreates.remoteclient.Util.notEmpty;

import java.io.IOException;

public abstract class AbstractRemoteClientTest {

  protected String host;

  protected Integer port;

  private RemoteClient remoteClient;

  public AbstractRemoteClientTest(String host, Integer port) {
    this.host = host;
    this.port = port;
  }

  abstract protected String getConnectionType();

  public void testMessage(String message, String expected) throws IOException {
    Request request = new Request(message);
    Response response = this.remoteClient.submitRequest(request);

    String expectedResponse = clean(expected);
    String actualResponse = clean(response.getPayload());

    String mesg = String.format("t:[%s] e:[%s] a:[%s]", message, expectedResponse, actualResponse);
    if(areEqual(expectedResponse, actualResponse)) {
      System.out.println("OK : " + mesg);
    } else {
      System.out.println("ERR: "+mesg);
    }
  }

  private String clean(String s) {
    if(notEmpty(s)) {
      String ss = s.trim();
      ss = ss.replaceAll("\n", "");
      ss = ss.replaceAll("\r", "");
      ss = ss.replace("\t+", " ");
      ss = ss.replaceAll("\\s+", " ");
      return ss;
    }
    return "";
  }

  private boolean areEqual(String L, String R){
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

  public void setup() throws IOException {
    this.remoteClient = RemoteClientFactory.i(this.getConnectionType(), host, port);
    this.remoteClient.open();
  }

  public void teardown() throws IOException {
    if(null != this.remoteClient) {
      this.remoteClient.close();
    }
  }
}

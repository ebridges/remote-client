package com.bpcreates.remoteclient;

public class RemoteClientChannelTest extends AbstractRemoteClientTest {

  public RemoteClientChannelTest(String host, Integer port) {
    super(host, port);
  }

  @Override
  protected String getConnectionType() {
    return "channel";
  }

  public static void main(String[] args) throws Exception {
    String host;
    Integer port;
    if(args.length == 2) {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } else {
      host = "localhost";
      port = 10005;
    }

    RemoteClientChannelTest integrationTest = new RemoteClientChannelTest(host, port);
    try {
      integrationTest.setup();

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
}

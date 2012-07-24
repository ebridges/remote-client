package com.bpcreates.remoteclient;

import static com.bpcreates.remoteclient.Util.DEFAULT_CHARSET;
import java.nio.charset.Charset;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 9:41 PM
 */
public class RemoteClientFactory {
  public static final String DEFAULT_CONNECTION_TYPE = "channel";

  public static RemoteClient i(String host, int port) {
    return i(DEFAULT_CONNECTION_TYPE, host, port);
  }

    public static RemoteClient i(String type, Charset charset, String host, int port) {
        return init(type, charset, host, port);
    }

    public static RemoteClient i(String type, String host, int port) {
        return init(type, Charset.forName(DEFAULT_CHARSET), host, port);
    }

    private static RemoteClient init(String type, Charset charset, String host, int port) {
        if("channel".equals(type)) {
            return new RemoteChannelClient(charset, host, port);
        }
        if("socket".equals(type)) {
            return new RemoteSocketClient(charset, host, port);
        }
        throw new IllegalArgumentException("invalid type: "+type);
    }

    private RemoteClientFactory() {
    }
}

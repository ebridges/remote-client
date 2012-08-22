package com.bpcreates.ioclient;

import java.net.UnknownHostException;
import java.nio.charset.Charset;

import static com.bpcreates.remoteclient.Util.isEmpty;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/22/12
 * Time: 5:10 PM
 */
public class IOClientFactory {
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    public static final Integer DEFAULT_READBUFFER_SIZE = 8192;

    public static IOClient i(String host, int port, IOClientCallback callback) throws UnknownHostException {
        if(null == callback) {
            throw new IllegalArgumentException("null callback");
        }
        if(isEmpty(host)) {
            throw new IllegalArgumentException("empty hostname");
        }
        if(port < 0) {
            throw new IllegalArgumentException("bad port num");
        }
        return new IOClientImpl(DEFAULT_CHARSET, host, port, callback, DEFAULT_READBUFFER_SIZE);
    }

    private IOClientFactory() {
        // factory class
    }
}

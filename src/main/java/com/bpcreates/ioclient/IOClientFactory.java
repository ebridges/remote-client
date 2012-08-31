package com.bpcreates.ioclient;

import static com.bpcreates.common.Util.isEmpty;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/22/12
 * Time: 5:10 PM
 */
public class IOClientFactory {
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    public static final Integer DEFAULT_READBUFFER_SIZE = 8192;

    public static IOClient i(String host, int port, IOClientCallback callback) throws IOException {
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

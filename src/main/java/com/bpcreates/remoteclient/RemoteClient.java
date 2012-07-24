package com.bpcreates.remoteclient;

import java.io.IOException;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 5:02 PM
 */
public interface RemoteClient {
    void open() throws IOException;

    Response submitRequest(Request request) throws IOException;

    void close() throws IOException;
}

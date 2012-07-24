package com.bpcreates.remoteclient;

import static com.bpcreates.remoteclient.Util.*;

import java.io.IOException;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 9:33 PM
 */
public class RemoteClientTest {
    private static final String TAG = "RemoteClientTest";

    public static void main(String[] args) throws IOException {
        String type;
        String command;
        String hostname;
        Integer port;
        if(args.length != 4) {
            System.err.println("Usage: RemoteClientTest <type> <command> <host> <port>");
        }

        if(notEmpty(args[0])) {
            type = args[0].trim();
        } else {
            type = "socket";
        }

        if(notEmpty(args[1])) {
            command = args[1].trim();
        } else {
            command = "start";
        }

        if(notEmpty(args[2])) {
            hostname = args[2].trim();
        } else {
            hostname = "localhost";
        }

        if(notEmpty(args[3])) {
            port = Integer.valueOf(args[3].trim());
        } else {
            port = 2007;
        }

        RemoteClient client = RemoteClientFactory.i(type, hostname, port);
        try {
            client.open();
            Request request = new Request(command);
            Response response = client.submitRequest(request);
            Logi(TAG, "got response: "+ response.getPayload());
        } catch (Throwable t) {
            Loge(TAG, "caught exception in main", t);
        } finally {
            client.close();
        }
    }
}

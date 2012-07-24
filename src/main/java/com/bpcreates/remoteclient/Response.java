package com.bpcreates.remoteclient;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 2:34 PM
 */
public class Response {
    private String payload;

    public Response(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}

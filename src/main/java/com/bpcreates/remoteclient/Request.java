package com.bpcreates.remoteclient;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 2:33 PM
 */
public class Request {
    private String payload;

    public Request(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}

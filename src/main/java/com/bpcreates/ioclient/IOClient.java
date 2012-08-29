package com.bpcreates.ioclient;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/22/12
 * Time: 5:19 PM
 */
public interface IOClient extends Runnable {
	void sendMessage(String message);
    void shutdown() throws IOException;
}

package com.bpcreates.ioclient;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/22/12
 * Time: 2:09 PM
 */
public interface IOClientCallback {
    /**
     * Called when exceptions are caught.
     * @param throwable
     */
    void onClientError(Throwable throwable);

    /**
     * Called after the connection to the remote server has been established.
     */
    void onClientConnect();

    /**
     * Called when the remote server has closed the connection, and a read reaches EOF.
     */
    void onClientDisconnect();

    /**
     * Called when remote server has sent data.  Data is encoded according to
     * charset configured for the related {@link IOClientImpl}.
     *
     * @param dataReceived
     */
    void onClientDataReceived(String dataReceived);

    /**
     * Called when shutdown of {@link IOClientImpl} has been requested, and after the event loop has
     * completed any pending I/O.
     */
    void onClientShutdown();

    /**
     * Called when data has been successfully delivered to remote server.
     */
    void onClientDataDelivered();
}

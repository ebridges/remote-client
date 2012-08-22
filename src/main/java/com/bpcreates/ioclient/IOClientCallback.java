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
    void onError(Throwable throwable);

    /**
     * Called after the connection to the remote server has been established.
     */
    void onConnect();

    /**
     * Called when the remote server has closed the connection, and a read reaches EOF.
     */
    void onDisconnect();

    /**
     * Called when remote server has sent data.  Data is encoded according to
     * charset configured for the related {@link IOClientImpl}.
     *
     * @param dataReceived
     */
    void onDataReceived(String dataReceived);

    /**
     * Called when shutdown of {@link IOClientImpl} has been requested, and after the event loop has
     * completed any pending I/O.
     */
    void onShutdown();

    /**
     * Use this method to inject data to be written.  This method should return null if there
     * is no data to be delevered.
     *
     * @return null if no data written, or the data to be written.
     */
    String sendData();

    /**
     * Called when data has been successfully delivered to remote server.
     */
    void onDataDelivered();
}

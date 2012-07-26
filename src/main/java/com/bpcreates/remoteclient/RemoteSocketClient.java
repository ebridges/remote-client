package com.bpcreates.remoteclient;

import static com.bpcreates.remoteclient.Util.*;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 5:03 PM
 */
public class RemoteSocketClient implements RemoteClient {
    private static final String TAG = RemoteSocketClient.class.getName();

    private int ioBufferSize;
    private CharsetDecoder decoder;
    private InetSocketAddress socketAddress;
    private Socket socket;

    public RemoteSocketClient(Charset charset, String serverHost, int serverPort) {
        this.ioBufferSize = 1024;
        this.decoder = charset.newDecoder();
        this.socketAddress = new InetSocketAddress(serverHost, serverPort);
        Logi(TAG, format("client initialized for %s:%d [%s].", serverHost, serverPort, charset.displayName()));
    }

    public void open() throws IOException {
        socket = new Socket();
        socket.setReceiveBufferSize(32);
        socket.setSendBufferSize(32);
        socket.connect(this.socketAddress);
        Logd(TAG, format("client connected to %s.", this.socketAddress.toString()));
    }

    public Response submitRequest(Request request) throws IOException {
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

        String query = request.getPayload();
        byte[] bytes = query.getBytes(DEFAULT_CHARSET);
        os.write(bytes);
        os.write(TERMINATOR);
        Logi(TAG, format("wrote %d bytes", bytes.length));

        int i=0,c;
        ByteBuffer buf = ByteBuffer.allocate(ioBufferSize);
        Logi(TAG, "reading from client...");
        while ((c = is.read()) != TERMINATOR) {
            i++;
            buf.put(intToByteArray(c));
        }
        Logi(TAG, format("read %d bytes", i));

        buf.flip();

        CharBuffer charBuffer = decoder.decode(buf);
        String response = new String(charBuffer.array());
        return new Response(response);
    }

    public void close() throws IOException {
        if(this.socket != null && !this.socket.isClosed()) {
            this.socket.close();
        }
    }
}

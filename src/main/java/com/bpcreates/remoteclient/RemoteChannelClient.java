package com.bpcreates.remoteclient;

import static com.bpcreates.remoteclient.Util.*;

import static java.lang.String.format;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class RemoteChannelClient implements RemoteClient {
    private static final String TAG = "RemoteChannelClient";

    private int ioBufferSize;
    private CharsetDecoder decoder;
    private SocketChannel socketChannel;
    private InetSocketAddress socketAddress;

    public RemoteChannelClient(Charset charset, String serverHost, int serverPort) {
        this.ioBufferSize = 1024;
        socketAddress = new InetSocketAddress(serverHost, serverPort);
        this.decoder = charset.newDecoder();
        Logi(TAG, format("client initialized for %s:%d [%s].", serverHost, serverPort, charset.displayName()));
    }

    public void open() throws IOException {
        this.socketChannel = SocketChannel.open();
        this.socketChannel.configureBlocking(false);
        if(this.socketChannel == null) {
            throw new IllegalStateException("unable to open socket channel.");
        }
        boolean result = this.socketChannel.connect(socketAddress);
        Logd(TAG, "connected socket channel, result: " + result);
    }

    public Response submitRequest(Request request) throws IOException {
        if(null == this.socketChannel) {
            throw new IllegalStateException("socket channel not opened.");
        }
        while (this.socketChannel.isConnectionPending()) {
            Logi(TAG, "socket channel is still pending connection, going to finish.");
            this.socketChannel.finishConnect();
            Logd(TAG, "socket channel finished connecting.");
        }

        ByteBuffer buf = ByteBuffer.allocate(ioBufferSize);
        buf.clear();
        buf.put(request.getPayload().getBytes());
        buf.put(new byte[]{TERMINATOR});

        buf.flip();

        while(buf.hasRemaining()) {
            socketChannel.write(buf);
        }

        int payloadSize = request.getPayload().length() + String.valueOf(TERMINATOR).length();
        Logd(TAG, format("wrote %d bytes to server.", payloadSize));

        buf.clear();

        int bytesRead;
        do {
            bytesRead = socketChannel.read(buf);
        } while(bytesRead == 0);

        Logi(TAG, format("read %d bytes from server.", bytesRead));
        buf.flip();

        CharBuffer charBuffer = decoder.decode(buf);
        String response = new String(charBuffer.array());
        return new Response(response);
    }

    public void close() throws IOException {
        if (this.socketChannel.isOpen()) {
            this.socketChannel.close();
        }
    }
}

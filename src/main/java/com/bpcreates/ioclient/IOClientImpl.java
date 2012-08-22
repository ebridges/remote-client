package com.bpcreates.ioclient;

import com.bpcreates.remoteclient.Util;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

import static com.bpcreates.remoteclient.Util.*;
import static java.lang.String.format;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/19/12
 * Time: 4:17 PM
 */

class IOClientImpl implements IOClient {
    private static final String TAG = IOClientImpl.class.getSimpleName();

    private Selector selector;
    private InetAddress address;
    private int port;
    private IOClientCallback callback;
    private Charset charset;
    private int readBufferSize;

    public IOClientImpl(Charset charset, String host, Integer port, IOClientCallback callback, int readBufferSize) throws UnknownHostException {
        this.charset = charset;
        this.address = InetAddress.getByName(host);
        this.port = port;
        this.callback = callback;
        this.readBufferSize = readBufferSize;
    }

    @Override
    public void run() {
        try {
            this.startClient();
        } catch (IOException e) {
            handleError(e);
        }
    }

    @Override
    @SuppressWarnings("unused")
    public void shutdown() throws IOException {
        Logi(TAG, "shutdown requested.");
        if(null != this.selector && this.selector.isOpen()) {
            this.selector.close();
            this.callback.onShutdown();
        }
    }

    private void startClient() throws IOException {
        Logd(TAG, "startClient() called.");

        this.selector = Selector.open();

        this.initalizeChannel();

        while (selector.isOpen()) {
            int count = selector.select();

            if (count == 0) {
                continue;
            }

            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isConnectable()) {
                    this.handleConnect(key);
                }

                if (key.isWritable()) {
                    this.handleWrite(key);
                }

                if (key.isReadable()) {
                    this.handleRead(key);
                }
            }
        }
    }

    private void initalizeChannel() throws IOException {
        Logd(TAG, "initializeChannel() called.");

        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        InetSocketAddress remoteServerAddress = new InetSocketAddress(this.address, this.port);
        channel.connect(remoteServerAddress);
        channel.register(this.selector, SelectionKey.OP_CONNECT);
    }

    private void handleConnect(SelectionKey key) throws IOException {
        Logd(TAG, format("handleConnect(%s) called.", Util.toString(key)));

        SocketChannel channel = (SocketChannel) key.channel();
        if(!channel.finishConnect()) {
            Logw(TAG, "channel not yet connected, waiting.");
            return;
        }
        Logi(TAG, "channel connection established, switching to listen for RW events.");
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        this.callback.onConnect();
    }

    private void handleWrite(SelectionKey key) throws IOException {
        Logd(TAG, format("handleWrite(%s) called.", Util.toString(key)));

        String data = this.callback.sendData();

        if(null != data && data.trim().length() > 0) {
            ByteBuffer buf;
            int bytesToWrite;

            if(key.attachment() == null) {
                byte[] dataBytes = data.trim().getBytes();
                bytesToWrite = dataBytes.length + TERMINATOR_BYTES.length;

                buf = ByteBuffer.allocate(bytesToWrite);
                buf.put(dataBytes);
                buf.put(TERMINATOR_BYTES);
                buf.flip();
            } else {
                buf = (ByteBuffer) key.attachment();
                bytesToWrite = buf.remaining();
            }

            SocketChannel channel = (SocketChannel) key.channel();
            int bytesWritten = channel.write(buf);

            if(bytesWritten == bytesToWrite) {
                Logi(TAG, format("wrote %d bytes to remote server.", bytesWritten));
                key.attach(null);
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                this.callback.onDataDelivered();
            } else {
                Logw(TAG, "write not yet finished, waiting for channel to be ready for writing.");
                key.attach(buf);
                key.interestOps(SelectionKey.OP_WRITE);
                //noinspection UnnecessaryReturnStatement
                return;
            }
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        Logd(TAG, format("handleRead(%s) called.", Util.toString(key)));

        SocketChannel channel = (SocketChannel) key.channel();


        ByteBuffer buffer;
        if(key.attachment() == null) {
            buffer = ByteBuffer.allocate(this.readBufferSize);
        } else {
            buffer = (ByteBuffer) key.attachment();
        }

        int numRead;
        int readAttemptCount=0;
        while ((numRead = channel.read(buffer)) > 0) {
            // read may take multiple attempts.
            Logd(TAG, format("read attempt #%d", ++readAttemptCount));
        }

        if (numRead == -1) {
            handleDisconnect(key);
            return;
        }

        buffer.flip();

        CharBuffer charBuffer = charset.decode(buffer);
        String dataReceived = new String(charBuffer.array());
        Logd(TAG, format("Data read [%s]", dataReceived));

        this.callback.onDataReceived(dataReceived);
    }

    private void handleDisconnect(SelectionKey key) throws IOException {
        Logd(TAG, format("handleDisconnect(%s) called.", Util.toString(key)));

        SocketChannel channel = (SocketChannel)key.channel();
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        Logi(TAG, format("Connection closed by server: %s", remoteAddr));
        channel.close();
        key.cancel();

        this.callback.onDisconnect();
    }

    private void handleError(Throwable throwable) {
       Loge(TAG, format("caught %s [%s]", throwable.getClass().getSimpleName(), throwable.getMessage()));
       this.callback.onError(throwable);
    }
}

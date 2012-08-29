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
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

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
    private SocketChannel channel;
    private InetAddress address;
    private int port;
    private IOClientCallback callback;
    private Charset charset;
    
    private Deque<byte[]> dataQueue;
    private int bufferSize;

    public IOClientImpl(Charset charset, String host, Integer port, IOClientCallback callback, int bufferSize) throws UnknownHostException {
        this.charset = charset;
        this.address = InetAddress.getByName(host);
        this.port = port;
        this.callback = callback;
        this.dataQueue = new ConcurrentLinkedDeque<byte[]>();
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        try {
            this.startClient();
        } catch (IOException e) {
            handleError(e);
        }
    }

    public void sendMessage(String message) {
    	if(Util.notEmpty(message))
    		this.dataQueue.addLast(message.getBytes());
    }
    
    @Override
    public void shutdown() throws IOException {
        Logi(TAG, "shutdown requested.");
        if(null != this.selector && this.selector.isOpen()) {
            this.selector.close();
        }
        
        if(null != channel) {
        	this.channel.close();
        }

        if(null != dataQueue && !dataQueue.isEmpty()) {
        	dataQueue.clear();
        }
        
        this.callback.onShutdown();
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

                if (!key.isValid()) {
                    continue;
                }

                if (key.isConnectable()) {
                    this.handleConnect(key);
                }

                if (key.isWritable()) {
                    boolean writeComplete = this.handleWrite(key);
                    if(!writeComplete) {
                    	// key should not be removed
                    	continue;
                    }
                }

                if (key.isReadable()) {
                    this.handleRead(key);
                }

                keys.remove();
            }
        }
    }

    private void initalizeChannel() throws IOException {
        Logd(TAG, "initializeChannel() called.");

        channel = SocketChannel.open();
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

    private boolean handleWrite(SelectionKey key) throws IOException {
        Logd(TAG, format("handleWrite(%s) called.", Util.toString(key)));

        synchronized(dataQueue) {
            if(!dataQueue.isEmpty()) {
            	byte[] data;
            	// even though the method is named "poll" this is not a blocking call.
            	// also, poll removes the data from the queue; we add back any 
            	// remaining if the write was incomplete
            	while( (data = dataQueue.poll()) != null) {
            		int bytesToWrite = data.length + TERMINATOR_BYTES.length;
            		ByteBuffer buf = ByteBuffer.allocate(bytesToWrite);
                    buf.put(data);
                    buf.put(TERMINATOR_BYTES);
                    buf.flip();
                    SocketChannel channel = (SocketChannel) key.channel();
                    int bytesWritten = channel.write(buf);
                    if(bytesWritten == bytesToWrite) {
	                    Logi(TAG, format("wrote %d bytes to remote server.", bytesWritten));
	                    key.interestOps(SelectionKey.OP_READ);
	                    this.callback.onDataDelivered();
                    } else {
                    	Logi(TAG, format("partial write: %d of %d bytes written.", bytesWritten, bytesToWrite));
                    	// handle partial write by replacing the tip of the queue with the data unwritten.
                    	byte[] remaining = new byte[bytesToWrite-bytesWritten];
                    	System.arraycopy(data, bytesWritten, remaining, 0, bytesToWrite-bytesWritten);
                    	dataQueue.addFirst(remaining);
	                    key.interestOps(SelectionKey.OP_WRITE);
	                    return false;
                    }
            	}
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
        return true;
    }

    private void handleRead(SelectionKey key) throws IOException {
        Logd(TAG, format("handleRead(%s) called.", Util.toString(key)));

        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer;
        if(key.attachment() == null) {
            buffer = ByteBuffer.allocate(this.bufferSize);
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
        this.dataQueue.remove(channel);
        this.callback.onDisconnect();
    }

    private void handleError(Throwable throwable) {
       Loge(TAG, format("caught %s [%s]", throwable.getClass().getSimpleName(), throwable.getMessage()));
       this.callback.onError(throwable);
    }
}

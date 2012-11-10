package com.bpcreates.ioclient;

import static com.bpcreates.common.Util.Logd;
import static com.bpcreates.common.Util.Loge;
import static com.bpcreates.common.Util.Logi;
import static com.bpcreates.common.Util.Logw;
import static com.bpcreates.common.Util.TERMINATOR_BYTES;
import static java.lang.String.format;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import com.bpcreates.common.Util;

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
    private final IOClientCallback callback;
    private final Charset charset;
    private final Deque<byte[]> dataQueue;
    private final int bufferSize;
    private final String host;
    private final Integer port;

    public IOClientImpl(Charset charset, String host, Integer port, IOClientCallback callback, int bufferSize) throws IOException {
        Logd(TAG, format("Constructing IOClientImpl using [%s][%s:%d][%s][%d]",charset.displayName(), host,port, callback.getClass().getSimpleName(), bufferSize));
        this.charset = charset;
        this.host = host;
        this.port = port;
        this.callback = callback;
// JDK7        this.dataQueue = new ConcurrentLinkedDeque<byte[]>();
        this.dataQueue = new LinkedList<byte[]>();
        this.bufferSize = bufferSize;

        // http://stackoverflow.com/a/7453555/87408
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    @Override
    public void run() {
        Logd(TAG,"running IOClientImpl");
        try {
            this.initialize();
            this.startClient();
        } catch (IOException e) {
            handleError(e);
        }
    }

    private void initialize() throws IOException {
        Logd(TAG, "initializing IOClientImpl...");
        this.selector = Selector.open();
        Logd(TAG, "... selector opened.");
        channel = SocketChannel.open();
        Logd(TAG, "... channel opened.");
        channel.configureBlocking(false);
        Logd(TAG, "... channel configured.");

        InetAddress address = InetAddress.getByName(host);
        Logd(TAG, format("... resolved host (%s) to address (%s)", host, address.getHostAddress()));
        InetSocketAddress remoteServerAddress = new InetSocketAddress(address, port);
        Logd(TAG, "... configured remote server address");
        channel.connect(remoteServerAddress);
        Logd(TAG, "... connected remote server address to channel");
        channel.register(this.selector, SelectionKey.OP_CONNECT);
        Logd(TAG, "... registered selector for this channel.");
    }

    // todo jdk7 this syncronization can be removed
    public synchronized void sendMessage(String message) {
        Logi(TAG, format("sendMessage(%s)", message));
        if(Util.notEmpty(message)) {
            Logd(TAG, format("adding message (%s) to dataQueue with %d pending messages", message, dataQueue.size()));
            this.dataQueue.addLast(message.getBytes());
        }
        Logd(TAG, format("dataQueue has %d pending messages", dataQueue.size()));
    }
    
    @Override
    public void shutdown() throws IOException {
        Logi(TAG, "shutdown requested.");
        cleanUp();
        Logi(TAG, "ioClient shut down complete.");
        this.callback.onClientShutdown();
    }

    private void cleanUp() throws IOException {
        if(null != this.selector && this.selector.isOpen()) {
            this.selector.close();
            Logd(TAG, "selector closed.");
        }
        
        if(null != channel) {
            this.channel.close();
            Logd(TAG, "channel closed.");
        }

        // todo jdk7 this syncronization can be removed
        if(null != dataQueue) {
            Logd(TAG, format("dataQueue has %d pending messages.", dataQueue.size()));
            synchronized (dataQueue) {
                if(!dataQueue.isEmpty()) {
                    dataQueue.clear();
                    Logd(TAG, "dataQueue cleared.");
                }
            }
        }        
    }

    private void startClient() throws IOException {
        Logd(TAG, "startClient() called.");
        
        while (selector.isOpen()) {
            int count = selector.select();

            Logd(TAG, format("selector is open, got %d selected channels.", count));
            if (count == 0) {
                continue;
            }

            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                Logd(TAG, format("obtained selection key: %s", Util.toString(key)));
                if (!key.isValid()) {
                    continue;
                }

                if (key.isConnectable()) {
                    Logd(TAG, "key is connectable.");
                    this.handleConnect(key);
                }

                if (key.isWritable()) {
                    Logd(TAG, "key is writable.");
                    boolean writeComplete = this.handleWrite(key);
                    if(!writeComplete) {
                        Logd(TAG, "write is incomplete, continuing write later.");
                        // key should not be removed
                        continue;
                    }
                }

                if (key.isReadable()) {
                    Logd(TAG, "key is readable.");
                    this.handleRead(key);
                }

                Logd(TAG, "finished with this selector, removing.");
                keys.remove();
            }
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        Logd(TAG, format("handleConnect(%s) called.", Util.toString(key)));

        SocketChannel channel = (SocketChannel) key.channel();
        Logd(TAG, "attempting to finish connection.");
        if(!channel.finishConnect()) {
            Logw(TAG, "channel not yet connected, waiting.");
            return;
        }
        Logi(TAG, "channel connection established, switching to listen for RW events.");
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        this.callback.onClientConnect();
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
                        this.callback.onClientDataDelivered();
                    } else {
                        Logi(TAG, format("partial write: %d of %d bytes written.", bytesWritten, bytesToWrite));
                        // handle partial write by replacing the tip of the queue with the data unwritten.
                        byte[] remaining = new byte[bytesToWrite-bytesWritten];
                        System.arraycopy(data, bytesWritten, remaining, 0, bytesToWrite-bytesWritten);
                        dataQueue.addFirst(remaining);
                        return false;
                    }
            	}
            }
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
        // chop off trailing null
        String dataReceived = charBuffer.subSequence(0, charBuffer.length()-1).toString();
        Logd(TAG, format("Data read [%s]", dataReceived));

        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        
        this.callback.onClientDataReceived(dataReceived);
    }

    private void handleDisconnect(SelectionKey key) throws IOException {
        Logd(TAG, format("handleDisconnect(%s) called.", Util.toString(key)));

        SocketChannel channel = (SocketChannel)key.channel();
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        Logi(TAG, format("Connection closed by server: %s", remoteAddr));
        channel.close();
        key.cancel();
        // todo jdk7 this syncronization can be removed
        synchronized (dataQueue) {
            this.dataQueue.clear();
        }
        this.callback.onClientDisconnect();
    }

    private void handleError(Throwable throwable) {
       Loge(TAG, format("caught %s [%s]", throwable.getClass().getSimpleName(), throwable.getMessage()));
       try {
        cleanUp();
        } catch (IOException e) {
            // jdk7 attach this IOException to the throwable
            e.printStackTrace(System.err);
        }
       this.callback.onClientError(throwable);
    }
}

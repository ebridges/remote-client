package com.bpcreates.remoteclient;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ebridges
 * Date: 8/18/12
 * Time: 11:38 AM
 */

public class EchoServer {
    private InetAddress addr;
    private int port;
    private Selector selector;
    private Map<SocketChannel,List<byte[]>> dataMap;
    private boolean started;

    public EchoServer(InetAddress addr, int port) throws IOException {
        this.addr = addr;
        this.port = port;
        dataMap = new HashMap<SocketChannel,List<byte[]>>();
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    public void stopServer() throws IOException {
        for(SocketChannel channel : dataMap.keySet()) {
            if(null != channel && channel.isConnected()) {
                channel.close();
            }
        }
        if(null != selector && selector.isOpen()) {
            selector.close();
        }
    }

    public void startServer() throws IOException {
        // create selector and channel
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // bind to port
        InetSocketAddress listenAddr = new InetSocketAddress(this.addr, this.port);
        serverChannel.socket().bind(listenAddr);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

     //   log("Echo server ready. Ctrl-C to stop.");

        //noinspection InfiniteLoopStatement
        while (true) {
            started = true;
            // wait for events
            this.selector.select();

            // wakeup to work on selected keys
            Iterator keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();

                // this is necessary to prevent the same key from coming up
                // again the next time around.
                keys.remove();

                if (! key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    this.accept(key);
                }
                else if (key.isReadable()) {
                    this.read(key);
                }
                else if (key.isWritable()) {
                    this.write(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        log("accept() called with key: "+key);
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        // write welcome message
        //channel.write(ByteBuffer.wrap("Welcome, this is the echo server\r\n".getBytes("US-ASCII")));

        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
  //      log("Connected to: " + remoteAddr);

        // register channel with selector for further IO
        dataMap.put(channel, new ArrayList<byte[]>());
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        log("read() called with key: "+key);
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int numRead = -1;
        try {
            numRead = channel.read(buffer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (numRead == -1) {
            this.dataMap.remove(channel);
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            log("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        log("Got: " + new String(data, "US-ASCII"));

        // write back to client
        doEcho(key, data);
    }

    private void write(SelectionKey key) throws IOException {
        log("write() called with key "+ key);
        SocketChannel channel = (SocketChannel) key.channel();
        if(this.dataMap.keySet().size() > 1) {
            log("multiple channels");
            for(Map.Entry<SocketChannel,List<byte[]>> me : this.dataMap.entrySet()) {
                log("channel: "+channel);
                if(!me.getKey().equals(channel)) {
                    Iterator<byte[]> items = me.getValue().iterator();
                    while (items.hasNext()) {
                        byte[] item = items.next();
                        items.remove();
                        channel.write(ByteBuffer.wrap(item));
                    }
                }
            }
        } else {
            List<byte[]> pendingData = this.dataMap.get(channel);
            Iterator<byte[]> items = pendingData.iterator();
            while (items.hasNext()) {
                byte[] item = items.next();
                items.remove();
                channel.write(ByteBuffer.wrap(item));
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void doEcho(SelectionKey key, byte[] data) {
        log("doEcho() called with key "+ key);
        SocketChannel channel = (SocketChannel) key.channel();
        if(this.dataMap.keySet().size() > 1) {
            log("multiple channels");
            for(Map.Entry<SocketChannel,List<byte[]>> me : this.dataMap.entrySet()) {
                log("channel: "+channel);
                if(!me.getKey().equals(channel)) {
                    me.getValue().add(data);
                }
            }
        } else {
            List<byte[]> pendingData = this.dataMap.get(channel);
            pendingData.add(data);
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private static void log(String s) {
        System.out.println(s);
    }

    public static void main(String[] args) throws Exception {
        new EchoServer(null, 8989);
    }
}
package com.bpcreates.common;

import static com.bpcreates.common.Util.Logd;
import static com.bpcreates.common.Util.Logi;
import static java.lang.String.format;

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
 * Created with IntelliJ IDEA. User: ebridges Date: 8/18/12 Time: 11:38 AM
 */

public class EchoServer implements Runnable {

  private static final String TAG = EchoServer.class.getSimpleName();

  private InetAddress addr;

  private int port;

  private Selector selector;

  private Map<SocketChannel, List<byte[]>> dataMap;

  private boolean started;

  public EchoServer(InetAddress addr, int port) throws IOException {
    this.addr = addr;
    this.port = port;
    dataMap = new HashMap<SocketChannel, List<byte[]>>();
    started = false;
  }

  public EchoServer(String host, int port) throws IOException {
    this(InetAddress.getByName(host), port);
  }

  @Override
  public void run() {
    try {
      this.startServer();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean isStarted() {
    return started;
  }

  public void stopServer() throws IOException {
    Logd(TAG, "stopServer() called.");
    for (SocketChannel channel : dataMap.keySet()) {
      if (null != channel && channel.isConnected()) {
        channel.close();
      }
    }
    if (null != selector && selector.isOpen()) {
      selector.close();
    }
  }

  public void startServer() throws IOException {
    Logd(TAG, "startServer() called.");
    // create selector and channel
    this.selector = Selector.open();
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);

    // bind to port
    InetSocketAddress listenAddr = new InetSocketAddress(this.addr, this.port);
    serverChannel.socket().bind(listenAddr);
    serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    Logi(TAG, format("server listening on %s:%d", this.addr.getHostAddress(), this.port));

    //noinspection InfiniteLoopStatement
    while (true) {
      started = true;
      // wakeup to work on selected keys
      if(this.selector.isOpen()) {
        // wait for events
        this.selector.select();

        Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
        while (keys.hasNext()) {
          SelectionKey key = keys.next();

          // this is necessary to prevent the same key from coming up
          // again the next time around.
          keys.remove();

          if (!key.isValid()) {
            continue;
          }

          if (key.isAcceptable()) {
            this.accept(key);
          } else if (key.isReadable()) {
            this.read(key);
          } else if (key.isWritable()) {
            this.write(key);
          }
        }
      } else {
        break;
      }
    }
  }

  private void accept(SelectionKey key) throws IOException {
    Logd(TAG, format("accept() called with key: [%s]", Util.toString(key)));
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel channel = serverChannel.accept();
    channel.configureBlocking(false);

    // register channel with selector for further IO
    dataMap.put(channel, new ArrayList<byte[]>());
    channel.register(this.selector, SelectionKey.OP_READ);
  }

  private void read(SelectionKey key) throws IOException {
    Logd(TAG, format("read() called with key: [%s] ", Util.toString(key)));
    SocketChannel channel = (SocketChannel) key.channel();

    ByteBuffer buffer = ByteBuffer.allocate(8192);
    int numRead = -1;
    try {
      numRead = channel.read(buffer);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (numRead == -1) {
      this.dataMap.remove(channel);
      Socket socket = channel.socket();
      SocketAddress remoteAddr = socket.getRemoteSocketAddress();
      Logi(TAG, format("Connection closed by client: %s", remoteAddr));
      channel.close();
      key.cancel();
      return;
    }

    byte[] data = new byte[numRead];
    System.arraycopy(buffer.array(), 0, data, 0, numRead);
    Logd(TAG, format("Data received: [%s]", new String(data, "US-ASCII")));

    // write back to client
    doEcho(key, data);
  }

  private void write(SelectionKey key) throws IOException {
    Logd(TAG, format("write() called with key [%s]", Util.toString(key)));
    SocketChannel channel = (SocketChannel) key.channel();
    if (this.dataMap.keySet().size() > 1) {
      for (Map.Entry<SocketChannel, List<byte[]>> me : this.dataMap.entrySet()) {
        if (!me.getKey().equals(channel)) {
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
    Logd(TAG, format("doEcho() called with key [%s]", Util.toString(key)));
    SocketChannel channel = (SocketChannel) key.channel();
    if (this.dataMap.keySet().size() > 1) {
      for (Map.Entry<SocketChannel, List<byte[]>> me : this.dataMap.entrySet()) {
        if (!me.getKey().equals(channel)) {
          me.getValue().add(data);
        }
      }
    } else {
      List<byte[]> pendingData = this.dataMap.get(channel);
      pendingData.add(data);
    }
    key.interestOps(SelectionKey.OP_WRITE);
  }
}

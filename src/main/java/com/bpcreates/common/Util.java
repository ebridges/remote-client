package com.bpcreates.common;

import static java.lang.String.format;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: ebridges
 * Date: 7/22/12
 * Time: 5:00 PM
 */
public class Util {
    public static final String DEFAULT_CHARSET="UTF-8";
    public static final char TERMINATOR = '\u0000';
    public static final byte[] TERMINATOR_BYTES = new byte[]{TERMINATOR};

    public static boolean notEmpty(String v) {
        return null != v && v.trim().length() > 0;
    }

    public static boolean isEmpty(String v) {
        return null == v || v.trim().isEmpty();
    }

    public static void Logd(String tag, String mesg) {
        System.out.println(format("[D] [%s] [%s] %s", now(), tag, mesg));
    }

    public static void Logi(String tag, String mesg) {
        System.out.println(format("[I] [%s] [%s] %s", now(), tag, mesg));
    }

    public static void Logw(String tag, String mesg) {
        System.out.println(format("[W] [%s] [%s] %s", now(), tag, mesg));
    }

    public static void Loge(String tag, String mesg) {
        System.err.println(format("[E] [%s] [%s] Error: %s", now(), tag, mesg));
    }

    public static void Loge(String tag, String mesg, Throwable t) {
        System.err.println(format("[E] [%s] [%s] Error: (%s) %s", now(), tag, t.getMessage(), mesg));
    }

    public static String now() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return fmt.format(new Date());
    }

    public static byte[] intToByteArray (final int integer) {
        byte[] result = new byte[4];

        result[0] = (byte)((integer & 0xFF000000) >> 24);
        result[1] = (byte)((integer & 0x00FF0000) >> 16);
        result[2] = (byte)((integer & 0x0000FF00) >> 8);
        result[3] = (byte)(integer & 0x000000FF);

        return result;
    }

    public static String toString(SelectionKey key) {
      AbstractSelectableChannel channel = (AbstractSelectableChannel) key.channel();
      InetAddress address;
      Integer portnum;
      if(channel instanceof SocketChannel) {
        address = ((SocketChannel) channel).socket().getInetAddress();
        portnum = ((SocketChannel) channel).socket().getPort();
      } else if(channel instanceof ServerSocketChannel) {
        address = ((ServerSocketChannel) channel).socket().getInetAddress();
        portnum = ((ServerSocketChannel) channel).socket().getLocalPort();
      } else {
        address = null;
        portnum = null;
      }

      if(address != null && portnum != null) {
        return format("%s:%d A:%s|C:%s|R:%s|W:%s",
            address,
            portnum,
            key.isAcceptable(),
            key.isConnectable(),
            key.isReadable(),
            key.isWritable());
      } else {
        return format("A:%s|C:%s|R:%s|W:%s",
            key.isAcceptable(),
            key.isConnectable(),
            key.isReadable(),
            key.isWritable());
      }
    }

    private Util() {
    }

  public static String clean(String s) {
    if(notEmpty(s)) {
      String ss = s.trim();
      ss = ss.replaceAll("\n", "");
      ss = ss.replaceAll("\r", "");
      ss = ss.replace("\t+", " ");
      ss = ss.replaceAll("\\s+", " ");
      return ss;
    }
    return "";
  }
}

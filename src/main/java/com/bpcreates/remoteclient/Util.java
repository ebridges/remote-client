package com.bpcreates.remoteclient;

import static java.lang.String.format;

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

    public static boolean notEmpty(String v) {
        return null != v && v.trim().length() > 0;
    }

    public static void Logd(String tag, String mesg) {
        System.out.println(format("[D] [%s] [%s] %s", now(), tag, mesg));
    }

    public static void Logi(String tag, String mesg) {
        System.out.println(format("[I] [%s] [%s] %s", now(), tag, mesg));
    }

    public static void Loge(String tag, String mesg) {
        System.out.println(format("[E] [%s] [%s] Error: %s", now(), tag, mesg));
    }

    public static void Loge(String tag, String mesg, Throwable t) {
        System.out.println(format("[E] [%s] [%s] Error: (%s) %s", now(), tag, t.getMessage(), mesg));
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

    private Util() {
    }
}

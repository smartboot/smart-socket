/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: StringUtils.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.util;

/**
 * 扩展StringUtils方法
 *
 * @author 三刀
 * @version StringUtils.java, v 0.1 2015年8月25日 下午2:48:21 Seer Exp.
 */
public class StringUtils {

    /**
     * 秘钥Key
     */
    public static final String SECRET_KEY = "_SecretKey_";
    private final static char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final int TWO = 2;

    /**
     * 将字节转换成16进制显示
     *
     * @param b byte
     * @return String
     */
    public static String toHex(byte b) {
        final char[] buf = new char[TWO];
        for (int i = 0; i < buf.length; i++) {
            buf[1 - i] = DIGITS[b & 0xF];
            b = (byte) (b >>> 4);
        }
        return new String(buf);
    }

    /**
     * 以16进制 打印字节数组
     *
     * @param bytes byte[]
     */
    public static String toHexString(final byte[] bytes) {
        final StringBuffer buffer = new StringBuffer(bytes.length);
        buffer.append("\r\n\t   0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f\r\n");
        int startIndex = 0;
        int column = 0;
        for (int i = 0; i < bytes.length; i++) {
            column = i % 16;
            switch (column) {
                case 0:
                    startIndex = i;
                    buffer.append(fixHexString(Integer.toHexString(i), 8)).append(": ");
                    buffer.append(toHex(bytes[i]));
                    buffer.append(" ");
                    break;
                case 15:
                    buffer.append(toHex(bytes[i]));
                    buffer.append(" ; ");
                    buffer.append(filterString(bytes, startIndex, column + 1));
                    buffer.append("\r\n");
                    break;
                default:
                    buffer.append(toHex(bytes[i]));
                    buffer.append(" ");
            }
        }
        if (column != 15) {
            for (int i = 0; i < 15 - column; i++) {
                buffer.append("   ");
            }
            buffer.append("; ").append(filterString(bytes, startIndex, column + 1));
            buffer.append("\r\n");
        }

        return buffer.toString();
    }

    /**
     * 过滤掉字节数组中0x0 - 0x1F的控制字符，生成字符串
     *
     * @param bytes  byte[]
     * @param offset int
     * @param count  int
     * @return String
     */
    private static String filterString(final byte[] bytes, final int offset, final int count) {
        final byte[] buffer = new byte[count];
        System.arraycopy(bytes, offset, buffer, 0, count);
        for (int i = 0; i < count; i++) {
            if (buffer[i] >= 0x0 && buffer[i] <= 0x1F) {
                buffer[i] = 0x2e;
            }
        }
        return new String(buffer);
    }

    /**
     * 将hexStr格式化成length长度16进制数，并在后边加上h
     *
     * @param hexStr String
     * @return String
     */
    private static String fixHexString(final String hexStr, final int length) {
        if (hexStr == null || hexStr.length() == 0) {
            return "00000000h";
        } else {
            final StringBuffer buf = new StringBuffer(length);
            final int strLen = hexStr.length();
            for (int i = 0; i < length - strLen; i++) {
                buf.append("0");
            }
            buf.append(hexStr).append("h");
            return buf.toString();
        }
    }

}

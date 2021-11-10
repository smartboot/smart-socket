/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: DelimiterFrameDecoderTest.java
 * Date: 2021-11-07
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package com.smartboot.socket.decoder;

import org.junit.Assert;
import org.junit.Test;
import org.smartboot.socket.extension.decoder.DelimiterFrameDecoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/11/7
 */
public class DelimiterFrameDecoderTest {

    @Test
    public void testDecoder1() {
        check("abcd", "c");
    }

    @Test
    public void testDecoder2() {
        check("aaabcd", "aab");
    }

    @Test
    public void testDecoder3() {
        check("abaaababa", "aab");
    }

    @Test
    public void testDecoder4() {
        check("abaaababa", "abab");
    }

    @Test
    public void testDecoder5() {
        check("aaabaabb", "aabb");
    }

    private void check(String data, String endFlag) {
        byte[] endBytes = endFlag.getBytes(StandardCharsets.UTF_8);
        DelimiterFrameDecoder decoder = new DelimiterFrameDecoder(endBytes, 512);
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
        if (decoder.decode(buffer)) {
            ByteBuffer byteBuffer = decoder.getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            System.out.println(new String(bytes));
            Assert.assertEquals(bytes.length, data.indexOf(endFlag) + endFlag.length());
        } else {
            Assert.fail();
        }

    }
}

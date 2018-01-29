/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: FormWithContentLengthStrategy.java
 * Date: 2018-01-23
 * Author: sandao
 */

package org.smartboot.socket.http.strategy;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.http.HttpV2Entity;

/**
 * Post普通表单提交
 *
 * @author 三刀
 * @version V1.0 , 2017/9/3
 */
public class FormWithContentLengthStrategy implements PostDecodeStrategy {
    @Override
    public boolean waitForBodyFinish() {
        return true;
    }

    @Override
    public boolean isDecodeEnd(byte b, HttpV2Entity entity) {
//        //识别body长度
//        if (entity.headDecoder1.getContentLength() <= 0) {
//            entity.headDecoder1.setContentLength(entity.getContentLength());
//        }
//        if (entity.headDecoder1.append(b)) {
//            String[] headDatas = StringUtils.split(entity.headDecoder1.toString(), "&");
//            if (ArrayUtils.isEmpty(headDatas)) {
//                throw new RuntimeException("data is emtpy");
//            }
//            for (int i = 0; i < headDatas.length; i++) {
//                entity.getParamMap().put(StringUtils.substringBefore(headDatas[i], "=").trim(), StringUtils.substringAfter(headDatas[i], "=").trim());
//            }
//            entity.headDecoder1.reset();
//            return true;
//        }
        return false;
    }

}

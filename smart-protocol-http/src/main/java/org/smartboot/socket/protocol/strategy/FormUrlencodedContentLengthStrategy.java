package org.smartboot.socket.protocol.strategy;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.smartboot.socket.protocol.HttpV2Entity;

/**
 * Post普通表单提交
 *
 * @author Seer
 * @version V1.0 , 2017/9/3
 */
public class FormUrlencodedContentLengthStrategy implements PostDecodeStrategy {
    @Override
    public boolean waitForBodyFinish() {
        return false;
    }

    @Override
    public boolean isDecodeEnd(byte b, HttpV2Entity entity) {
        //识别body长度
        if (entity.dataStream.getContentLength() <= 0) {
            entity.dataStream.setContentLength(entity.getContentLength());
        }
        if (entity.dataStream.append(b)) {
            String[] headDatas = StringUtils.split(entity.dataStream.toString(), "&");
            if (ArrayUtils.isEmpty(headDatas)) {
                throw new RuntimeException("data is emtpy");
            }
            for (int i = 0; i < headDatas.length; i++) {
                entity.getParamMap().put(StringUtils.substringBefore(headDatas[i], "=").trim(), StringUtils.substringAfter(headDatas[i], "=").trim());
            }
            entity.dataStream.reset();
            return true;
        }
        return false;
    }

}

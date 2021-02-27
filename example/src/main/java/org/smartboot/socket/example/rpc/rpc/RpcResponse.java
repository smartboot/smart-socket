/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: RpcResponse.java
 * Date: 2021-02-27
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.example.rpc.rpc;

import java.io.Serializable;

/**
 * RPC响应消息
 *
 * @author Seer
 * @version RpcResponse.java, v 0.1 2015年11月20日 下午9:11:03 Seer Exp.
 */
public class RpcResponse implements Serializable {
    /**
     * 消息的唯一标示，与对应的RpcRequest uuid值相同
     */
    private String uuid;
    /**
     * 返回对象
     */
    private Object returnObject;

    /**
     * 返回对象类型
     */
    private String returnType;

    /**
     * 异常
     */
    private String exception;


    public RpcResponse(String uuid) {
        this.uuid = uuid;
    }

    public Object getReturnObject() {
        return returnObject;
    }

    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getUuid() {
        return uuid;
    }

}

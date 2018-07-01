package org.smartboot.socket.example.rpc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/7/1
 */
public class RpcConsumerProcessor implements MessageProcessor<byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcConsumerProcessor.class);
    private Map<String, Object> synchRespMap = new ConcurrentHashMap<>();
    private Map<Class, Object> objectMap = new ConcurrentHashMap<>();
    private AioSession<byte[]> aioSession;


    @Override
    public void process(AioSession<byte[]> session, byte[] msg) {
        ObjectInput objectInput = null;
        try {
            objectInput = new ObjectInputStream(new ByteArrayInputStream(msg));
            RpcResponse resp = (RpcResponse) objectInput.readObject();
            Object reqMsg = synchRespMap.get(resp.getUuid());
            if (reqMsg != null) {
                synchronized (reqMsg) {
                    if (synchRespMap.containsKey(resp.getUuid())) {
                        synchRespMap.put(resp.getUuid(), resp);
                        reqMsg.notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (objectInput != null) {
                try {
                    objectInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public <T> T getObject(final Class<T> remoteInterface) {
        Object obj = objectMap.get(remoteInterface);
        if (obj != null) {
            return (T) obj;
        }
        obj = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{remoteInterface},
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RpcRequest req = new RpcRequest();
                        req.setInterfaceClass(remoteInterface.getName());
                        req.setMethod(method.getName());
                        Class<?>[] types = method.getParameterTypes();
                        if (!ArrayUtils.isEmpty(types)) {
                            String[] paramClass = new String[types.length];
                            for (int i = 0; i < types.length; i++) {
                                paramClass[i] = types[i].getName();
                            }
                            req.setParamClassList(paramClass);
                        }
                        req.setParams(args);

                        RpcResponse rmiResp = sendRpcRequest(req);
                        if (StringUtils.isNotBlank(rmiResp.getException())) {
                            throw new RuntimeException(rmiResp.getException());
                        }
                        return rmiResp.getReturnObject();
                    }
                });
        objectMap.put(remoteInterface, obj);
        return (T) obj;
    }


    private final RpcResponse sendRpcRequest(RpcRequest request) throws Exception {
        synchRespMap.put(request.getUuid(), request);

        //输出消息
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream);
        objectOutput.writeObject(request);
        aioSession.write(byteArrayOutputStream.toByteArray());

        if (synchRespMap.containsKey(request.getUuid()) && synchRespMap.get(request.getUuid()) == request) {
            synchronized (request) {
                if (synchRespMap.containsKey(request.getUuid()) && synchRespMap.get(request.getUuid()) == request) {
                    try {
                        request.wait();
                    } catch (InterruptedException e) {
                        LOGGER.warn("", e);
                    }
                }
            }
        }
        Object resp = null;
        synchronized (request) {
            resp = synchRespMap.remove(request.getUuid());
        }
        if (resp == null || resp == request) {
            throw new SocketTimeoutException("Message is timeout!" + resp);
        }
        if (resp instanceof RpcResponse) {
            return (RpcResponse) resp;
        }
        throw new RuntimeException("invalid response " + resp);
    }

    @Override
    public void stateEvent(AioSession<byte[]> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                this.aioSession = session;
                break;
        }
    }

}

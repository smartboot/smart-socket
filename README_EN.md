smart-socket is a AIO client server framework which enables quick and easy development of network applications such as protocol servers and clients.It greatly simplifies and streamlines network programming such as TCP  socket server(unsupport UDP).

## Features
- Better throughput, lower latency
- Less resource consumptions
- less code 



## Quick Start

### Maven Dependency
The smart-socket jar file is available from [Maven Central](http://mvnrepository.com/artifact/org.smartboot.socket/aio-core) and can be integrated into your dependency manager of choice from there.
```xml
<dependency>
    <groupId>org.smartboot.socket</groupId>
    <artifactId>aio-core</artifactId>
    <version>1.3.23</version>
</dependency>
```

### Demo
#### Protocol

```java
public class IntegerProtocol implements Protocol<Integer> {

    private static final int INT_LENGTH = 4;

    @Override
    public Integer decode(ByteBuffer data, AioSession<Integer> session, boolean eof) {
        if (data.remaining() < INT_LENGTH)
            return null;
        return data.getInt();
    }

    @Override
    public ByteBuffer encode(Integer s, AioSession<Integer> session) {
        ByteBuffer b = ByteBuffer.allocate(INT_LENGTH);
        b.putInt(s);
        b.flip();
        return b;
    }
}
```
#### Writing a server

```java
public class IntegerServerProcessor implements MessageProcessor<Integer> {
    @Override
    public void process(AioSession<Integer> session, Integer msg) {
        Integer respMsg = msg + 1;
        System.out.println("receive data from client: " + msg + " ,rsp:" + (respMsg));
        try {
            session.write(respMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<Integer> session, StateMachineEnum stateMachineEnum, Throwable throwable) {

    }
}
```


```java
public class IntegerServer {
    public static void main(String[] args) {
        AioQuickServer<Integer> server = new AioQuickServer<Integer>()
                .bind(8888)
                .setProtocol(new IntegerProtocol())
                .setProcessor(new IntegerServerProcessor());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```
#### Writing a client
In our case all we want to do is print the value out the the console.
```java
public class IntegerClientProcessor implements MessageProcessor<Integer> {
    private AioSession<Integer> session;

    @Override
    public void process(AioSession<Integer> session, Integer msg) {
        System.out.println("receive data from server：" + msg);
    }

    @Override
    public void stateEvent(AioSession<Integer> session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                this.session = session;
                break;
            default:
                System.out.println("other state:" + stateMachineEnum);
        }

    }

    public AioSession<Integer> getSession() {
        return session;
    }
}
```

```java
public class IntegerClient {
    public static void main(String[] args) throws Exception {
        IntegerClientProcessor processor = new IntegerClientProcessor();
        AioQuickClient<Integer> aioQuickClient = new AioQuickClient<Integer>()
                .connect("localhost", 8888)
                .setProtocol(new IntegerProtocol())
                .setProcessor(processor);
        aioQuickClient.start();
        processor.getSession().write(1);
        Thread.sleep(1000);
        aioQuickClient.shutdown();
    }
}
```

## License

## About us
Edit By 三刀(sandao)  
E-mail:zhengjunweimail@163.com  

Update Date: 2017-11-20
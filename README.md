## 友情提示
如果有朋友用了smart-socket觉得它还不错，并且做出了可开源的案例，烦请告知本人或在评论下留言。因日常工作较忙，争取每个周末保持更新进度，同时希望大家一起支持开源，助力开源的发展，谢谢！！！
## 简介
过去的几年一直在研究NIO技术，虽说没做出什么成绩，但对于Socket技术也是有了一定的了解。从2017年7月份正式开始接触AIO，尽管起步比较晚，但进步还是非常快的。断断续续的用了几周时间写了这个AIO版的通信框架smart-socket，目前的它非常轻量级,核心代码量才500多行，期待它成长后的模样。


## Maven

    <dependency>
        <groupId>org.smartboot.socket</groupId>
        <artifactId>aio-core</artifactId>
        <version>1.0.0</version>
    </dependency>

## 性能测试报告

| 项目 | 结果 |
| --- | --- |
|CPU| i7-4790 3.60Ghz|
|内存| 8G|
|测试代码|服务端：P2PServer，客户端：P2PMultiClient|
|测试时长|大于两分钟（服务端与客户端启动后的第一分钟数据是无效的，因为实际未跑满一分钟）
|时间单位|1分钟|
|数据总流量|7064MB|
|消息大小|33B|
|消息数|224484842|

## 工程结构
1. aio-core		
smart-socket的核心代码
2. smart-protocol-http		
简单实现Http协议编解码，目前的实现并不规范，仅合适ab测试
3. smart-protocol-p2p	
实现了私有协议P2P，性能测试也是基于该协议进行的

## 近期项目安排（分支：1.0.0-DEV）
1. 实现相对规范的Http协议编解码
2. 继续重构优化aio-core代码

## 开发手册
基于smart-socket进行通信服务的开发，主要有三个步骤：

1. 协议编解码
2. 消息处理
3. 启动服务

接下来我们会通过一个简单例子来演示如何通过smart-socket开发服务端与客户端程序。为简化操作，服务端与客户端交互的数据为一个整型数据。
### 协议编解码
正常情况下服务端与客户端通信共用同一套协议规则，因此我们只需编写一份协议编解码实现即可。如下所示，协议编解码的需要实现接口Protocol。

	public class IntegerProtocol implements Protocol<Integer> {

	    private static final int INT_LENGTH = 4;
	
	    @Override
	    public Integer decode(ByteBuffer data, AioSession<Integer> session) {
	        if (data.remaining() < INT_LENGTH)
	            return null;
	        return data.getInt();
	    }
	
	    @Override
	    public ByteBuffer encode(Integer s, AioSession<Integer> session) {
	        ByteBuffer b = ByteBuffer.allocate(INT_LENGTH);
	        b.putInt(s);
	        return b;
	    }
	}

上述代码很简单，一个整数的长度为4byte，所以只要长度大于等于4，我们就能解析到一个整数。

### 消息处理
业务消息的处理需要实现接口`MessageProcessor`，该接口只有两个方法：`process`,`registerAioSession `。其中 **registerAioSession**仅在建立连接时调用一次，可在该方法中进行会话的初始化操作。**process**则会处理每一个接收到的业务消息。
#### 服务端
	public class IntegerServerProcessor implements MessageProcessor<Integer> {
	    @Override
	    public void process(AioSession<Integer> session, Integer msg) throws Exception {
	        Integer respMsg=msg+1;
	        System.out.println("接受到客户端数据：" + msg + " ,响应数据:" + (respMsg));
	        session.write(respMsg);
	    }
	
	    @Override
	    public void registerAioSession(AioSession<Integer> session) {
	
	    }
	}
	
#### 客户端
	public class IntegerClientProcessor implements MessageProcessor<Integer> {
	    private AioSession session;
	
	    @Override
	    public void process(AioSession<Integer> session, Integer msg) throws Exception {
	        System.out.println("接受到服务端响应数据：" + msg);
	    }
	
	    @Override
	    public void registerAioSession(AioSession<Integer> session) {
	        this.session = session;
	    }
	
	    public AioSession getSession() {
	        return session;
	    }
	}

### 启动服务
#### 服务端
	public class IntegerServer {
	    public static void main(String[] args) {
	        AioQuickServer server = new AioQuickServer()
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
	
#### 客户端
	public class IntegerClient {
	    public static void main(String[] args) throws Exception {
	        IntegerClientProcessor processor=new IntegerClientProcessor();
	        AioQuickClient aioQuickClient=new AioQuickClient()
	                .connect("localhost",8888)
	                .setProtocol(new IntegerProtocol())
	                .setProcessor(processor);
	        aioQuickClient.start();
	        processor.getSession().write(1);
	        Thread.sleep(1000);
	        aioQuickClient.shutdown();
	    }
	}
## 推荐项目
- [NIO版smart-socket](http://git.oschina.net/smartdms/smart-socket)
- [smart-boot](http://git.oschina.net/smartboot/smart-boot)
- [maven-mydalgen-plugin](http://git.oschina.net/smartboot/maven-mydalgen-plugin)

## 关于作者
Edit By Seer  
E-mail:zhengjunweimail@163.com  
QQ:504166636

Update Date: 2017-08-24
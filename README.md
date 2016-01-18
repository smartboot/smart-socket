![输入图片说明](https://static.oschina.net/uploads/img/201508/26160044_Uvf4.png "在这里输入图片标题")
##框架结构
- 传输层  
	smart-quickly:NIO的具体实现，并提供协议解析接口
- 协议层  
	smart-protocol-p2p:以二进制流进行点对点通信的协议，
- 应用层  
	smart-p2p-server:基于smart-protocol-p2p开发的服务端应用  
	smart-p2p-client:基于smart-protocol-p2p开发的客户端应用  

##更新日志
- 2016-01-16 完成代码重构的beta版，性能得到大幅提升

##未来规划
- 消息体支持加密通信
- 提升传输层的安全级别，支持防御类似DDOS之类的网络攻击
- 完善视频教程

>  原先的存储方式将致使大消息体产生的数组对象被直接存储于Tenured Gen，直至触发Full GC才释放内存  

- 待补充...

##快速启动
服务端: P2PServer.java
客户端: P2PMultiClient.java

##Smart-Socket简介
采用NIO实现的通信框架，目前在此基础上提供了P2P协议的交互支持。

特点：  
1. 长连接：由于Socket连接的建立和关闭很耗性能，Smart-Socket的定位为采用了长连接通信;  
2. 自修复：采用长连接方式的一个弊端为可能出现网络断连，因此对于客户端，提供了链路断连自动修复支持;   
3. 自定义消息过滤器：通过实现接口SmartFilter，可以对通信的消息进行自定义监控或预处理；

##P2P协议简介
P2P协议采用TCP协议承载，二进制编码格式，其消息结构分为P2P协议头部和业务消息体两部分。 

1. P2P消息头定义为定长字段，字段定义固定总长度为（32个字节）。包括：
	- MAGIC_NUMBER：幻数(4字节)
	- Length：消息总长度(4个字节)  
	- MessageType：消息类型(4个字节)  
	- SequenceID：由发送方填写，请求和响应消息必须保持一致(4个字节)  
	- 预留16字节
2. P2P消息体定义：  
用于存储特定业务数据的区域，支持的数据类型包括boolean、short、int、String、Object(序列化对象,不建议频繁使用)

> 为什么定义P2P协议？
> 出于两方面考虑：1、效率，P2P专注于业务数据，协议本身除了头部的32字节，没有其余特殊的格式要求。单一的结构使数据更加紧凑而灵活，所有的业务数据都存储于协议的消息体部分，可以保障只传递有价值的数据。从而大幅降低数据传递所需的流量消耗，并且有效的提升的数据的可识别度。2、安全，这是一个一直以来被关注的焦点。采用P2P协议，甚至可以在不依赖于SSL通信的前提下确保数据的安全性。无论何种安全方案都无法有效做到数据的防篡改，因此都为了防窃听而在增加安全级别。而P2P协议的编解码规则完全交由业务消息的提供者控制，第三方很难对其进行破解。

##开发教程
###定义P2P消息
基于P2P协议进行消息通信，定义的消息体需要继承BaseMessage,例如`public class HelloWorldReq extends BaseMessage`。  

继承该抽象类需要实现三个方法:  

- encodeBody：编码
- decodeBody：解码
- getMessageType：定义消息类型

####定义业务字段
消息通信是业务数据的传递，因此需要将包含业务意义的字段定义于消息类中。以`HelloWorldReq`为例，该消息需要发送姓名(String name)、年龄(int age)、性别(boolean male)给服务端：
	
	private String name;
	private int age;
	private boolean male;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isMale() {
		return male;
	}

	public void setMale(boolean male) {
		this.male = male;
	}

####encodeBody
为了能够把本地的数据传递至网络的对端，需要对数据进行编码，转换成P2P协议的消息格式。继承`BaseMessage`之后便可直接使用父类提供的编码方法，根据不同的数据类型选择不同的编码方法。

	protected void encodeBody(ByteBuffer buffer) throws ProtocolException {
		writeString(buffer, name);
		writeInt(buffer, age);
		writeBoolean(buffer, male);
	}
####decodeBody
解码是对P2P数据报文的反向解析，还原其业务含义。解码字段的顺序需要与编码保持一致，否则将破坏原真是数据，甚至导致异常的发生。

	protected void decodeBody(ByteBuffer buffer) throws DecodeException {
		name = readString(buffer);
		age = readInt(buffer);
		male = readBoolen(buffer);
	}

####getMessageType
消息分为请求消息与响应消息两种：`MessageType.REQUEST_MESSAGE = 0x10000000`，`MessageType.RESPONSE_MESSAGE = 0x11000000`

定义消息类型时可从0x01~0x111111中任选一个数值与MessageType进行位运算得到消息类型值。例如`HelloWorldReq`:

	public int getMessageType() {
		return MessageType.REQUEST_MESSAGE|0x01;
	}
假如`HelloWorldResp`为与其对应的响应消息，则`HelloWorldResp`的`getMessageType`必须如下:

	public int getMessageType() {
		return MessageType.RESPONSE_MESSAGE|0x01;
	}

####完整代码
请求消息:

	public class HelloWorldReq extends BaseMessage {
		private String name;
		private int age;
		private boolean male;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}
		public boolean isMale() {
			return male;
		}
		public void setMale(boolean male) {
			this.male = male;
		}
		@Override
		protected void encodeBody(ByteBuffer buffer) throws ProtocolException {
			writeString(buffer, name);
			writeInt(buffer, age);
			writeBoolean(buffer, male);
		}
		@Override
		protected void decodeBody(ByteBuffer buffer) throws DecodeException {
			name = readString(buffer);
			age = readInt(buffer);
			male = readBoolen(buffer);
		}
		@Override
		public int getMessageType() {
			return MessageType.REQUEST_MESSAGE | 0x01;
		}
	}

响应消息:
	
	public class HelloWorldResp extends BaseMessage {
		public HelloWorldResp() {
			super();
		}
	
		public HelloWorldResp(HeadMessage head) {
			super(head);
		}
	
		private String say;
	
		@Override
		protected void encodeBody(ByteBuffer buffer) throws ProtocolException {
			writeString(buffer, say);
		}
	
		@Override
		protected void decodeBody(ByteBuffer buffer) throws DecodeException {
			say = readString(buffer);
		}
	
		public String getSay() {
			return say;
		}
	
		public void setSay(String say) {
			this.say = say;
		}
	
		@Override
		public int getMessageType() {
			return MessageType.RESPONSE_MESSAGE | 0x01;
		}
	}

**注意:1、所有继承BaseMessage的对象必须保留不带参数的默认构造方法；2、各消息的getMessageType值必须保证唯一，P2P实现中已经默认占用了一些数值，使用时应注意避免冲突**

###消息处理器
对于服务器来说，接受到请求消息后需要进行处理，并给予客户端相应的响应信息。处理器的实现需要继承类`AbstractServiceMessageProcessor`,以下的代码为接受客户端的HelloWorldReq并响应HelloWorldResp消息。构造响应消息时，必须将请求消息中的消息头赋值到响应消息对象中，否则客户端无法识别请求/响应消息的关系。

	public class HelloWorldProcessor extends AbstractServiceMessageProcessor {
	
		@Override
		public void processor(Session session, DataEntry message) throws Exception {
			HelloWorldReq request = (HelloWorldReq) message;
			HelloWorldResp resp = new HelloWorldResp(request.getHead());
			resp.setSay(request.getName() + " say: Hello World,I'm "
					+ request.getAge() + " years old. I'm a "
					+ (request.isMale() ? "boy" : "girl"));
			session.sendWithoutResponse(resp);
		}
	}

###服务端开发
服务器的开发主要包括两部分:
1. 注册服务器接受的消息类型，以及各消息对应的处理器
2. 设置服务器的相关配置

以下为简化的服务器实现：

	public class HelloWorldServer {
		public static void main(String[] args) throws ClassNotFoundException {
			// 注册消息以及对应的处理器
			Properties properties = new Properties();
			properties.put(HelloWorldReq.class.getName(), HelloWorldProcessor.class.getName());
			P2pServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
			messageFactory.loadFromProperties(properties);
			// 启动服务
			QuicklyConfig<BaseMessage> config = new QuicklyConfig<BaseMessage>(true);
			config.setProtocolFactory(new P2PProtocolFactory(messageFactory));// 设置协议对象工厂
			config.setProcessor(new P2PServerMessageProcessor(messageFactory));
	
			NioQuickServer<BaseMessage> server = new NioQuickServer<BaseMessage>(config);
			try {
				server.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

运行之后,控制台打印：

	[2016-01-18 15:25:27.474] [main] INFO  regist(P2pServiceMessageFactory.java:83) - load Service Processor Class[com.test.message.HelloWorldProcessor] for com.test.message.HelloWorldReq
	[2016-01-18 15:25:27.475] [main] INFO  regiestMessage(P2pServiceMessageFactory.java:35) - load Message Class[com.test.message.HelloWorldReq]
	[2016-01-18 15:25:27.495] [Nio-Server] INFO  notifyWhenUpdateStatus(NioQuickServer.java:161) - Running with 8888 port


###客户端开发
	
	public class HelloWorldClient {
		public static void main(String[] args) throws Exception {
			Properties properties = new Properties();
			properties.put(HelloWorldResp.class.getName(), "");
			P2pServiceMessageFactory messageFactory =new  P2pServiceMessageFactory();
			messageFactory.loadFromProperties(properties);
			QuicklyConfig<BaseMessage> config = new QuicklyConfig<BaseMessage>(false);
			P2PProtocolFactory factory = new P2PProtocolFactory(messageFactory);
			config.setProtocolFactory(factory);
			P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
			config.setProcessor(processor);
			config.setHost("127.0.0.1");
			config.setTimeout(1000);
			NioQuickClient<BaseMessage> client = new NioQuickClient<BaseMessage>(config);
			client.start();
			int num = 10;
			while (num-- > 0) {
				HelloWorldReq req = new HelloWorldReq();
				req.setName("seer" + num);
				req.setAge(num);
				req.setMale(num % 2 == 0);
				BaseMessage msg = processor.getSession().sendWithResponse(req);
				System.out.println(msg);
			}
			client.shutdown();
		}
	}
	
##推荐项目
- [smart-sosa](https://git.oschina.net/smartdms/smart-sosa)
- [smart-boot](https://git.oschina.net/smartdms/smart-boot)
- [maven-mybatisdalgen-plugin](https://git.oschina.net/smartdms/maven-mybatisdalgen-plugin)

##关于作者
Edit By [Seer](http://zhengjunweimail.blog.163.com/)  
E-mail:zhengjunweimail@163.com  
QQ:504166636

Update Date: 2015-10-20
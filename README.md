#Smart-Socket

##框架结构
- 传输层  
	smart-quickly:NIO的具体实现，并提供协议解析接口
- 协议层  
	1. smart-protocol-http:暂时未完善Http解析  
	2. smart-protocol-p2p:以二进制流进行点对点通信的协议，
- smart-omc 无效工程

##快速启动
服务端: P2PServer.java
客户端: P2PClient.java

##Smart-Socket简介
采用NIO实现的通信框架，目前再次基础上提供了P2P协议的交互支持。

特点：  
1. 长连接：由于Socket连接的建立和关闭很耗性能，Smart-Socket的定位为采用了长连接通信;  
2. 自修复：采用长连接方式的一个弊端为可能出现网络断连，因此对于客户端，提供了链路断连自动修复支持;  
3. 服务端支持集群：各服务端节点都可提供业务服务的同时,充当路由网关进行消息分发。也可通过实现接口ClusterTriggerStrategy自定义消息分发策略；  
4. 自定义负载均衡：服务端开启集群功能后，Smart-Socket目前默认提供了轮循算法RoundRobinLoad用于现在负载均衡，也可通过继承AbstractLoadBalancing自定义负载均衡算法;  
5. 自定义消息过滤器：通过实现接口SmartFilter，可以对通信的消息进行自定义监控或预处理；

##P2P协议简介
P2P协议采用TCP协议承载，二进制编码格式，其消息结构分为P2P协议头部和业务消息体两部分。 

1. P2P消息头定义为定长字段，字段定义固定总长度为（32个字节）。包括：
	- Length：消息总长度(4个字节)  
	- MessageType：消息类型(4个字节)  
	- SequenceID：由发送方填写，请求和响应消息必须保持一致(4个字节)  
	- 预留20字节
2. P2P消息体定义：  
用于存储特定业务数据的区域，支持的数据类型包括boolean、short、int、String、Object(序列化对象,不建议频繁使用)

##关于作者
Edit By [Seer](http://zhengjunweimail.blog.163.com/)  
E-mail:zhengjunweimail@163.com  
QQ:501190241

Update Date: 2015-08-24
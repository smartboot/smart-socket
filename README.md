## 友情提示
如果有朋友用了smart-socket觉得它还不错，并且做出了可开源的案例，烦请告知本人或在评论下留言。因日常工作较忙，争取每个周末保持更新进度，同时希望大家一起支持开源，助力开源的发展，谢谢！！！
## 简介
过去的几年一直在研究NIO技术，虽说没做出什么成绩，但对于Socket技术也是有了一定的了解。从2017年7月份正式开始接触AIO，尽管起步比较晚，但进步还是非常快的。断断续续的用了几周时间写了这个AIO版的通信框架smart-socket，目前的它非常轻量级,核心代码量才500多行，期待它成长后的模样。


## Maven

    <dependency>
        <groupId>org.smartboot.socket</groupId>
        <artifactId>aio-core</artifactId>
        <version>1.0.1</version>
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

[开发手册](http://smartsocket.mydoc.io/)
## 推荐项目
- [NIO版smart-socket](http://git.oschina.net/smartdms/smart-socket)
- [smart-boot](http://git.oschina.net/smartboot/smart-boot)
- [maven-mydalgen-plugin](http://git.oschina.net/smartboot/maven-mydalgen-plugin)

## 关于作者
Edit By Seer  
E-mail:zhengjunweimail@163.com  
QQ:504166636

Update Date: 2017-08-24
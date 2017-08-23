##简介
过去的几年一直在研究NIO技术，虽说没做出什么成绩，但对于Socket技术也是有了一定的了解。从2017年7月份正式开始接触AIO，尽管起步比较晚，但进步还是非常快的。断断续续的用了几周时间写了这个AIO版的通信框架smart-socket，目前的它非常轻量级,核心代码量才700多行，期待它成长后的模样。

##性能测试报告

| 项目 | 结果 |
| --- | --- |
|CPU| i7-4790 3.60Ghz|
|内存| 8G|
|测试代码|服务端：P2PServer，客户端：P2PMultiClient|
|测试时长|大于两分钟（服务端与客户端启动后的第一分钟数据是无效的，因为实际未跑满一分钟）
|时间单位|1分钟|
|数据总流量|6548MB|
|消息大小|33B|
|消息数|208081113|


##开发手册

##推荐项目
- [smartboot-sosa](http://git.oschina.net/smartboot/smartboot-sosa)
- [smart-boot](http://git.oschina.net/smartboot/smart-boot)
- [maven-mydalgen-plugin](http://git.oschina.net/smartboot/maven-mydalgen-plugin)

##关于作者
Edit By Seer  
E-mail:zhengjunweimail@163.com  
QQ:504166636

Update Date: 2017-08-21
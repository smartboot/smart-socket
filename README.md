## smart-socket

[![](https://img.shields.io/badge/lang-Chinese-<green>.svg)](README.md) [![](https://img.shields.io/badge/lang-English-<green>.svg)](./README.en.md)

smart-socket 是一款增强了原生 JDK 实现的 AIO 通信框架。

换言之，smart-socket 100% 遵循 JDK 对于 AIO 接口规范的定义，只是重新提供了一套代码实现。
使其有着相较 JDK 官方 AIO 更高的通信性能，更少的资源开销，以及更稳定的运行保障。

- **[文档地址](https://smartboot.tech/smart-socket/)**
- **[我们的用户](https://smartboot.tech/smart-socket/users.html)**

### 为什么开发 smart-socket?
- AIO 是一个面向开发人员更友好的设计理念，值得被更多人应用。
- 原生 JDK 提供的实现存在性能问题，其线程模型限制了 IO 调度效率。
- 原生 JDK 提供的实现存在资源开销问题，连接越多内存需求越高，难以在低规格服务器中支撑百万级长连接。
- 原生 JDK 提供的实现存在稳定性问题，Mac 系统下进行压测存在不明原因的死机现象。
- 我们需要一款比 Netty 更容易上手的通信框架。




### 🍁项目特色
1. 高性能、高并发、低延迟、绿色节能。
2. 代码量极少，可读性强。核心代码不到 1500 行，工程结构、包层次清晰。
3. 学习门槛低，二次开发只需实现 2 个接口（Protocol、MessageProcessor）,具备通信开发经验的几乎无学习成本。
4. 良好的线程模型、内存模型设计，保障服务高效稳定的运行。
5. 支持自定义插件，并已提供了丰富的插件，包括：SSL/TLS通信插件、心跳插件、断链重连插件、服务指标统计插件、黑名单插件、内存池监测插件。

### 🍒生态项目
- [smart-http](https://gitee.com/smartboot/smart-http)
- [smart-servlet](https://gitee.com/smartboot/smart-servlet)
- [smart-mqtt](https://gitee.com/smartboot/smart-mqtt)

### 🍭推荐
- 《[smart-socket 单机百万长连接实战教程](https://mp.weixin.qq.com/s/l_IBSBI6SAY4FmomwLFa-Q)》
- 《[新手小白必读：通信协议](https://mp.weixin.qq.com/s/2w9C8CQvhOXZsLEOd6Gzww)》
- 《[图解通信框架的调度模型](https://mp.weixin.qq.com/s/Hq4T-X7LtjIOVi1aEEvxKQ)》
- 《[通信框架 smart-socket 设计概览](https://mp.weixin.qq.com/s/M9sMfDKahgsR8LgX0M4CVQ)》

### 🎃性能排行
![输入图片说明](image.png)

### 🎈插件清单
| Plugin | 用途 |
|---|---|
|BlackListPlugin|黑名单插件,smart-socket会拒绝与黑名单中的IP建立连接|
|BufferPageMonitorPlugin|内存池监控插件|
|HeartPlugin|心跳插件|
|MonitorPlugin|服务指标监控插件|
|SocketOptionPlugin|连接属性配置插件|
|SslPlugin|TLS/SSL加密通讯插件|
|StreamMonitorPlugin|传输层通讯码流监控插件|

### 🍩感谢
- 感谢 Gitee 提供的代码托管和曾经的 Pages 服务。
- 感谢 Github 提供的代码托管和现在的 Pages 服务。
- 感谢 JetBrains 为 smart-socket 提供的 IDEA License。     
    <a href="https://www.jetbrains.com/?from=smart-socket"><img src="jetbrains.png" width="20%" height="20%"/></a>

### 🥳加入社群

- **官方QQ群：** 172299083 、830015805。入群条件：
  1. 普通用户：[捐赠5元](https://smartboot.tech/donation.html)，并备注您的QQ号，我们将人工审核入群人员。
  2. 企业用户：完成【[案例登记](https://gitee.com/smartboot/smart-socket/issues/IHV69)】并在加群时备注企业名。
- 扫描底部二维码关注三刀公众号

<img src="wx.jpg" width="50%" height="50%"/>

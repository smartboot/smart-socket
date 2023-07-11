## smart-socket [![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu) 

[![](https://img.shields.io/badge/lang-Chinese-<green>.svg)](README.md) [![](https://img.shields.io/badge/lang-English-<green>.svg)](./README.en.md)

smart-socket 是一款100%自研的国产开源通信框架，通过强化 AIO 的实现使其有着超越各大语言的通信性能和稳定性。

凭借自身极简、易用、高性能的特性，smart-socket 得到了诸多 [开发人员和企业](https://smartboot.gitee.io/smart-socket/users.html) 的认可和青睐。

**[文档地址](https://smartboot.gitee.io/smart-socket/)**

### 🍁项目特色
1. 高性能、高并发、低延迟、绿色节能。
2. 代码量极少，可读性强。核心代码不到 1500 行，工程结构、包层次清晰。
3. 学习门槛低，二次开发只需实现 2 个接口（Protocol、MessageProcessor）,具备通信开发经验的几乎无学习成本。
4. 良好的线程模型、内存模型设计，保障服务高效稳定的运行。
5. 支持自定义插件，并已提供了丰富的插件，包括：SSL/TLS通信插件、心跳插件、断链重连插件、服务指标统计插件、黑名单插件、内存池监测插件。

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
- 感谢码云提供的代码托管和 Pages 服务。
- 感谢 JetBrains 为 smart-socket 提供的 IDEA License。     
    <a href="https://www.jetbrains.com/?from=smart-socket"><img src="jetbrains.png" width="20%" height="20%"/></a>

### 🥳加入社群

- **官方QQ群：** 172299083 、830015805。入群条件：
  1. 普通用户：[捐赠5元](https://smartboot.tech/donation.html)，并备注您的QQ号，我们将人工审核入群人员。
  2. 企业用户：完成【[案例登记](https://gitee.com/smartboot/smart-socket/issues/IHV69)】并在加群时备注企业名。
- 扫描底部二维码关注三刀公众号

​	<img src="wx.jpg" width="50%" height="50%"/>

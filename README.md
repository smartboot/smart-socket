# smart-socket

<p align="center">
  <b>A lightweight Java AIO framework for high-performance TCP applications</b><br/>
  Simple API · Low memory footprint · Built for long-lived connections & custom protocols
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/smartboot/smart-socket" alt="License"/></a>
  <a href="https://central.sonatype.com/artifact/io.github.smartboot.socket/aio-core"><img src="https://img.shields.io/maven-central/v/io.github.smartboot.socket/aio-core" alt="Maven Central"/></a>
  <a href="https://github.com/smartboot/smart-socket/stargazers"><img src="https://img.shields.io/github/stars/smartboot/smart-socket" alt="GitHub Stars"/></a>
  <a href="https://github.com/smartboot/smart-socket"><img src="https://img.shields.io/github/forks/smartboot/smart-socket" alt="Forks"/></a>
</p>

<p align="center">
  <a href="README.md">English</a> | <a href="README_zh.md">中文</a>
</p>

---

## Why smart-socket?

Java’s native AIO (NIO.2) provides a clean asynchronous programming model, but in real-world networking workloads it often falls short:

- Threading and I/O scheduling limits under high connection counts
- Memory cost that scales poorly with long-lived connections
- Stability issues observed under stress on some platforms

**smart-socket** keeps the developer-friendly AIO style while reimplementing the underlying engine for better efficiency and reliability:

- Higher throughput and lower resource usage for massive long-lived TCP connections
- A very small, readable core that is easy to understand and audit
- A minimal API surface — you only need to implement **two interfaces**

> smart-socket is **not** a drop-in replacement for Netty.  
> It is a focused, lightweight alternative when you want less complexity and tighter control over the networking layer.

---

## Key Features

| Feature | Description |
|--------|-------------|
| **AIO-style API** | Familiar asynchronous model with `AioQuickServer`, `AioQuickClient`, and `AioSession` |
| **Minimal learning curve** | Implement only `Protocol<T>` + `MessageProcessor<T>` |
| **Tiny core** | Small codebase, clear package structure, minimal dependencies |
| **Efficient memory** | Buffer pooling designed for high connection counts and long-running services |
| **Plugin system** | SSL/TLS, idle detection, metrics, rate limiting, traffic monitoring, and more |
| **TCP + UDP** | Full TCP support; UDP available via extension modules |
| **Production-oriented** | Explicit connection lifecycle events and a clear state machine |

---

## Requirements

- **JDK 8+**
- Maven or Gradle

---

## Quick Start

### Maven dependency

```xml
<dependency>
    <groupId>io.github.smartboot.socket</groupId>
    <artifactId>aio-pro</artifactId>
    <version>2.1.3</version>
</dependency>
```

> Prefer a smaller footprint? Use `aio-core` instead of `aio-pro` when you do not need the extension plugins.

### 1. Define a protocol

Length-prefixed string messages (4-byte length + payload):

```java
import io.github.smartboot.socket.Protocol;
import io.github.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringProtocol implements Protocol<String> {
    @Override
    public String decode(ByteBuffer buffer, AioSession session) {
        if (buffer.remaining() < Integer.BYTES) {
            return null; // need more data
        }
        buffer.mark();
        int length = buffer.getInt();
        if (length > buffer.remaining()) {
            buffer.reset(); // half-packet
            return null;
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
```

### 2. Implement a message processor

```java
import io.github.smartboot.socket.MessageProcessor;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EchoProcessor implements MessageProcessor<String> {
    @Override
    public void process(AioSession session, String msg) {
        System.out.println("Received: " + msg);
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        WriteBuffer wb = session.writeBuffer();
        try {
            wb.writeInt(body.length);
            wb.write(body);
            wb.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 3. Start a server

```java
import io.github.smartboot.socket.transport.AioQuickServer;

public class EchoServer {
    public static void main(String[] args) throws Exception {
        AioQuickServer server = new AioQuickServer(8888, new StringProtocol(), new EchoProcessor());
        server.start();
        System.out.println("Echo server started on port 8888");
    }
}
```

### 4. Connect with a client

```java
import io.github.smartboot.socket.MessageProcessor;
import io.github.smartboot.socket.transport.AioQuickClient;
import io.github.smartboot.socket.transport.AioSession;
import io.github.smartboot.socket.transport.WriteBuffer;

import java.nio.charset.StandardCharsets;

public class EchoClient {
    public static void main(String[] args) throws Exception {
        MessageProcessor<String> processor = (session, msg) ->
                System.out.println("Response: " + msg);

        AioQuickClient client = new AioQuickClient("localhost", 8888, new StringProtocol(), processor);
        AioSession session = client.start();

        byte[] body = "hello smart-socket".getBytes(StandardCharsets.UTF_8);
        WriteBuffer wb = session.writeBuffer();
        wb.writeInt(body.length);
        wb.write(body);
        wb.flush();
    }
}
```

That’s it — a working async TCP echo service with a custom protocol.

---

## Core Concepts

| Component | Responsibility |
|-----------|----------------|
| **`Protocol<T>`** | Decode bytes from the read buffer into a message of type `T`. Return `null` when the frame is incomplete. |
| **`MessageProcessor<T>`** | Handle decoded messages and connection lifecycle events (`stateEvent`). |
| **`AioQuickServer`** | Bootstrap and run a TCP server. |
| **`AioQuickClient`** | Bootstrap and run a TCP client. |
| **`AioSession`** | Per-connection abstraction. Use `writeBuffer()` to send data. |
| **`WriteBuffer`** | Buffered outbound API (`writeInt`, `write`, `flush`, …). |

### Connection state events

`MessageProcessor.stateEvent(...)` is invoked for important lifecycle points, including:

- `NEW_SESSION` — connection established
- `INPUT_SHUTDOWN` — read side closed
- `SESSION_CLOSING` / `SESSION_CLOSED`
- `DECODE_EXCEPTION` / `PROCESS_EXCEPTION`
- `INPUT_EXCEPTION` / `OUTPUT_EXCEPTION`

Prefer extending `AbstractMessageProcessor` when you need plugins; it wires the plugin chain for you.

---

## Architecture

```text
Your Application
       │
       ▼
┌──────────────────────────────────────┐
│  Protocol<T>     MessageProcessor<T> │  ← your code
│         AioSession                   │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│  Buffer pools  ·  Plugin pipeline    │
│  Enhanced async I/O runtime          │
└──────────────────────────────────────┘
       │
       ▼
        TCP / UDP
```

smart-socket separates:

1. **Network I/O** (framework)
2. **Framing / decoding** (`Protocol`)
3. **Business logic** (`MessageProcessor`)

The runtime uses an enhanced asynchronous channel implementation aimed at better scheduling and memory behavior than stock JDK AIO for typical server workloads.

---

## When to use / When not to use

### Good fit

- Custom binary or text protocols
- Long-lived TCP connections (IM, IoT gateways, device management, internal RPC)
- Teams that want a small, understandable networking core
- Resource-sensitive deployments where connection count and memory matter

### Probably not the best fit

- You need a batteries-included HTTP / WebSocket stack → use Netty or a web framework
- You depend heavily on Netty’s broad ecosystem of codecs and handlers
- You want a full application framework rather than a focused transport layer

---

## Plugins

Plugins hook into accept, read/write, pre-process, and state events without modifying the core.

| Plugin | Purpose |
|--------|---------|
| **SslPlugin** | TLS/SSL encryption |
| **IdleStatePlugin** | Idle / heartbeat handling |
| **MonitorPlugin** | Service metrics |
| **RateLimiterPlugin** | Traffic limiting |
| **StreamMonitorPlugin** | Transport-level stream monitoring |
| **BufferPageMonitorPlugin** | Buffer pool monitoring |
| **ProxyProtocolPlugin** | PROXY protocol support |

Register plugins via `AbstractMessageProcessor.addPlugin(...)`.

---

## Ecosystem

smart-socket is the networking foundation for several projects in the smartboot ecosystem:

| Project | Description |
|---------|-------------|
| **[feat](https://github.com/smartboot/feat)** | Lightweight high-performance Java web / application framework |
| **[smart-mqtt](https://github.com/smartboot/smart-mqtt)** | Cloud-native MQTT broker for large-scale IoT device connectivity |
| **[Redisun](https://github.com/smartboot/redisun)** | Lightweight Redis client built on smart-socket |
| **[smart-servlet](https://github.com/smartboot/smart-servlet)** | Servlet container powered by the same networking core |

---

## Documentation & Examples

- Documentation: [https://smartboot.tech/smart-socket](https://smartboot.tech/smart-socket)
- Examples: [example/](example/)
- Benchmarks: [benchmark/](benchmark/)

---

## Roadmap

- [x] Java AIO communication core
- [x] TCP server & client
- [x] Protocol-based programming model
- [x] Plugin architecture
- [x] SSL/TLS support
- [x] Monitoring & metrics plugins
- [ ] More end-to-end examples
- [ ] Broader ecosystem integrations
- [ ] Expanded international documentation

---

## Contributing

Contributions are welcome.

- Report bugs and request features via [GitHub Issues](https://github.com/smartboot/smart-socket/issues)
- Improve documentation and examples
- Submit pull requests

Please keep changes focused and include a clear description of the problem and solution.

---

## License

smart-socket is released under the [Apache License 2.0](LICENSE).

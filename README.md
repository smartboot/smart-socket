
# smart-socket

<p align="center">

<b>A lightweight, high-performance Java networking framework for building reliable and scalable network applications.</b>

</p>

<p align="center">

[![License](https://img.shields.io/github/license/smartboot/smart-socket)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/smartboot/smart-socket)](https://github.com/smartboot/smart-socket)
[![GitHub Issues](https://img.shields.io/github/issues/smartboot/smart-socket)](https://github.com/smartboot/smart-socket/issues)

</p>


---

## Introduction

smart-socket is a lightweight Java networking framework based on the JDK asynchronous I/O model.

It provides a simple, efficient, and extensible communication layer for building high-performance applications, such as:

- Custom TCP servers
- IoT platforms
- Message systems
- RPC frameworks
- Real-time applications
- High-concurrency backend services


smart-socket focuses on:

- Simple programming model
- Low memory consumption
- High performance
- Easy customization
- Stable long-running connections


Unlike many heavyweight networking frameworks, smart-socket keeps the core architecture small and understandable, allowing developers to focus on business protocols instead of complex framework internals.


---

# Why smart-socket?


Building high-concurrency network applications usually requires dealing with:

- Connection management
- Asynchronous I/O
- Buffer management
- Protocol parsing
- Thread scheduling


Existing solutions often provide powerful features but may introduce additional complexity.

smart-socket provides a lightweight alternative:

> A small, efficient, and extensible networking foundation for Java developers who need control and simplicity.


---

# Features


## 🚀 High Performance

smart-socket is designed for high-concurrency communication scenarios.

Key features:

- Asynchronous network I/O
- Efficient connection processing
- Optimized memory usage
- Stable long-term connections


It is suitable for applications requiring thousands or millions of concurrent connections.


---

## 🪶 Lightweight Architecture


smart-socket keeps the core implementation simple and compact.

Benefits:

- Easy to understand
- Easy to debug
- Easy to extend
- Low dependency footprint


Developers can quickly understand the internal architecture and customize it for their own requirements.


---

## 💾 Low Memory Consumption


Long-lived connections require efficient resource management.

smart-socket provides:

- Optimized buffer management
- Reduced unnecessary object allocation
- Efficient connection lifecycle handling


This makes it suitable for:

- IoT devices
- Edge computing
- Embedded systems
- Large-scale connection services


---

## 🔌 Simple Programming Model


Building a custom protocol requires only a few core components.


The main abstractions are:

```

Protocol

MessageProcessor

````


Example:


```java
public class EchoProcessor implements MessageProcessor {

    @Override
    public void process(
            AioSession session,
            Object message) {

        session.write(message);
    }
}
````

Developers can focus on protocol and business logic without dealing with low-level network details.

---

## 🧩 Extensible Plugin System

smart-socket provides a flexible plugin architecture.

Built-in plugins include:

| Plugin                | Description                     |
| --------------------- | ------------------------------- |
| SSL Plugin            | TLS/SSL encrypted communication |
| Heartbeat Plugin      | Connection health detection     |
| Monitor Plugin        | Runtime metrics collection      |
| Blacklist Plugin      | IP access control               |
| Socket Option Plugin  | Socket configuration            |
| Stream Monitor Plugin | Network traffic monitoring      |

Developers can create custom plugins to extend the framework.

---

# Architecture

```
+--------------------------------+
|          Application           |
+--------------------------------+

              |

+--------------------------------+
|      MessageProcessor          |
+--------------------------------+

              |

+--------------------------------+
|          Protocol              |
+--------------------------------+

              |

+--------------------------------+
|      smart-socket Core         |
+--------------------------------+

              |

+--------------------------------+
| JDK AsynchronousSocketChannel  |
+--------------------------------+

              |

+--------------------------------+
|        Operating System        |
+--------------------------------+
```

smart-socket separates:

* Network communication
* Protocol processing
* Business logic

This architecture makes applications easier to maintain and extend.

---

# Quick Start

## Maven Dependency

```xml
<dependency>
    <groupId>io.github.smartboot.socket</groupId>
    <artifactId>aio-core</artifactId>
    <version>${latest.version}</version>
</dependency>
```

---

## Create a Server

Example:

```java
public class EchoServer {

    public static void main(String[] args) {

        AioQuickServer server =
                new AioQuickServer(8080);

        server.start();
    }
}
```

A simple asynchronous server can be started with only a few lines of code.

---

# Use Cases

## Custom TCP Protocol

smart-socket is suitable for:

* Industrial communication protocols
* Private network protocols
* Financial systems
* Real-time communication

---

## IoT Applications

For large-scale device communication:

* Persistent connections
* Low memory usage
* Efficient message processing

smart-socket provides a reliable communication foundation.

---

## Messaging Systems

Use smart-socket to build:

* Message brokers
* Gateway services
* Distributed communication components

---

## Database and Client Components

smart-socket can also serve as the networking layer for:

* Database clients
* Cache clients
* Service connectors

---

# Ecosystem

smart-socket is the communication foundation behind several high-performance Java projects in the smartboot ecosystem.

```
                    smart-socket

                         |

        +----------------+----------------+

        |                |                |

       Feat          smart-mqtt       Redisun

        |                |                |

  Web Framework     IoT Broker     Redis Client
```

---

## Feat

A lightweight Java application framework for modern backend development.

Feat provides:

* High-performance web services
* Cloud-native application development
* Simple programming model
* Lightweight runtime

smart-socket provides the underlying network communication capability for Feat.

Repository:

[https://github.com/smartboot/feat](https://github.com/smartboot/feat)

---

## smart-mqtt

A lightweight MQTT broker designed for large-scale IoT communication.

Features:

* High-concurrency device connections
* Efficient message delivery
* Lightweight deployment
* Reliable communication

smart-socket provides the high-performance networking layer for smart-mqtt.

Repository:

[https://github.com/smartboot/smart-mqtt](https://github.com/smartboot/smart-mqtt)

---

## Redisun

A lightweight Redis client component for Java, built on top of smart-socket.

Redisun focuses on:

* High-performance Redis communication
* Efficient connection management
* Lightweight architecture
* Easy integration

By using smart-socket as the network engine, Redisun provides a controllable and efficient Redis client implementation.

Repository:

[https://github.com/smartboot/redisun](https://github.com/smartboot/redisun)

---

# smart-socket vs Traditional Networking Frameworks

smart-socket is designed for developers who prefer a lightweight and understandable networking foundation.

|                 | smart-socket | Traditional Frameworks |
| --------------- | ------------ | ---------------------- |
| Architecture    | Lightweight  | Feature-rich           |
| Learning curve  | Low          | Medium / High          |
| Custom protocol | Simple       | Depends                |
| Resource usage  | Low          | Depends                |
| Customization   | Easy         | Depends                |

Different scenarios require different tools.

smart-socket focuses on simplicity, performance, and control.

---

# Performance

smart-socket focuses on:

* High throughput
* Stable latency
* Efficient memory usage
* Long-running connection stability

Benchmark results:

Coming soon.

---

# Documentation

Documentation:

[https://smartboot.tech/](https://smartboot.tech/)

Examples:

[https://github.com/smartboot/smart-socket/tree/master/smart-socket-example](https://github.com/smartboot/smart-socket/tree/master/smart-socket-example)

---

# Roadmap

* [x] High-performance AIO core
* [x] Plugin architecture
* [x] SSL/TLS support
* [x] Connection monitoring
* [x] Memory optimization
* [x] Production usage in smartboot ecosystem
* [ ] More protocol examples
* [ ] More international documentation
* [ ] More ecosystem integrations

---

# Community

If you are building:

* IoT platforms
* Message systems
* RPC frameworks
* Database clients
* Real-time services
* Custom network protocols

smart-socket can provide a lightweight and reliable communication foundation.

GitHub:

[https://github.com/smartboot/smart-socket](https://github.com/smartboot/smart-socket)

---

# Contributing

Contributions are welcome!

You can help by:

* Reporting issues
* Improving documentation
* Creating examples
* Submitting pull requests

---

# License

smart-socket is licensed under the Apache License 2.0.

---

# Thanks

Thanks to:

* GitHub
* Gitee
* JetBrains

for supporting open-source development.


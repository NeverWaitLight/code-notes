# 网络编程技术演示模块

本模块演示了 Java 网络编程的多种技术实现，包括 BIO、NIO 以及虚拟线程(Loom)的应用。

## 包含的技术演示

1. **BIO 实现**

   - BioServer.java: 传统的阻塞 IO 服务器实现
   - 特点：每个连接需要一个独立线程处理

2. **NIO 实现**

   - NioEchoServer.java: 基于 Selector 的多路复用 Echo 服务器
   - NioDemo.java/NioDemo2.java: NIO 的不同实现方式
   - 特点：单线程处理多连接，非阻塞 IO

3. **虚拟线程(Loom)**
   - LoomThreadMain.java: 展示虚拟线程的使用
   - 在 NioEchoServer 中用于处理读操作

## 技术要点

- 使用 Java NIO 的 Selector 模式
- 采用非阻塞 IO 提高并发能力
- 结合虚拟线程处理 IO 操作
- 演示了基本的网络通信模式

## 运行示例

1. 启动 NIO Echo 服务器：

```bash
java org.waitlight.codememo.proxy.NioEchoServer
```

2. 使用 telnet 或 nc 测试：

```bash
telnet localhost 8080
```

## 适用场景

- 学习 Java 网络编程
- 理解 BIO/NIO 区别
- 实践虚拟线程应用
- 开发高性能网络服务基础

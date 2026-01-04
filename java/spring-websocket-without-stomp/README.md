# Spring WebSocket 示例 (不使用 STOMP)

该项目演示了如何在 Spring 框架中使用底层 WebSocket API 而不依赖 STOMP 协议。

## 项目特点

- 基于 Spring Boot 2.6.3
- 使用`TextWebSocketHandler`实现 WebSocket 消息处理
- 简单的 JSON 消息交互示例
- 不使用 STOMP 协议

## 核心组件

1. `SocketHandler` - 继承`TextWebSocketHandler`处理 WebSocket 消息

   - 处理文本消息
   - 管理 WebSocket 会话
   - 使用 Gson 进行 JSON 解析

2. `StompWebSocketConfig` - WebSocket 配置类
   - 注册 WebSocket 处理器
   - 映射 WebSocket 端点("/name")

## 快速开始

1. 启动应用
2. 使用 WebSocket 客户端连接: `ws://localhost:8080/name`
3. 发送 JSON 消息: `{"name": "your_name"}`
4. 将收到响应: `Hello your_name !`

## 技术栈

- Spring WebSocket
- Java 8
- Gson (JSON 处理)

## 参考资源

- [Spring WebSocket 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [WebSocket 协议规范](https://tools.ietf.org/html/rfc6455)
- [Gson 文档](https://github.com/google/gson/blob/master/UserGuide.md)

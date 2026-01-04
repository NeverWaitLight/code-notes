# 代码备忘录项目 (code-memo)

这是一个用于存储各种技术演示和代码片段的存储库，包含 Java、JavaScript 和 Shell 脚本等多种技术实现。

## 项目结构概述

### Java 项目

- **algorithm**：计算几何算法实现（点与图形、多边形处理、射线投射算法）
- **bridgeway**：Windows 服务代理工具（安装/卸载本地软件代理）
- **flink-demo**：Apache Flink 流处理框架示例（支持 Java 8 的 1.14.4 版本）
- **proxy-demo**：Java 网络编程技术演示（BIO/NIO/虚拟线程）
- **seata-demo**：分布式事务框架 Seata 示例（XA/AT/TCC/SAGA 模式）
- **simple-demo**：基础 Java 技术集成演示（日志、网络、基准测试等）
- **spring-websocket-without-stomp**：底层 WebSocket API 实现（不使用 STOMP 协议）
- **webflux-demo**：Spring WebFlux 响应式编程与 MongoDB 集成

### JavaScript 项目

- **tracking.js**：基于浏览器的面部识别与拍照功能实现

### Shell 脚本

- **Git 操作**：分支管理、版本发布、标签更新等自动化脚本
- **Java 应用管理**：服务启动/监控脚本（内存管理、CPU 监控）
- **部署工具**：简易 CI/CD 部署脚本

---

## 详细项目说明

### Java 项目

#### algorithm

包含计算几何相关的算法实现：

- 点与图形基础类（Point.java, Shape.java）
- 多边形处理（面积/周长计算）
- 射线投射算法（判断点是否在多边形内部）

#### bridgeway

安装到 Windows 并注册为服务，代理本地软件请求到指定后端：

- 安装：管理员运行 install.sh
- 卸载：管理员运行 uninstall.sh

#### flink-demo

Apache Flink 流处理示例：

- 使用支持 Java 8 的 1.14.4 版本
- 测试方法：
  1. 运行数据源：`nc -lk 9000`(Linux/macOS)或`ncat -lk 9000`(Windows)
  2. 启动 flink-demo 观察控制台输出

#### proxy-demo

网络编程技术演示：

- BIO 实现：传统阻塞 IO 服务器
- NIO 实现：基于 Selector 的多路复用 Echo 服务器
- 虚拟线程(Loom)：轻量级线程处理 IO 操作

#### seata-demo

分布式事务框架 Seata 示例：

- 支持 XA、AT、TCC 和 SAGA 四种模式
- 技术栈：
  - Spring Boot 2.6.3
  - Spring Cloud 2021.0.1
  - Nacos v2.1.0
  - Seata Server 1.5.1
- 使用步骤：
  1. 启动 Nacos 服务
  2. 启动 Seata Server
  3. 创建数据库表
  4. 按顺序启动各微服务

#### simple-demo

基础 Java 技术集成：

- 主要依赖：
  - Log4j 2.17.0
  - Netty 4.1.109.Final
  - JMH 1.37
  - CGLIB 3.3.0
- 实用工具包：
  - 图像处理
  - Excel 操作
  - 二维码生成
  - 压缩文件处理
  - 布隆过滤器

#### spring-websocket-without-stomp

底层 WebSocket API 实现：

- 核心组件：
  - SocketHandler：处理 WebSocket 消息
  - StompWebSocketConfig：WebSocket 配置类
- 使用流程：
  1. 连接 `ws://localhost:8080/name`
  2. 发送 JSON 消息：`{"name": "your_name"}`
  3. 接收响应：`Hello your_name !`

#### webflux-demo

Spring WebFlux 响应式编程：

- 技术栈：
  - Spring Boot 3.1.5
  - Java 21
  - MongoDB Reactive
- REST API 功能：
  - 用户管理（创建/查询/更新/删除）
  - 全异步非阻塞处理
- 运行说明：
  1. 启动 MongoDB：`docker run -d -p 27017:27017 --name mongodb mongo`
  2. 启动应用：`mvn spring-boot:run`

---

### JavaScript 项目

#### tracking.js

- **face-camera.html**：人脸识别并拍照
- **face-camera-II.html**：竖屏裁剪摄像头画面进行人脸识别

---

### Shell 脚本

#### Git 操作

- **release.sh**：分支合并与版本发布
- **delete_branch.sh**：清理 feat/fix 开发分支
- **mono_release.sh**：单仓库版本发布

#### Java 应用管理

- **start.sh**：服务启动/重启（指定 JVM 内存）
- **startw.sh**：带监控的服务启动（CPU 超 90%自动 jstack）
- **deploy.sh**：简易打包部署脚本

#### 参数工具

- getopts：命令行参数解析
- argbash：参数解析框架

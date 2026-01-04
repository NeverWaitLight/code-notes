# Simple Demo 项目

这是一个基于 Maven 构建的 Java 演示项目，主要用于展示各种 Java 技术的集成使用。

## 项目信息

- **Java 版本**: 21
- **构建工具**: Maven

## 主要依赖

- Log4j 2.17.0 - 日志记录
- Netty 4.1.109.Final - 高性能网络框架
- JMH 1.37 - Java 微基准测试工具
- CGLIB 3.3.0 - 代码生成库

## 项目结构

```
simple-demo/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/     # Java源代码
│   │   └── resources/ # 资源文件
└── target/           # 构建输出目录
```

## 新增功能模块

- **Utils 工具包**：提供各种实用工具类
  - 图像处理(ImageUtil)
  - Excel 操作(ExcelUtil, ExcelWriter)
  - 二维码生成(QRCode)
  - 压缩文件处理(ZipUtil)
  - 布隆过滤器(SimpleBloomFilter)

## 用途

该项目可用于演示或测试各种 Java 技术的集成使用方式，包含网络、日志、基准测试、代码生成以及实用工具等功能，可作为学习这些技术的参考实现。

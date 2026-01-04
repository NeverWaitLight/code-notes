# Flink 示例项目

参考文档：

- https://zhuanlan.zhihu.com/p/80221477

## [创建项目](https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/configuration/overview/)

Flink 已弃用 Java 8 支持，将在未来版本中移除。建议所有用户迁移到 Java 11。

当前最新版本为 1.15.1，但该版本已移除对 Java 8 的支持。因此我们使用支持 Java 8 的 1.14.4 版本。

```bash
mvn archetype:generate                \
 -DarchetypeGroupId=org.apache.flink   \
 -DarchetypeArtifactId=flink-quickstart-java \
 -DarchetypeVersion=1.14.4 \
 -DgroupId=xyz.demo \
 -DartifactId=flink-client \
 -Dversion=0.0.1-SNAPSHOT \
 -Dpackage=xyz.demo.flink \
 -DinteractiveMode=false
```

## 测试方法

### 1. 运行数据源

模拟一个服务器

#### Linux/macOS

```shell
nc -lk 9000
```

#### Windows

下载安装 `ncat`: https://nmap.org/ncat/

```shell
ncat -lk 9000
```

### 2. 运行 flink-demo

启动 `flink-demo` 后，在数据源终端模拟客户端输入，观察 `flink-demo` 控制台输出

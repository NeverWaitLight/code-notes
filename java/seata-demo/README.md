# Seata 分布式事务演示项目

本项目演示了Seata支持的多种分布式事务模式，包括XA、AT、TCC和SAGA模式。

## 项目结构

- **seata-commons**: 公共代码模块，包含基础配置和工具类
- **seata-xa**: XA模式演示
- **seata-at**: AT模式演示
- **seata-tcc**: TCC模式演示  
- **seata-saga**: SAGA模式演示

## 组件版本

| 组件                 | Version    |
| -------------------- | ---------- |
| Spring-Boot          | 2.6.3      |
| Spring-Cloud         | 2021.0.1   |
| Spring-Cloud-Alibaba | 2021.0.1.0 |
| Nacos-Server         | v2.1.0     |
| Seata-Server         | 1.5.1      |

## 环境准备

1. 启动Nacos服务
2. 启动Seata Server
3. 创建必要的数据库表

## 使用指南

1. 按顺序启动各服务：
   - 账户服务
   - 库存服务  
   - 订单服务
   - 业务服务

2. 测试事务：
```bash
curl -X POST http://localhost:8080/purchase
```

## 子模块文档

- [XA模式](seata-xa/README.md)
- [AT模式](seata-at/README.md) 
- [TCC模式](seata-tcc/README.md)
- [SAGA模式](seata-saga/README.md)

> 参考文档：[Seata高可用部署方案](https://seata.io/zh-cn/blog/seata-ha-practice.html)

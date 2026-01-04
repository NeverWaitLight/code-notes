# Seata 公共模块

本模块包含Seata分布式事务演示项目的公共代码和基础配置。

## 功能说明

- 提供统一的依赖管理
- 包含基础配置类
- 封装常用工具类
- 定义公共DTO和异常处理

## 包含内容

1. **基础配置**
   - Seata配置
   - 数据源配置
   - MyBatis配置

2. **工具类**
   - 响应封装
   - 异常处理
   - 日志工具

3. **公共DTO**
   - 统一返回结果
   - 错误码定义

## 使用方式

其他子模块通过Maven依赖引入本模块：

```xml
<dependency>
    <groupId>org.waitlight.codememo</groupId>
    <artifactId>seata-commons</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 注意事项

- 本模块不包含业务逻辑
- 只提供基础支持和公共组件
- 修改需考虑向后兼容性

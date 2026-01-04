# Spring WebFlux 与 MongoDB Reactive 集成示例

该项目演示了如何使用 Spring WebFlux 和 MongoDB Reactive 构建响应式 RESTful API。

## 技术栈

- **Spring Boot**: 3.1.5
- **Java**: 21
- **Spring WebFlux**: 响应式 Web 框架
- **Spring Data MongoDB Reactive**: 响应式 MongoDB 集成
- **Project Reactor**: 响应式编程库
- **MongoDB**: 文档数据库

## 核心功能

1. 用户管理 REST API：
   - 创建用户
   - 查询用户列表
   - 根据 ID 查询用户
   - 更新用户信息
   - 删除用户
2. 响应式数据处理：
   - 全异步非阻塞处理
   - 流式数据处理支持

## 项目结构

```
webflux-demo/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── org/waitlight/codememo/webflluxdemo/
│       │       ├── WebfluxDemoApplication.java   # 应用入口
│       │       ├── User.java                     # 用户实体
│       │       ├── UserRepository.java           # MongoDB 响应式存储库
│       │       ├── UserService.java              # 用户服务
│       │       └── UserController.java           # REST控制器
│       └── resources/
│           └── application.yml                  # 应用配置
└── README.md
```

## API 接口

| 方法   | 路径                       | 描述             | 请求体                                                      |
| ------ | -------------------------- | ---------------- | ----------------------------------------------------------- |
| GET    | /users                     | 获取所有用户     | -                                                           |
| GET    | /users/{id}                | 根据 ID 获取用户 | -                                                           |
| GET    | /users/by-name?name={name} | 根据名称查询用户 | -                                                           |
| POST   | /users                     | 创建新用户       | JSON: {"name": "John", "email": "john@example.com"}         |
| PUT    | /users/{id}                | 更新用户信息     | JSON: {"name": "John Updated", "email": "john@example.com"} |
| DELETE | /users/{id}                | 删除用户         | -                                                           |

## 运行说明

### 1. 启动 MongoDB

```shell
# 拉取镜像
docker pull mongo

# 创建容器
docker run -d -p 27017:27017 --name mongodb -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=admin mongo

# 进入容器
docker exec -it mongodb bash
```

> **注意**: MongoDB 6.0+ 使用 `mongosh` 替代原来的 `mongo` shell

### 2. 启动应用

```shell
mvn spring-boot:run
```

### 3. 访问 API

- 获取所有用户: `curl http://localhost:8080/users`
- 创建新用户: `curl -X POST -H "Content-Type: application/json" -d '{"name":"Alice","email":"alice@example.com"}' http://localhost:8080/users`

## 参考资源

- [Spring WebFlux 官方文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Spring Data MongoDB Reactive 参考](https://spring.io/projects/spring-data-mongodb)
- [Project Reactor 文档](https://projectreactor.io/docs/core/release/reference/)

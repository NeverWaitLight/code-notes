# AT Doc

> [Seata AT 模式](https://seata.apache.org/zh-cn/docs/dev/mode/at-mode/)

AT 模式是 [Seata](https://seata.apache.org) 独有的模式，属于**两阶段提交（2PC）**

# Steps

## Docker network

方便组件之间相互访问

```bash
docker network create test
```

## MySQL

用作 seata-server 的存储，方便调试出错时清除数据。

```bash
docker pull mysql:5.7
docker run --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 --network test -d mysql:5.7
```

> seata-server 只支持 MySQL 5.X

### 初始化数据

- [seata-server 初始化脚本](https://github.com/seata/seata/blob/develop/script/server/db/mysql.sql)
- [demo 初始化脚本](script/xa-tcc-init.sql)

## 部署 nacos-server

主要目的：注册微服务和 Seata-server（协调者 TC）

```bash
docker pull nacos/nacos-server:v2.1.0
docker run --name nacos -e MODE=standalone -p 8848:8848 -p 9848:9848 -p 9849:9849 --network test -d nacos/nacos-server:v2.1.0
```

## 部署 seata-server

```bash
docker pull seataio/seata-server:1.5.1
docker run --name seata -p 8091:8091 -p 7091:7091 -e SEATA_IP=your-intranet-ip --network test -d seataio/seata-server:1.5.1
```

使用你的内网 IP 替换`your-intranet-ip` 之后再执行 `docker run` 命令

seata-server 使用 seata-server/resources/application.yml 配置，修改或添加 application.yml 中包含的如下配置：

```yaml
seata:
  config:
    type: file
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: http://nacos:8848
      group: SEATA_GROUP
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.jdbc.Driver
      url: jdbc:mysql://mysql:3306/seata?rewriteBatchedStatements=true
      user: root
      password: 123456
```

## 测试 demo

启动如下服务:

- business
- order
- storage
- wallet

发送请求：

```bash
curl --location --request POST 'localhost:65010/ordering' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "tom",
    "productId": 1,
    "amount": 1,
    "bill": "10.5"
}'
```

业务会随机失败以检查 AT 事务是否发挥作用

## 如何使用

- 在 Application 类上添加 `@EnableAutoDataSourceProxy(dataSourceProxyMode = "AT")` 注解开启 AT 事务
- TM 使用 `@GlobalTransactional` 开启全局事务，RM 使用 `@Transactional` 加入全局事务

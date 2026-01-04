## Celery 是什么?

Celery 不是替换 MQ，而是构建在 MQ 之上的简化层。如果你讨厌重复造轮子，用 Celery；如果需要极致控制，用直接 MQ。

在 Java 中 Celery ≈ Spring Boot + Spring AMQP/RabbitMQ (或 Spring Cloud Task)，Spring Boot 提供内置的任务执行和调度（@Scheduled 注解像 Celery Beat），结合 Spring AMQP 可以直接集成 RabbitMQ 作为 broker，实现异步任务队列。Spring Cloud Task 进一步扩展为分布式任务，支持短期任务和批处理。
示例：使用@EnableTask 定义任务，结合 RabbitTemplate 发送消息到队列，worker 消费处理。

## Step-by-Step

使用 docker 启动 redis

```sh
docker run -d -p 6379:6379 redis
```

安装依赖

```sh
pip install -r requirements.txt
```

启动 celery 服务

```sh
celery -A <module> worker --loglevel=info -P gevent
# module 就是 tasks.py
celery -A tasks worker --loglevel=info -P gevent
```

为什么和官方命令不一致？添加 `-P gevent` 后缀是因为 Celery 4.0+ 目前尚未正式支持 Windows。但它仍然可以在 Windows 上用于一些开发测试目的。

> [Celery raises ValueError: not enough values to unpack](https://stackoverflow.com/questions/45744992/celery-raises-valueerror-not-enough-values-to-unpack)

执行调用脚本

```sh
python test.py
```

应该输出 8

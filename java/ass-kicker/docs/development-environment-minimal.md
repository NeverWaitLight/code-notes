# 开发/测试环境与凭据最小手册（MVP）

本手册服务于 Story 1.0：给出一条“能跑起来、能验证”的最小路径。

## 1. 目标与范围（最小闭环）

- 本地启动 PostgreSQL 与 Kafka 兼容服务
- 提供统一的环境变量口径（后端为主）
- 给出最小验证步骤（依赖健康 → 服务健康 → API 可达）

## 2. 本地依赖启动（推荐方式：Docker Compose）

仓库已提供最小依赖编排文件：

- `infra/local-dev/docker-compose.yml`

在仓库根目录执行：

```bash
docker compose -f infra/local-dev/docker-compose.yml up -d
```

停止与清理：

```bash
docker compose -f infra/local-dev/docker-compose.yml down
```

说明：

- PostgreSQL 对外端口：`5432`
- Kafka 兼容服务使用 Redpanda：对外端口为 `19092`

## 3. 环境变量最小口径（后端）

仓库已提供最小样例：

- `.env.example`

建议至少包含如下变量：

```env
# PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/ass_kicker
DB_USER=ass_kicker
DB_PASSWORD=ass_kicker

# Kafka / Redpanda（本地外部连接端口）
KAFKA_BOOTSTRAP=localhost:19092

# Auth（示例占位，实际由企业 SSO/密钥系统提供）
JWT_PUBLIC_KEY=__REPLACE_WITH_PUBLIC_KEY_PEM_CONTENT__
JWT_ISSUER=ass-kicker-local
JWT_AUDIENCE=ass-kicker

# 观测性/日志
LOG_LEVEL=INFO
```

关键约定：

- 本地 Kafka 连接应使用 `localhost:19092`（而非容器内地址）
- 任何密钥类配置仅保留占位符，不要提交真实凭据

## 4. 开发环境 JWT 获取/替代方案（最小可行）

优先方案（推荐）：

- 通过企业 SSO/IdP 获取开发环境 JWT，并按统一环境变量口径配置

本地替代方案（当 SSO 不可用时）：

1. 生成一对本地测试密钥（RSA）

```bash
# 生成私钥
openssl genpkey -algorithm RSA -out jwt-private.pem -pkeyopt rsa_keygen_bits:2048

# 导出公钥
openssl rsa -pubout -in jwt-private.pem -out jwt-public.pem
```

2. 将 `jwt-public.pem` 的内容填入：

- `JWT_PUBLIC_KEY`

3. 使用任意 JWT 工具用 `jwt-private.pem` 签发测试 Token

建议的最小 Claims：

- `iss=ass-kicker-local`
- `aud=ass-kicker`
- `sub=dev-user`
- `exp` 为当前时间之后的合理时长

说明：

- 这里的本地方案仅用于开发/测试，不适用于生产
- 如果后续实现提供 `dev` profile 的鉴权旁路，应在安全边界内明确标注

## 5. 最小验证路径（Smoke Test 清单）

当 `ass-kicker-server` 与 `ass-kicker-worker` 模块就位后，按以下顺序验证：

1. 启动本地依赖

```bash
docker compose -f infra/local-dev/docker-compose.yml up -d
```

2. 验证依赖健康（任选其一）

```bash
docker ps
```

3. 启动后端服务（目标命令口径）

```bash
./mvnw -pl services/ass-kicker-server,services/ass-kicker-worker spring-boot:run
```

4. 验证服务健康（目标端点口径）

```bash
curl http://localhost:8080/actuator/health
```

5. 验证最小 API 可达（目标口径，实际以实现为准）

```bash
curl -X POST http://localhost:8080/api/v1/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"businessLineId":"00000000-0000-0000-0000-000000000000","channel":"EMAIL","target":"user@example.com","templateId":"00000000-0000-0000-0000-000000000000","templateVersionId":"00000000-0000-0000-0000-000000000000"}'
```

## 6. 凭据与安全边界（最小原则）

- 开发/测试凭据来源：企业 SSO 或密钥管理系统
- 本仓库只保存“变量名与占位符”，不保存真实密钥
- 推荐在 CI/CD 中通过密钥注入环境变量

## 7. 与架构文档的对齐点

- 技术栈与命令口径参考：`docs/architecture.md`
- Epic/Story 口径参考：`docs/prd.md`
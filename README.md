# RentNest

租房综合服务平台：房源租赁、公共服务、租客服务、社区交流、实时聊天与 AI 助手。

> 实施进度见下方「开发状态」章节。README 随实施推进持续更新。

## 项目结构

模块化单体

```
rentnest/
├── .env.example
├── README.md
├── backend/                    # Spring Boot 单体（内嵌 Netty WS 服务）
│   ├── pom.xml
│   └── src/main/java/com/rentnest/
│       ├── common/             # 统一响应、异常、权限注解、审计
│       ├── auth/               # JWT 登录、角色
│       ├── user/               # 用户资料
│       ├── rental/             # 房源、预约
│       ├── pservice/           # 公共服务（商品、订单）
│       ├── tenant/             # 报修、反馈、公告查询
│       ├── community/          # 帖子、评论、点赞
│       ├── chat/               # 会话/消息（HTTP 查询）+ netty/（WS）
│       ├── file/               # FileStorage 接口 + local/minio 实现
│       ├── ai/                 # agent/（tools+chat）、rag/、mcp/
│       └── admin/              # 管理端聚合接口、账号/角色/审计
│   └── src/main/resources/db/migration/   # Flyway V1..Vn
└── frontend/                   # Vue3 + TS + Vite + Pinia + Element Plus
    └── src/
        ├── views/user/         # 用户端页面
        ├── views/admin/        # 管理端页面（独立 Layout + 路由守卫）
        ├── api/ stores/ router/ composables/
```

- **MySQL**（本机 9.x）：用户、房源、预约、订单、报修、反馈、公告、社区、聊天消息、知识库文档与片段、文件记录 —— 全部业务事实数据，`db/schema.sql` 手动建表。
- **Redis**（本机 8.x）：热点缓存、限流、WS 一次性票据、在线状态、知识库向量检索（RedisVectorStore）。**不是任何业务事实数据的唯一存储。**
- **文件存储**：MinIO（唯一存储方案），业务代码经 `FileService` 统一上传/读取，objectKey 不对外暴露；文件元数据在 MySQL `file_record`。
- **聊天**：Netty 集成在后端进程内（独立端口 9090），连接状态存进程内存；多实例扩展点为 `ChatPushRouter` 接口（可换 Redis Pub/Sub 实现）。

## 启动方式（本地开发，无 Docker）

前置要求：Java 21、Maven 3.9+、Node 20+、本机 MySQL 8+/9+、本机 Redis 8+（需向量检索能力）、本机 MinIO。

MinIO 启动（账号密码与 `.env` 中 `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` 一致，生产必须改）：

```bash
brew install minio
minio server ./data/minio --console-address ":9001"   # RootUser/RootPass 见 .env 的 MINIO_*
# bucket 无需手动创建：应用启动时若 rentnest 不存在会自动创建（MinioStorage.init）
```

```bash
# 1. 准备环境变量
cp .env.example .env     # 填入你的 MySQL/Redis 连接信息

# 2. 初始化数据库（本机 MySQL 中执行一次）
mysql -u root -p -e "CREATE DATABASE rentnest DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p rentnest < backend/src/main/resources/db/schema.sql
# 脚本幂等（CREATE TABLE IF NOT EXISTS），可重复执行

# 3. 启动 Redis（如未运行）
brew services start redis   # 或 redis-server --daemonize yes

# 4. 启动后端（首次会下载依赖）
cd backend && mvn spring-boot:run

# 5. 启动前端
cd frontend && npm install && npm run dev
```

## 默认开发账号

`mysql -u root -p rentnest < backend/src/main/resources/db/seed.sql` 后可用（密码均为 `123456`，仅本地）：

| 账号 | 手机号 | 角色 |
|---|---|---|
| admin | 13800000001 | ADMIN 管理员（全量权限） |
| landlord | 13800000002 | LANDLORD 房东（房源/预约/租客服务处理） |
| user | 13800000003 | USER 普通用户 |

## 测试

```bash
cd backend && mvn test        # 需本机 MySQL / Redis / MinIO 已启动，.env 配置正确
```

- 测试**真连本机服务，不做 mock**：MySQL 走独立库 `rentnest_test`（`createDatabaseIfNotExist` 自动建库，启动时自动执行 `schema.sql` + `seed.sql`，均幂等），不污染开发库 `rentnest`。
- 测试数据会累积在 `rentnest_test`（房源、预约、MinIO 对象），需要干净环境时直接 `DROP DATABASE rentnest_test` 重跑。
- 用例覆盖：登录与 JWT 鉴权、角色越权（USER 访问管理端 403 / 游客 401 / ADMIN 超集放行）、图片上传（magic bytes 校验）、PUBLIC/PRIVATE 文件权限、房源 CRUD 与游客可见性、预约六态状态机与房源状态联动、状态流转留痕与审计日志。

## AI / MCP 配置

- 模型密钥等全部走环境变量（`.env`），代码与仓库不含真实凭据。
- 未配置 `SPRING_AI_OPENAI_API_KEY` 时：业务功能正常启动，AI 接口返回明确的"AI 未配置"提示。
- MCP Server 随应用启动，SSE 端点 `/mcp`，只读工具（房源搜索/详情、公告查询）。

## Redis 向量检索要求与何时引入 Qdrant

- RAG 默认使用 `RedisVectorStore`，要求 Redis ≥ 8.0（本机已满足）。云托管开源版 Redis 不支持向量检索，生产部署请自建 Redis 8+。
- **何时引入 Qdrant**：知识库片段量级超出 Redis 舒适区（数十万片段以上）、需要复杂过滤/混合检索、或 Redis 内存成本过高时。

## 关键假设（README 记录）

1. 需求参考《ZSY.docx》（翱翔安居 V1.0 确认版）；文档中"微信小程序"按最新要求实现为 Vue 3 Web。
2. 文档中的阶段日期为历史规划，不作为当前开发截止日期。
3. **实时聊天与 AI（Agent/RAG/MCP）为新增需求，原需求文档未包含**，按平台扩展能力建设。
4. 角色按文档 6.3.2 拆分：LANDLORD（房东）管房源/预约/租客信息/报修反馈处理；ADMIN（管理员）为超集，另管公共服务、社区内容、公告、账号与审计。USER 为普通用户。
5. 房源状态四态：可出租/预约中/已出租/暂停出租（暂停出租不对用户端展示）；预约六态：待审核/已联系/看房安排中/已确认/已完成/已取消。
6. FAQ 复用公告表（type=FAQ），租客服务与 AI 助手共用。
7. 公共服务以鲜奶配送为示例：订单仅状态流转，无支付/第三方配送，履约扩展点为 `OrderFulfillmentHandler`。
8. 本地开发不使用 Docker，MySQL/Redis 为本机原生服务；部署时如需容器化再补充。

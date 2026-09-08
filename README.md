# self-investor：美股财报监听与 AI 分析 MVP

这是一个 Java 17 / Spring Boot 3 的个人投资研究后端。它定时检查关注公司的 SEC EDGAR filings，识别财报相关的 `8-K`、`10-Q`、`10-K`，选择最有价值的 Earnings Release / `EX-99.1`，清洗内容，调用 OpenAI Structured Outputs 生成结构化基本面分析，持久化到 MySQL，并通过 Telegram 通知。

本项目不包含自动交易、复杂前端、市场一致预期或估值功能。

## 1. 系统架构

```text
MonitorScheduler
  -> SEC submissions API
  -> 过滤 10-Q / 10-K / 8-K Item 2.02
  -> accession_number 唯一约束去重
  -> FilingDetectedEvent
       -> 下载 SEC 完整 submission 并拆分附件
       -> FilingDownloadedEvent
            -> EX-99.1 / Earnings Release 打分选择 + Jsoup 清洗
            -> FilingParsedEvent
                 -> OpenAI Responses API + strict JSON Schema
                 -> AiAnalysisCompletedEvent
                      -> Telegram NotificationService
```

主要 package：

- `company`：关注列表与 REST API
- `monitor`：调度、SEC 类型过滤、开发模拟入口
- `sec`：SEC submissions 与 filing 下载客户端
- `filing`：实体、去重、附件选择、解析、分块与恢复
- `analysis`：OpenAI 客户端、Structured Output DTO、分析存储和查询
- `notification`：通知扩展接口与 Telegram 实现
- `event`：轻量级 Spring ApplicationEvent 流水线
- `config`：类型安全配置与线程池

数据库由 `src/main/resources/db/migration/V1__initial_schema.sql` 创建。`filing.accession_number` 和 `ai_analysis.filing_id` 均有唯一约束。应用重启后只恢复未完成流水线，`COMPLETED` filing 不会再次分析。

## 2. 环境要求

- Java 17
- Docker / Docker Compose（推荐用于本地 MySQL）
- 可访问 SEC、OpenAI 和 Telegram API 的网络
- OpenAI API Key
- 有效且包含联系方式的 SEC User-Agent，例如 `self-investor/0.1 you@example.com`

不要求本机安装 Maven；仓库包含 Maven Wrapper。

## 3. 启动数据库

```bash
docker compose up -d mysql
docker compose ps
```

默认连接是 `jdbc:mysql://localhost:3306/earnings`，用户名和密码均为 `earnings`。生产环境必须替换默认密码。Flyway 在应用启动时自动建表和升级 schema。

## 4. 环境变量

```bash
cp .env.example .env
set -a
source .env
set +a
```

必需配置：

| 环境变量 | 说明 |
|---|---|
| `SEC_USER_AGENT` | SEC 要求的应用名和真实联系方式 |
| `OPENAI_API_KEY` | OpenAI API Key，不得提交到 Git |
| `DB_PASSWORD` | MySQL 密码 |

常用可选配置：

| 环境变量 | 默认值 | 说明 |
|---|---:|---|
| `DB_URL` | 本地 `earnings` 数据库 | JDBC URL |
| `DB_USERNAME` | `earnings` | 数据库用户 |
| `OPENAI_MODEL` | `gpt-5-mini` | 支持 Structured Outputs 的模型 |
| `EARNINGS_MONITOR_ENABLED` | `true` | 是否启用轮询 |
| `EARNINGS_MONITOR_INTERVAL_SECONDS` | `30` | 每轮结束后的等待秒数 |
| `TELEGRAM_ENABLED` | `false` | 是否推送 Telegram |
| `TELEGRAM_BOT_TOKEN` | 空 | BotFather 生成的 token |
| `TELEGRAM_CHAT_ID` | 空 | 接收消息的 chat ID |
| `EARNINGS_SIMULATION_ENABLED` | `false` | 是否打开仅供本地使用的模拟接口 |

API Key、数据库密码和 Telegram token 均只从环境变量读取，日志不会输出这些值。

## 5. 添加关注公司

应用启动后可使用 REST API：

```bash
curl -X POST http://localhost:8080/api/companies \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"CRDO","cik":"1807794","companyName":"Credo Technology Group Holding Ltd","enabled":true}'
```

查看或禁用：

```bash
curl http://localhost:8080/api/companies
curl -X PATCH http://localhost:8080/api/companies/1/enabled \
  -H 'Content-Type: application/json' -d '{"enabled":false}'
```

也可以直接执行 SQL。CIK 建议保存为 10 位字符串：

```sql
INSERT INTO company (ticker, cik, company_name, enabled, created_at, updated_at) VALUES
('CRDO',  '0001807794', 'Credo Technology Group Holding Ltd', TRUE, NOW(6), NOW(6)),
('NVDA',  '0001045810', 'NVIDIA Corporation', TRUE, NOW(6), NOW(6)),
('MU',    '0000723125', 'Micron Technology Inc', TRUE, NOW(6), NOW(6)),
('GOOGL', '0001652044', 'Alphabet Inc', TRUE, NOW(6), NOW(6)),
('MSFT',  '0000789019', 'Microsoft Corporation', TRUE, NOW(6), NOW(6)),
('TSM',   '0001046179', 'Taiwan Semiconductor Manufacturing Co Ltd', TRUE, NOW(6), NOW(6));
```

## 6. 启动应用

```bash
./mvnw spring-boot:run
```

或构建后运行：

```bash
./mvnw clean package
java -jar target/earnings-monitor-0.1.0-SNAPSHOT.jar
```

日志会显示 filing 发现、附件下载和选择、清洗字符数、OpenAI 耗时以及 Telegram 推送结果。HTTP 429、5xx 和网络异常最多重试 3 次，使用指数退避；不可重试的解析错误会写入 `filing.error_message` 并将状态设为 `FAILED`。

## 7. 测试

```bash
./mvnw test
```

测试覆盖：

- `FilingDocumentSelectorTest`：`EX-99.1` 优先于 8-K 主文档和 XBRL 辅助文件
- `FilingDeduplicationTest`：重复 accession number 只保存一次
- `FilingParserTest`：使用本地 HTML fixture 验证噪音删除和表格数字保留
- `AiAnalysisServiceTest`：mock OpenAI 客户端验证输入与 DTO
- `OpenAiClientTest`：mock HTTP server 验证 Responses API strict JSON Schema 请求和反序列化
- `SecMonitorTest`：mock SEC 返回，验证仅处理相关 filing

## 8. 模拟新 Filing

模拟接口默认关闭。设置 `EARNINGS_SIMULATION_ENABLED=true` 并重启。先从 SEC 找一个属于关注公司的真实历史 accession number 和 primary document，然后提交。下载器会从 SEC 拉取真实附件，所以假 accession 会进入 `FAILED`，可用于验证失败链路。

```bash
curl -X POST http://localhost:8080/api/dev/simulate-filing \
  -H 'Content-Type: application/json' \
  -d '{
    "ticker":"CRDO",
    "accessionNumber":"填入真实-accession-number",
    "formType":"8-K",
    "filingDate":"2026-01-01",
    "primaryDocument":"填入真实主文档名.htm",
    "items":"2.02,9.01",
    "secUrl":"填入真实 SEC filing URL"
  }'
```

第二次提交相同 accession 会返回 `created: false`，且不会再次调用 AI。不要在生产环境开启此接口。

## 9. 查看状态和分析结果

```bash
curl http://localhost:8080/api/filings
curl http://localhost:8080/api/filings/{filingId}/analysis
```

也可以直接查询数据库：

```sql
SELECT id, accession_number, form_type, status, error_message, created_at
FROM filing ORDER BY id DESC;

SELECT filing_id, model, summary, analysis_json, created_at
FROM ai_analysis ORDER BY id DESC;
```

原始附件和清洗内容保存在 `filing_document.raw_content` 与 `clean_content`。AI 输出中 `consensusComparisonAvailable` 被强制为 `false`，因为本版本没有接入分析师一致预期，不能判断 Beat/Miss。

## 10. 已知限制

- 只监听 SEC submissions；尚未监听公司 Investor Relations 网站。
- 8-K 仅在 SEC metadata 包含 Item 2.02 时处理，以避免普通 8-K 浪费 AI 调用；极少数分类异常 filing 可能漏检。
- 每份 filing 的 MVP 默认只选得分最高的一个附件，但 selector 返回集合，后续可扩展为多文档联合分析。
- 长内容按财务报表、Guidance、管理层表述和其他内容分类分块；超长单行只能按字符边界切分。
- 没有 Bloomberg / FactSet / LSEG consensus、实时股价、Forward PE、DCF 或电话会文本。
- Telegram 失败会重试并记录日志，但当前没有独立通知 outbox 表。
- `FAILED` 不会无限自动重跑；排查 `error_message` 后需人工修复或增加运维重试入口。
- 单实例运行最符合 MVP 定位。数据库唯一约束可保证多实例发现时不重复入库，但没有分布式任务租约。

未来可在现有 `SecClient`、`NotificationService` 和事件边界后加入 IR 监听、Consensus、Earnings Call、估值和券商接口，而不需要改写核心 filing 流水线。

OpenAI 请求遵循官方 [Structured Outputs 指南](https://platform.openai.com/docs/guides/structured-outputs) 和 [Responses API 参考](https://platform.openai.com/docs/api-reference/responses/create)。SEC 使用配置化 User-Agent，并通过共享限速器控制请求频率。

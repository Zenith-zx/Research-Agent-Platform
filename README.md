# self-investor：美股财报监听与 AI 分析 MVP

这是一个 Java 17 / Spring Boot 3 / MyBatis 的个人投资研究后端。它既能监听 SEC 财报并生成基本面分析，也能在 QQQ 短期超卖时获取跨资产数据和最近市场新闻，生成 Market Stress / Mean Reversion 研究信号。结果使用 OpenAI Structured Outputs、MySQL append-only 历史记录，并支持 Telegram 与邮件通知。

本项目不包含自动交易或复杂前端，不自动采集市场一致预期。宏观发布输入可携带有来源的 consensus。

新增 **Macro Agent V1**：CPI / NFP / FOMC / 用户输入 → 唯一 US_MACRO Thread → 共享动态 Agent Runtime
→ 模型选择事实工具 → 结构化宏观判断 → 校验并稀疏更新 MacroState。
实现入口、配置、示例请求、工具能力边界见 [宏观 Agent 使用与架构说明](docs/macro-agent-v1.md)。

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
                 -> EarningsAnalysisService（graph / chain 可切换）
                 -> Planner -> Research -> Critic
                      -> sufficient -> Finalizer
                      -> insufficient -> Replanner -> Research（有预算循环）
                 -> OpenAI Responses API + strict JSON Schema
                 -> AiAnalysisCompletedEvent
                      -> Telegram NotificationService

MarketStressScheduler（America/New_York 交易时段）
  -> Finnhub Quote API 每小时获取 QQQ 报价
  -> 将上一交易日正式收盘价保存到 MARKET_DAILY_CLOSE
  -> 从 MySQL 累积日线并计算 RSI
  -> Java Wilder RSI + 配置阈值
  -> 数据库 cooldown
  -> OversoldDetectedEvent
       -> 跨资产快照 + 72 小时新闻
       -> MarketContextReadyEvent
            -> OpenAI strict JSON Schema 分类
            -> Java Mean Reversion Score + Action 风险护栏
            -> MARKET_STRESS_ANALYSIS append-only 保存
            -> MarketStressAnalysisCompletedEvent
                 -> Email NotificationService
```

主要 package：

- `company`：关注列表、MyBatis Mapper 接口与 REST API
- `monitor`：调度、SEC 类型过滤、开发模拟入口
- `sec`：SEC submissions 与 filing 下载客户端
- `filing`：实体、MyBatis Mapper 接口、去重、附件选择、解析、分块与恢复
- `analysis`：OpenAI 客户端、MyBatis Mapper 接口、Structured Output DTO、分析存储和查询
- `agent/earnings`：强类型 Agent State、五个节点、条件边、预算、Evidence 和 checkpoint
- `marketstress`：行情/新闻 Provider 抽象、市场 DTO、历史分析 PO 与重触发策略
- `service`：财报与市场压力业务服务、Telegram 和 Email 通知实现
- `repository`：全部 MyBatis Mapper 接口
- `notification`：分析完成后的通知事件分发
- `event`：轻量级 Spring ApplicationEvent 流水线
- `config`：类型安全配置与线程池

数据库由 `V1__initial_schema.sql` 创建财报表，`V2__market_stress_analysis.sql` 创建市场压力历史表，`V3__market_daily_close.sql` 创建每日收盘价表，`V4__earnings_agent_run.sql` 创建 Agent run/checkpoint 表，表名统一为大写。业务 SQL 全部位于 `src/main/resources/mapper/*.xml`。`EARNINGS_AGENT_RUN` 在每个节点后更新强类型 State 的 JSON snapshot；`AI_ANALYSIS` 继续保存兼容旧 API 和通知的最终结果。

## 2. 环境要求

- Java 17
- Docker / Docker Compose（推荐用于本地 MySQL）
- 可访问 SEC、OpenAI 和 Telegram API 的网络
- OpenAI API Key
- 启用 Market Stress 时需要 Finnhub API Key
- 有效且包含联系方式的 SEC User-Agent，例如 `self-investor/0.1 you@example.com`

不要求本机安装 Maven；仓库包含 Maven Wrapper。

项目通过 `.mvn/maven.config` 自动加载 `.mvn/settings.xml`，直接连接 Maven Central，并使用独立的 `${user.home}/.m2/repository-self-investor` 本地仓库。它不会修改或依赖公司级 `~/.m2/settings.xml`。首次导入会重新下载依赖；在 IntelliJ IDEA 中执行一次 Maven Reload 即可。

## 3. 启动数据库

```bash
docker compose up -d mysql
docker compose ps
```

默认连接是 `jdbc:mysql://localhost:3306/earnings`，用户名和密码均为 `earnings`。生产环境必须替换默认密码。Flyway 在应用启动时自动建表和升级 schema。

如果你已经用旧版小写表名执行过 `V1__initial_schema.sql`，本次仍处于 MVP 初始化阶段，建议删除本地 `earnings` 数据库或 Docker volume 后重新创建；否则 Flyway 会因 V1 checksum 变化拒绝启动。不要在已有生产数据的数据库上直接删除，应先另写数据迁移脚本。

## 4. 环境变量

```bash
cp .env.example .env
```

Spring Boot 会通过 `spring.config.import` 自动加载项目根目录的 `.env`，无需在 Shell 中执行 `source .env`。

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
| `OPENAI_MODEL` | `gpt-5.6-sol` | 财报与市场压力 Structured Outputs 分析模型 |
| `OPENAI_CONNECT_TIMEOUT` | `10s` | OpenAI TCP 连接超时 |
| `OPENAI_READ_TIMEOUT` | `120s` | OpenAI 响应读取超时 |
| `OPENAI_PROXY_HOST` | 空 | OpenAI HTTP 代理主机；AWS 可留空直连 |
| `OPENAI_PROXY_PORT` | `0` | OpenAI HTTP 代理端口；配置代理主机时必须为有效端口 |
| `EARNINGS_MONITOR_ENABLED` | `true` | 是否启用轮询 |
| `EARNINGS_MONITOR_INTERVAL_SECONDS` | `30` | 每轮结束后的等待秒数 |
| `EARNINGS_FILING_LOOKBACK_DAYS` | `30` | 只注册最近若干自然日内的 Filing，避免首次启动处理多年历史数据 |
| `EARNINGS_RECOVERY_BATCH_SIZE` | `20` | 每次启动最多恢复的近期未完成 Filing 数量 |
| `EARNINGS_ANALYSIS_MODE` | `graph` | `graph` 使用 LongGraph Agent，`chain` 回退到旧固定分析 |
| `EARNINGS_AGENT_MAX_ROUNDS` | `3` | 最大研究轮数 |
| `EARNINGS_AGENT_MAX_STEPS` | `20` | 最大 Graph 节点执行数 |
| `EARNINGS_AGENT_MAX_LLM_CALLS` | `15` | 最大逻辑 LLM 调用数（含错误 JSON 重试） |
| `EARNINGS_AGENT_JSON_RETRY_ATTEMPTS` | `2` | Structured Output 解析失败后的最大重试次数 |
| `TELEGRAM_ENABLED` | `false` | 是否推送 Telegram |
| `TELEGRAM_BOT_TOKEN` | 空 | BotFather 生成的 token |
| `TELEGRAM_CHAT_ID` | 空 | 接收消息的 chat ID |
| `EARNINGS_SIMULATION_ENABLED` | `false` | 是否打开仅供本地使用的模拟接口 |

Market Stress 配置：

| 环境变量 | 默认值 | 说明 |
|---|---:|---|
| `MARKET_STRESS_ENABLED` | `false` | 总开关；默认关闭以免影响原财报功能 |
| `MARKET_STRESS_SYMBOLS` | `QQQ` | 扫描标的，未来可用逗号扩展 |
| `MARKET_STRESS_RSI_PERIOD` | `6` | 超卖触发 RSI 周期 |
| `MARKET_STRESS_RSI_TRIGGER` | `30` | 严格低于此值才触发 |
| `MARKET_STRESS_COOLDOWN_HOURS` | `12` | 同标的 AI 分析冷却时间 |
| `MARKET_STRESS_INTERVAL_MINUTES` | `60` | 盘中扫描间隔 |
| `MARKET_STRESS_MARKET_HOURS_ONLY` | `true` | 只在纽约常规交易时段扫描 |
| `FINNHUB_API_KEY` | 空 | Finnhub Quote 与 Market News API Key |
| `FINNHUB_BASE_URL` | `https://finnhub.io/api/v1` | Finnhub API 根地址 |
| `FINNHUB_VIX_SYMBOL` | `^VIX` | VIX 查询代码；免费套餐不可用时保存为 `null` |
| `MARKET_STRESS_NEWS_LOOKBACK_HOURS` | `72` | 新闻回溯窗口 |
| `MARKET_STRESS_MAX_NEWS_ARTICLES` | `30` | 最多发送给 GPT 的新闻数量 |

邮件配置：

| 环境变量 | 默认值 | 说明 |
|---|---:|---|
| `MARKET_STRESS_EMAIL_ENABLED` | `false` | 是否创建邮件通知渠道 |
| `MARKET_STRESS_EMAIL_RECIPIENTS` | 空 | 逗号分隔的收件人 |
| `MAIL_HOST` / `MAIL_PORT` | 空 / `587` | SMTP 服务地址和端口 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 空 | SMTP 凭证 |
| `MAIL_SMTP_AUTH` | `true` | 是否启用 SMTP 认证 |
| `MAIL_STARTTLS_ENABLE` | `true` | 是否启用 STARTTLS |

API Key、SMTP/数据库密码和 Telegram token 均只从环境变量读取，日志不会输出这些值。

### 启用 Market Stress

最小配置如下；先保持邮件关闭验证行情和数据库，再配置 SMTP：

```bash
export MARKET_STRESS_ENABLED=true
export FINNHUB_API_KEY='your-finnhub-api-key'
export OPENAI_API_KEY='your-openai-key'
```

Scheduler 每 60 分钟检查一次，但只在 `America/New_York` 的正常交易日和常规时段执行。每次 Quote 响应中的 `pc` 是上一交易日正式收盘价，系统将其按交易日期去重保存；`c` 只作为本轮盘中价格参与实时 RSI，不会冒充正式收盘价写入历史表。

```sql
SELECT symbol, trade_date, close_price, source
FROM MARKET_DAILY_CLOSE
WHERE symbol = 'QQQ'
ORDER BY trade_date;
```

系统不会调用 Finnhub Premium `/stock/candle`。RSI6 需要至少 7 个价格观测值，盘中计算时通常是 6 个已保存收盘价加当前价；RSI14 至少需要 15 个价格观测值。数据不足时只记录收盘价并输出积累进度，不创建分析记录、不调用 OpenAI。应用必须每个交易日至少成功运行一次；停机跨过多个交易日时，免费 Quote API 无法补回漏掉的历史收盘价。

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
INSERT INTO COMPANY (ticker, cik, company_name, enabled, created_at, updated_at) VALUES
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

日志会显示 filing 发现、附件下载和选择、清洗字符数、OpenAI 耗时以及 Telegram 推送结果。HTTP 429、5xx 和网络异常最多重试 3 次，使用指数退避；不可重试的解析错误会写入 `filing.error_message` 并将状态设为 `FAILED`。首次启动默认只注册最近 30 天的 Filing；线程池满载时扫描线程会主动分担任务形成反压，不会丢失已提交事件。应用重启后会分批恢复近期停留在 `DETECTED`、`DOWNLOADING`、`DOWNLOADED`、`PARSING`、`PARSED` 或 `ANALYZING` 状态的任务。

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
- `TechnicalIndicatorServiceTest`：已知序列验证 Wilder RSI6 / RSI14
- `OversoldTriggerTest`：验证可配置阈值
- `CooldownTest`：验证同标的冷却期内不重复分析
- `MeanReversionScoreTest`：验证配置权重和系统性风险护栏
- `MarketStressAiServiceTest`：mock 共用 OpenAI 客户端
- `MarketSnapshotBuilderTest`：验证部分跨资产数据缺失时降级
- `SchedulerTest`：验证非交易时间和纽约 DST

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
FROM FILING ORDER BY id DESC;

SELECT filing_id, model, summary, analysis_json, created_at
FROM AI_ANALYSIS ORDER BY id DESC;

SELECT run_id, filing_id, status, current_round, step_count, llm_call_count, confidence
FROM EARNINGS_AGENT_RUN ORDER BY start_time DESC;

SELECT id, symbol, analysis_time, rsi6, mean_reversion_score, action, status, error_message
FROM MARKET_STRESS_ANALYSIS ORDER BY id DESC;

SELECT symbol, trade_date, close_price, source, created_at
FROM MARKET_DAILY_CLOSE ORDER BY symbol, trade_date;
```

原始附件和清洗内容保存在 `FILING_DOCUMENT.raw_content` 与 `clean_content`。AI 输出中 `consensusComparisonAvailable` 被强制为 `false`，因为本版本没有接入分析师一致预期，不能判断 Beat/Miss。

## 10. 已知限制

- 只监听 SEC submissions；尚未监听公司 Investor Relations 网站。
- 8-K 仅在 SEC metadata 包含 Item 2.02 时处理，以避免普通 8-K 浪费 AI 调用；极少数分类异常 filing 可能漏检。
- 每份 filing 的 MVP 默认只选得分最高的一个附件，但 selector 返回集合，后续可扩展为多文档联合分析。
- 长内容按财务报表、Guidance、管理层表述和其他内容分类分块；超长单行只能按字符边界切分。
- 没有 Bloomberg / FactSet / LSEG consensus、实时股价、Forward PE、DCF 或电话会文本。
- Telegram 失败会重试并记录日志，但当前没有独立通知 outbox 表。
- `FAILED` 不会无限自动重跑；排查 `error_message` 后需人工修复或增加运维重试入口。
- 单实例运行最符合 MVP 定位。数据库唯一约束可保证多实例发现时不重复入库，但没有分布式任务租约。
- Market Stress 使用 Finnhub 免费 Quote API，不调用 Premium Candle。新数据库需要先积累收盘价：RSI6通常约需6个已结束交易日，RSI14约需14个已结束交易日，MA20/MA50/MA200需要更长时间。
- 如果程序某个交易日完全没有运行，后续免费 Quote 只能提供最近一个上一收盘价，无法自动补齐更早缺口；需要人工导入可信历史数据或等待重新积累。
- Finnhub免费 Quote 主要覆盖美国股票。VIX是否可用取决于当前套餐；US2Y、US10Y、US30Y、DXY、WTI、Brent和Gold当前明确保存为 `null`，不会使用ETF或期货冒充宏观现货序列。
- 新闻相关性第一版采用 Finnhub `general` Market News 加本地关键词分类，不包含复杂爬虫、新闻反应函数或自动重大新闻重触发。
- 该模块只读取 Finnhub 行情并生成 AI Investment Research Signal，没有任何下单接口，也不会执行交易。

未来可在现有 `SecClient`、`NotificationService` 和事件边界后加入 IR 监听、Consensus、Earnings Call、估值和券商接口，而不需要改写核心 filing 流水线。

OpenAI 请求遵循官方 [Structured Outputs 指南](https://platform.openai.com/docs/guides/structured-outputs) 和 [Responses API 参考](https://platform.openai.com/docs/api-reference/responses/create)。SEC 使用配置化 User-Agent，并通过共享限速器控制请求频率。

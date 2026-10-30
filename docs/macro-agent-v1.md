# Macro Agent V1：架构与运行说明

## 1. 从哪里开始看

先看 `MacroController.event()` → `MacroTrigger.published()`，再进入共享 Runtime。
宏观判断规则在 `prompts/macro-agent-system.txt`，不是写死在 Java 中的 CPI/FOMC 节点。

主要文件（相对于项目根目录）：

| 文件 | 职责 |
| --- | --- |
| `src/main/java/com/selfinvestor/earnings/controller/MacroController.java` | 事件、用户输入、Steer、状态与审计 HTTP 入口 |
| `src/main/java/com/selfinvestor/earnings/agent/macro/MacroTrigger.java` | CPI/NFP/FOMC 目标与 Submission 入队，不做判断 |
| `src/main/java/com/selfinvestor/earnings/agent/runtime/AgentLoop.java` | 公司和宏观共用的 Decide → Tool → Observation 循环 |
| `src/main/java/com/selfinvestor/earnings/agent/runtime/profile/AgentDomain.java` | 领域扩展接口；未来可加主题，不另造 Runtime |
| `src/main/java/com/selfinvestor/earnings/agent/macro/MacroAgentDomain.java` | US_MACRO_ANALYST Profile、状态上下文、最终结果和精简记忆 |
| `src/main/java/com/selfinvestor/earnings/agent/macro/MacroStateService.java` | 状态读取、前态验证、稀疏更新和版本控制 |
| `src/main/java/com/selfinvestor/earnings/agent/macro/MacroFacts.java` | 只读事实查询，包装已有行情/新闻 Provider |
| `src/main/java/com/selfinvestor/earnings/config/MacroToolsConfiguration.java` | 将事实能力注册为共享 AgentTool |
| `src/main/java/com/selfinvestor/earnings/agent/runtime/AgentCompletionService.java` | 状态、分析记录、记忆、FINAL_RESPONSE、Turn 完成的原子事务 |
| `src/main/resources/prompts/macro-agent-system.txt` | surprise、趋势、传导机制、反例和不确定性约束 |
| `src/main/resources/prompts/macro-decision-schema.json` | 严格结构化结果；合法枚举与稀疏维度变更 |
| `src/main/resources/db/migration/V6__macro_agent.sql` | 宏观表 migration |

新增 Java 方法均使用多行 JavaDoc，所有枚举含中文 `desc`，if/else 单句也保留大括号，Bean 依赖使用 `@Resource`。

## 2. 共享运行架构

```text
CPI / NFP / FOMC 人工导入或未来监听器 / 用户输入
  → MacroTrigger
  → SubmissionQueue → SubmissionDispatcher
  → MACRO / US_MACRO 长期 Thread
  → TurnManager.startOrSteer
  → US_MACRO_ANALYST Profile
  → AgentRuntime → AgentLoop
  → LLM Decide → ToolExecutor → Observation → LLM Decide …
  → FINAL_ANSWER + MacroAnalysisResult
  → AgentCompletionService（事务）
       → 校验 MacroStateChange.from
       → 只更新变化维度
       → MACRO_ANALYSIS + Thread Memory + FINAL_RESPONSE + COMPLETED
  → 可选通知
```

没有 MacroAgentRuntime、固定 Macro Workflow 或独立模型循环。
Profile 中的工具白名单同时用于 ContextBuilder 展示和 ToolExecutor 执行检查，不能靠模型调用工具名越权。
模型传输层使用 `argumentsJson` JSON 对象字符串，转换后仍是共享 AgentDecision 的 Map 参数；
因此严格输出 Schema 不需要开放的 `additionalProperties: true`。

## 3. US_MACRO 与状态更新

首次提交或读取状态时，以 `SubjectType.MACRO + subjectCode=US_MACRO` 获取/创建 Thread；
现有唯一索引保证长期复用，不会为每份 CPI 单独建立 Thread。

MacroState 有 growth、inflation、labor、fed、liquidity、rates、dollar、risk 八个强类型维度，
初始均为 UNKNOWN，并有 version / updatedAt。UNKNOWN 是证据不足，不等同于中性。

模型最终结果包含 summary、whatChanged、keyFindings、marketImpacts、marketReaction、
researchImplications、risks、confidence 和 proposedStateChange。例如：

```json
{
  "inflation": {
    "from": "STICKY",
    "to": "REACCELERATING",
    "reason": "这里应写实际取得的多期一致证据，不能仅凭单次读数"
  }
}
```

Java 校验合法字段/枚举、from 与数据库一致、reason 非空；锁定状态行并检查版本。
未变化的维度不进入 UPDATE，全部无变化时不更新 version / updatedAt。
from=to 的无效变化在验证后从报告和变化历史中剔除。
任何最终持久化步骤失败，状态、分析记录、记忆和最终 Item 一起回滚。
工具不能写 regime，也不存在外部直接修改 regime 的 HTTP API。

每步上下文包含当前状态、最近五份分析摘要、最近五次实质状态变化、当前目标/预算/事件/工具结果/Steer。
长期 Memory 仅存当前 thesis、regimes 和最近三次变化，不复制完整工具输出。
超长当前 Turn 上下文优先移除较旧的内部观察并注明缺失，保留外部输入；必需输入本身超限则明确失败，
不会拼接截断后的非法顶层 JSON。单条正文也可能被截短，完整记录仍在 TurnItem。

未来公司分析可调用 `MacroStateService.getCurrentUsMacroState()`；本次没有把 Earnings Context 强制耦合到宏观。

## 4. 三类事件如何触发

- CPI：输入 headline/core 的 YoY/MoM、actual/consensus/previous；关注 surprise、构成和趋势，判断通胀与 Fed 路径。
- NFP：输入 payrolls、失业率、工资增长、参与率和 revisions；判断就业降温程度及增长/政策传导。
- FOMC：输入 rateDecision、statement、previousStatement，可附 projections（SEP/dot plot）、powellRemarks；
  关注声明变化与政策立场，不由 Java 用关键词判定鹰鸽。
- MACRO_USER_INPUT：`POST /api/macro/input`，文本即研究目标。

都只产生 Submission。其他 SubmissionType 是预留位，事件入口会拒绝 V1 尚不支持的类型。
数据允许部分缺失：用 null 或省略字段，不要填 0；单位必须明确，预期来源通过 consensusSource 提供。
releaseDate 必须带时区，数据库排序按 UTC 归一化。source 是导入者声明的来源，并不代表系统已独立核验。

以下是自动化测试实际执行过的 Fake LLM 路径，不是线上实时调查，也不是固定顺序：

```text
CPI_RELEASED → Submission → Turn
→ get_macro_history → get_treasury_yields → get_dollar_index
→ FINAL → 校验状态 → 保存分析/记忆 → COMPLETED

FOMC_RELEASED → Submission → Turn
→ get_fomc_statement → compare_previous_fomc_statement → get_treasury_yields
→ FINAL → 校验状态 → 保存分析/记忆 → COMPLETED
```

真实模型可以选择其他允许的路径，也可在证据不足时明确说明限制或 WAIT_FOR_USER。

## 5. 工具与数据能力边界

| 工具 | 已实现的能力 | 数据限制 |
| --- | --- | --- |
| get_macro_release | 读取带来源/时间的导入发布 | 没有自动 BLS/BEA/Fed 采集；不是实时官方接口 |
| get_macro_history | 最多 24 份导入记录，含子指标与修订 | 需要先导入历史；可能有断档，不同版本不等于独立月份 |
| get_cpi_components | 读取导入的 CPI 分项 | 未导入则 available=false，不推造 Supercore 等 |
| get_fomc_statement | 读取导入的声明、利率决定、SEP/发言 | 可选内容没提供就缺失 |
| compare_previous_fomc_statement | 并列返回本次/前次原文 | 不做 Java 固定鹰鸽解释；两份原文均需提供 |
| get_market_index | 复用 Finnhub SPY/QQQ/IWM 当前报价和本地日线 | 依赖 API Key/权限；日涨跌不是事件窗口反应 |
| get_vix | 复用已有 Finnhub VIX 查询 | 依赖权限及 VIX symbol 配置，可能 unavailable |
| get_macro_news | 复用 Finnhub 48 小时市场新闻，最多 12 条 | 新闻是补充，不等于官方事实，也不一定全属宏观 |
| get_treasury_yields | 四期限及同时间/单位的 2s10s、5s30s 包装和计算 | **当前 Finnhub Provider 没有国债 feed，实际返回缺失** |
| get_dollar_index | Provider 接口适配与缺失标记 | **当前 Provider 没有 DXY 数据，趋势未实现** |
| get_oil_price | WTI Provider 接口适配与缺失标记 | **当前 Provider 没有原油数据** |

未实现/未注册：Fed Funds futures / 降息概率、TIPS real yields、sector performance、自动全球宏观监听。
Treasury/DXY/WTI 的工具包装可用不代表底层数据已经接通。它们没有随机数、常量假行情或静默替代标的。
空数据是数据缺口，异常会成为失败 Observation，模型可以继续用其他证据；累计失败达到限制则 FAILED。

## 6. 如何启用和手动运行

在本地配置中设置（不要把真实密钥提交到 Git）：

```properties
SELF_INVESTOR_AGENT_ENABLED=true
SELF_INVESTOR_MACRO_ENABLED=true
SELF_INVESTOR_MACRO_MAX_STEPS=15
SELF_INVESTOR_MACRO_MAX_LLM_CALLS=15
SELF_INVESTOR_AGENT_MAX_REPEATED_TOOL_CALLS=2
SELF_INVESTOR_AGENT_MAX_TOOL_FAILURES=5
SELF_INVESTOR_MACRO_NOTIFICATIONS_ENABLED=false
```

仍需已有 MySQL / OPENAI_API_KEY / OPENAI_MODEL 配置。行情和新闻需要 FINNHUB_API_KEY；
缺少行情能力不妨碍导入事实，但模型必须报告限制。
`SELF_INVESTOR_AGENT_ENABLED=true` 也会让已有财报分析走共享动态 Runtime；这是已有开关语义。
只设置 macro=true 而不启用共享 Runtime 时，研究入口会拒绝入队。

在项目根目录启动：

```bash
./mvnw spring-boot:run
```

**示例文件都是明确标注的合成数据，不是实际 CPI/NFP/FOMC。请使用独立演示数据库，避免污染真实 US_MACRO 记忆。**
实际 HTTP 触发会调用配置的模型，可能产生 API 费用。自动测试不调用真实模型，也不发送真实通知。

先导入历史（仅保存事实，不触发模型；可自行准备多个实际历史 period）：

```bash
curl -X POST http://localhost:8080/api/macro/releases \
  -H 'Content-Type: application/json' \
  --data-binary @docs/examples/cpi-demo.json
```

触发 CPI：

```bash
curl -X POST http://localhost:8080/api/macro/events/CPI_RELEASED \
  -H 'Content-Type: application/json' \
  --data-binary @docs/examples/cpi-demo.json
```

NFP / FOMC：

```bash
curl -X POST http://localhost:8080/api/macro/events/NFP_RELEASED \
  -H 'Content-Type: application/json' --data-binary @docs/examples/nfp-demo.json
curl -X POST http://localhost:8080/api/macro/events/FOMC_RELEASED \
  -H 'Content-Type: application/json' --data-binary @docs/examples/fomc-demo.json
```

返回 202 accepted、submissionId、threadId，表示入队，不是已完成。
同 releaseId 的事实不可覆盖（内容不同会拒绝，修订请使用新 ID）；
重复 POST 事件仍是新的研究输入，V1 不提供事件级 exactly-once 去重。

查看结果和追加输入：

```bash
curl http://localhost:8080/api/macro/thread
curl http://localhost:8080/api/macro/state
curl http://localhost:8080/api/macro/turns
curl http://localhost:8080/api/macro/turns/替换为turnId/items
curl http://localhost:8080/api/macro/turns/替换为turnId/analysis

curl -X POST http://localhost:8080/api/macro/input \
  -H 'Content-Type: application/json' -d '{"text":"当前美国通胀判断有哪些证据缺口？"}'
curl -X POST http://localhost:8080/api/macro/steer \
  -H 'Content-Type: application/json' -d '{"text":"重点解释为什么 QQQ 没跌，并区分日涨跌和事件反应"}'
curl -X POST http://localhost:8080/api/macro/cancel
```

`turns` 返回最近 20 次 Turn（含 status / finalResponse / failureMessage）。
`analysis` 只在完成后可用，其 resultJson 为完整结构化结果，changesJson 为实际变化。
Steer 有活跃 Turn 时追加同一 Turn；没有活跃 Turn 时按现有共享语义启动新的研究。
模型调用期间到达的 Steer 会使旧 FINAL/WAIT 失效并重新决策。

通知：启用 SELF_INVESTOR_MACRO_NOTIFICATIONS_ENABLED 后，复用已有 Telegram/Email 配置。
邮件沿用 MARKET_STRESS_EMAIL_ENABLED 与收件人；Telegram 沿用 TELEGRAM_ENABLED。
通知只在结果提交后执行，故障会记录 ERROR，不撤销已完成状态；暂未实现持久化通知重试队列。

## 7. 数据库与并发

V6 新建：

- MACRO_STATE：一 Thread 一行，八个枚举、version、updated_at。
- MACRO_ANALYSIS：一 Turn 一条结果、实际变更、状态版本、摘要、时间。
- MACRO_RELEASE：不可变发布档案，releaseId 唯一，包含来源和所有可选事实字段的 JSON。

复用 V5 的 AGENT_THREAD / AGENT_TURN / AGENT_TURN_ITEM；不修改旧 migration，
无需清空已有数据库。启动时 Flyway 自动应用 V6。已升级环境不要手动编辑已执行 migration。

V1 **仅支持单应用实例**：每 Thread 的 start/steer/finish 短锁保证最多一个 Active Turn；
Runtime 执行锁保证旧取消任务真正退出前，不启动同 Thread 的下一条 Loop。
状态行锁与乐观版本检查是额外保护，不等于已经实现多实例运行协调。

SubmissionQueue 沿用进程内有界队列，重启会丢失未分发输入；RUNNING Turn 不自动从断点恢复。
重启后可查看审计并取消残留活跃任务，再重新提交。WAITING Turn 可通过 Steer 恢复，预算和重复调用计数保留。
没有 MQ、Redis、Vector DB、Theme Agent 或自动交易扩展。
接口沿用项目现有无鉴权方式，应仅在可信本地网络使用，不要直接暴露公网。

## 8. 验证

```bash
./mvnw test
./mvnw -Dtest=MacroAgentIntegrationTest,MacroCompletionTransactionTest,MacroConcurrencyTest test
```

已覆盖：CPI/FOMC 的 Fake LLM 动态链；STICKY → REACCELERATING；无变化/无效变更不覆盖状态；
Steer 不新建 Turn且不遗漏最新输入；失败 Observation 后继续；重复工具限制；
WAIT 恢复后不重置限制；Profile 双重权限；NFP/用户输入/预留类型边界；导入不可变；
失败预算；非法枚举；完成事务原子回滚；并发输入唯一 Active Turn；取消后的 Loop 串行；
Flyway、MyBatis 和全项目 JavaDoc / if 大括号 / 枚举 desc 检查。

测试使用 H2 MySQL 模式及 Fake/Mock 模型、行情。未连接真实生产 MySQL、实时宏观源、真实模型或通知渠道；
不把通过单测宣称为已完成外部供应商联调。当前 V1 最重要的后续能力是可靠的官方发布/历史/预期数据接入，
以及国债、DXY、实际收益率与严格事件窗口行情。

本次完整回归结果：69 项测试，0 失败、0 错误、0 跳过；其中新增 16 项宏观集成/事务/并发测试，
以及 2 项共享模型传输格式测试。git diff --check 通过。

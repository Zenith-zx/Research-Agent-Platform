# 大模型接入：策略模式与 DeepSeek

## 如何切换

默认仍使用 OpenAI，原来的 `OPENAI_*` 配置继续生效。切换 DeepSeek 时，在项目根目录 `.env` 中设置并重启应用：

```properties
LLM_PROVIDER=DEEPSEEK
DEEPSEEK_API_KEY=替换为自己的密钥
DEEPSEEK_MODEL=deepseek-flash
```

不要提交真实密钥。DeepSeek 被选中时不需要 OpenAI 密钥，反之亦然。此开关同时作用于财报旧 Chain、财报 Graph、共享 Agent Runtime（含宏观）和市场压力分析，不改变业务启用开关或数据库结构。

| 配置 | 默认值 | 含义 |
|---|---|---|
| `LLM_PROVIDER` | `OPENAI` | `OPENAI` 或 `DEEPSEEK`；未知值启动失败，修改后重启 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | API 根地址；也支持末尾 `/v1`，不要填写完整 `/chat/completions` |
| `DEEPSEEK_MODEL` | `deepseek-flash` | DeepSeek 模型 ID，可按账号支持情况自行配置 |
| `DEEPSEEK_API_KEY` | 空 | 仅实际调用所选供应商时要求非空 |
| `DEEPSEEK_MAX_INPUT_CHARS` | `60000` | 财报分块、Graph 源材料与 Runtime 上下文的字符预算，不是 token 数 |
| `DEEPSEEK_MAX_TOKENS` | `8192` | 单次生成上限；思考模式下预算包含思考输出，需结合模型限制调整 |
| `DEEPSEEK_THINKING_ENABLED` | `false` | 显式控制思考模式；只解析最终 `content`，不保存 `reasoning_content` |
| `DEEPSEEK_CONNECT_TIMEOUT` | `10s` | TCP 连接超时 |
| `DEEPSEEK_READ_TIMEOUT` | `120s` | 响应读取超时；思考模式可能需要调大 |
| `DEEPSEEK_PROXY_HOST` / `DEEPSEEK_PROXY_PORT` | 空 / `0` | 独立 HTTP 代理配置，不继承 OpenAI 代理 |

切回 OpenAI：`LLM_PROVIDER=OPENAI`，配置 `OPENAI_API_KEY`、`OPENAI_MODEL`。可通过 `OPENAI_BASE_URL` 设置根地址（支持 `/v1`），其他原有超时、代理和输入预算变量保持不变。

## 代码从哪里看

```text
业务入口
├─ AnalysisModelService          财报 Chain / 市场压力 Prompt 与 DTO
├─ DefaultEarningsAgentModel     财报 Graph 决策与原有预算重试
└─ DefaultAgentDecisionModel     共享 Runtime，包括 Macro Profile
          ↓
      LlmClient                 策略上下文，按 LlmProperties 注册/选择
          ↓ LlmStrategy
          ├─ OpenAiLlmStrategy   Responses /v1/responses
          └─ DeepSeekLlmStrategy Chat Completions /chat/completions
          ↓
      LlmHttpTransport           连接、超时、代理、有限瞬态重试
          ↓
      StructuredOutputParser     JSON + JSON Schema + DTO 校验
```

建议先读 `llm/LlmStrategy.java` 和 `llm/LlmClient.java`，再看 `llm/strategy/DeepSeekLlmStrategy.java`。业务类不再直接依赖 OpenAI：原 `OpenAiClient` 的业务提示适配拆到 `AnalysisModelService`，HTTP 协议拆到具体策略；两个 OpenAI 命名的 Agent 适配类改成供应商无关的 `Default*`。

新增供应商只需：为 `LlmProvider` 增加带中文 `desc` 的枚举值，增加独立配置，实现并注册 `LlmStrategy` Spring Bean，再增加对应协议测试。无需在每个业务服务中新增 `if/else`。所有新增生产方法使用多行 JavaDoc，条件分支带大括号，Bean 优先使用 `@Resource` 注入。

## 输出约束与错误行为

OpenAI 保留 `text.format.type=json_schema` 和 `strict=true`。[官方 Structured Outputs 说明](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses)

DeepSeek 使用 `response_format.type=json_object`，系统提示中包含 JSON 指令及完整业务 Schema。JSON Output 不保证符合指定 Schema，所以两种策略都经过本地 Draft 2020-12 校验，再反序列化为 DTO。[DeepSeek JSON Output 说明](https://api-docs.deepseek.com/guides/json_mode/)

- 检查 required、additionalProperties、枚举、类型、数值范围、嵌套本地 `$ref` 和 `anyOf`，保留现有财报/宏观/市场压力契约。
- 拒绝空输出、非法 JSON、重复键、多段 JSON；DeepSeek `finish_reason` 必须为 `stop`，截断或过滤结果不作为成功返回。
- 模型配置、实际落库模型名称和输入预算均跟随所选策略。
- HTTP 429、5xx 和网络故障最多重试三次；400/401/403 不重试。HTTP 错误消息只报告供应商及状态码，不携带上游错误正文。
- 共享客户端不额外发起“修复 JSON”请求；财报 Graph 原有的有限 JSON 重试仍由其预算逻辑管理。
- 不自动跨供应商回退，避免意外把数据发给另一家服务或重复产生费用。输出校验失败交回现有业务失败流程。
- Schema 只允许本地片段引用，禁止隐式远程加载。此校验仅保证结构，不能替代现有宏观状态更新、证据与评分等业务校验，也不能保证分析事实正确。

当前默认 DeepSeek 模型名和 `thinking` 参数依据 [Chat Completions 官方协议](https://api-docs.deepseek.com/api/create-chat-completion/)；账号可用模型及费用以服务商为准。

## 验证

```bash
./mvnw clean test
```

`LlmStrategyHttpTest` 使用本地 MockWebServer 检查两个协议、鉴权、路径前缀、策略选择、异常与重试。`LlmBusinessIntegrationTest` 使用真实 Spring 注入和业务 Schema，验证 DeepSeek 财报、宏观决策和市场压力输出；不会调用真实 API。既有 Graph、Runtime、Macro、持久化及代码格式测试继续执行。

真实 API 联调需要自行配置密钥并通过已有业务入口触发，会产生供应商费用；自动测试不能验证账号权限、真实模型输出质量或线上连通性。

# Hotel Pricing System — ADD 3.0 Multi-Agent Architecture Design

基于 **Spring AI Alibaba** 多 Agent 框架，使用 **GPT-5.4** 模型执行 **ADD 3.0** 方法，
完成 Hotel Pricing System 的架构设计。

## 项目配置

- **AI 范式**: Option 3 — Multi-Agent（Supervisor + Sub-Agent 协作验证）
- **模型**: `pa/gpt-5.4`（通过 PPIO OpenAI 兼容 API）
- **API 端点**: `https://api.ppio.com/openai`
- **框架**: Spring AI Alibaba Agent Framework 1.1.2.2
- **方法论**: Attribute-Driven Design (ADD) 3.0

## 快速开始

### 1. 前置条件

- **JDK 17+**
- **Maven 3.6+**（项目自带 `mvnw`，无需系统安装 Maven）
- **PPIO API Key**（由助教在课堂上分发）

### 2. 配置 API Key

```bash
# Linux / macOS / Git Bash
export PPIO_API_KEY="助教分发的API-Key"
```

> **Windows CMD:**
> ```cmd
> set PPIO_API_KEY=助教分发的API-Key
> ```
>
> **Windows PowerShell:**
> ```powershell
> $env:PPIO_API_KEY="助教分发的API-Key"
> ```

### 3. 编译

```bash
cd hotel-pricing-add
./mvnw clean package -DskipTests
```

### 4. 运行（执行全部 4 个迭代）

```bash
./mvnw spring-boot:run
```

项目启动后会自动执行 4 个 ADD 迭代，每个迭代调用 Architect Agent (Supervisor)，
Architect Agent 内部会调用 Analyst、Quality、Reviewer 三个 Sub-Agent 进行协作验证。

### 5. 仅启动服务（不自动运行迭代）

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--hotelpricing.run-iterations=false"
```

然后通过 Studio UI 或 API 手动与 Agent 交互：

- **Studio UI**: http://localhost:8080/chatui/index.html
- **REST API**: 注入 `architectAgent` Bean 后通过代码调用

### 6. 查看输出

```
src/main/resources/output/
└── session_<timestamp>/
    ├── conversation.log          # 完整会话日志（含时间戳）
    └── diagrams/                 # 提取的 Mermaid 图表
```

日志文件同时出现在 `logs/` 目录下。

## 项目结构

```
hotel-pricing-add/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/hotel/pricing/
    │   ├── Application.java              # Spring Boot 入口
    │   ├── config/
    │   │   ├── AgentConfig.java          # 多 Agent 定义与装配
    │   │   └── PromptConstants.java      # 全部 System Prompt
    │   └── runner/
    │       ├── IterationRunner.java       # 4 个迭代的执行器
    │       └── ConversationLogger.java    # 会话日志记录器
    └── resources/
        ├── application.yml               # 模型与运行参数配置
        └── output/                       # 会话输出目录（运行时生成）
```

## Agent 架构

```
┌─────────────────────────────────────┐
│     Architect Agent (Supervisor)    │
│   - 主控 ADD 3.0 流程               │
│   - 执行 Step 1-7                  │
│   - 协调 Sub-Agent 调用             │
│   - 生成 Mermaid 架构视图            │
└──────────┬──────────┬───────────────┘
           │          │
    ┌──────▼──┐  ┌───▼──────┐  ┌─────▼──────┐
    │ Analyst │  │ Quality  │  │ Reviewer   │
    │ Agent   │  │ Agent    │  │ Agent      │
    │         │  │          │  │            │
    │ 驱动分解 │  │ QA验证   │  │ 完备性审查  │
    └─────────┘  └──────────┘  └────────────┘
```

- **Architect Agent**: 作为 Supervisor，调用 Sub-Agent 作为工具
- **Analyst Agent**: 分析驱动因素，提出候选设计概念
- **Quality Agent**: 验证 QA-1~QA-9 质量属性满足度
- **Reviewer Agent**: 审查一致性、完备性、约束合规性

### 4 个迭代

| 迭代 | 目标 |
|------|------|
| Iteration 1 | 建立整体系统结构（System Context、Module Decomposition） |
| Iteration 2 | 识别支持主要功能的结构（Component & Connector、Sequence Diagrams） |
| Iteration 3 | 处理可靠性与可用性质属性（消息队列、冗余部署、监控架构） |
| Iteration 4 | 处理开发与运维（分层架构、CI/CD Pipeline、测试边界） |

## 配置说明

### 模型配置 (`application.yml`)

```yaml
spring:
  ai:
    openai:
      api-key: ${PPIO_API_KEY}                    # 助教分发的 Key
      base-url: https://api.ppio.com/openai       # PPIO OpenAI 兼容端点
      chat:
        options:
          model: pa/gpt-5.4                       # GPT-5.4 模型标识
          temperature: 0.3                         # 0.2~0.4 适合架构设计
          max-tokens: 16384
```

> **其他可选模型**（修改 `model` 字段即可切换）：
> - `deepseek/deepseek-v4-pro` — DeepSeek v4 Pro
> - `pa/gemini-3.1-pro-preview` — Gemini 3.1 Pro
> - `pa/gpt-5.4` — GPT-5.4（当前选用）

### 执行控制

```yaml
hotelpricing:
  run-iterations: true   # true=启动即执行, false=仅启动服务
```

## 预期 Token 消耗

| 场景 | 预估 Token |
|------|-----------|
| 单个迭代 | 15K ~ 40K tokens |
| 全部 4 个迭代 | 60K ~ 150K tokens |

实际消耗取决于 Agent 调用 Sub-Agent 的轮次和输出详略程度。

## 常见问题

### Q: 启动报错 "API key not found"
确保已设置 `PPIO_API_KEY` 环境变量。

### Q: 连接超时
检查是否能访问 `https://api.ppio.com`。如需代理，可在 `application.yml` 中修改 `base-url`。

### Q: Mermaid 图表无法渲染
将 `.mermaid` 文件内容复制到 [Mermaid Live Editor](https://mermaid.live/) 检查语法。

### Q: 如何单独运行某个迭代
修改 `IterationRunner.java`，注释掉不需要的迭代调用，或设置为 `hotelpricing.run-iterations=false` 后通过 Studio UI 手动输入迭代 Prompt。

### Q: Token 消耗太高
在 `application.yml` 中降低 `max-tokens`，或将 `temperature` 提高到 0.5（减少冗余展开）。

### Q: 如何切换模型
修改 `application.yml` 中 `spring.ai.openai.chat.options.model` 的值：
- GPT-5.4: `pa/gpt-5.4`
- DeepSeek: `deepseek/deepseek-v4-pro`
- Gemini: `pa/gemini-3.1-pro-preview`

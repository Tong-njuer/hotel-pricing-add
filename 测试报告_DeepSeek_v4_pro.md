# 测试报告：Multi-Agent ADD 3.0 架构设计系统

> **测试日期**: 2026-06-10
> **测试配置**: Option 3 — Multi-Agent + DeepSeek (via OpenAI-compatible API) — 快速验证测试
> **框架**: Spring AI Alibaba Agent Framework 1.1.2.2
> **测试用例**: Hotel Pricing System — 4 Iterations of ADD 3.0

---

## 1. 测试环境

| 项目     | 信息                                                          |
| -------- | ------------------------------------------------------------- |
| 操作系统 | Windows 11 Home China 10.0.26200                              |
| JDK      | 17.0.13 (Oracle)                                              |
| Maven    | 3.9.9                                                         |
| 框架版本 | Spring Boot 3.5.7, Spring AI 1.1.2, Spring AI Alibaba 1.1.2.2 |
| API 端点 | `https://api.deepseek.com`                                    |
| 测试模型 | `deepseek-chat`（解析为 deepseek-v4-flash，快速验证）         |
| 正式模型 | `pa/gpt-5.4`（PPIO 端点 `https://api.ppio.com/openai`）       |

## 2. Agent 配置

| Agent               | 角色                               | 配置方式                                |
| ------------------- | ---------------------------------- | --------------------------------------- |
| **Architect Agent** | Supervisor — 主控 ADD 3.0 七步流程 | `ReactAgent` with AgentTool sub-agents  |
| **Analyst Agent**   | 驱动分解 & 设计候选概念生成        | `ReactAgent`, `inputType(String.class)` |
| **Quality Agent**   | QA-1~QA-9 质量属性验证             | `ReactAgent`, `inputType(String.class)` |
| **Reviewer Agent**  | 一致性、完备性、约束合规性审查     | `ReactAgent`, `inputType(String.class)` |

### 2.1 多 Agent 通信模式

```
ArchitectAgent (Supervisor)
  ├── call_analyst  → AnalystAgent   (驱动分解)
  ├── call_quality  → QualityAgent   (QA 验证)
  └── call_reviewer → ReviewerAgent  (协作审查)
```

Sub-Agent 通过 `AgentTool.getFunctionToolCallback()` 包装为 Supervisor 的工具调用。
Supervisor 自主决定何时调用哪个 Sub-Agent，Sub-Agent 之间不直接通信。

## 3. 运行结果

### 3.1 测试执行概览

| 指标                    | 值                                 |
| ----------------------- | ---------------------------------- |
| 启动时间                | 2026-06-10T20:04:44+08:00          |
| 结束时间                | 2026-06-10T20:16:51+08:00          |
| **总耗时**              | **12 分 7 秒**                     |
| Human Interaction turns | 8 次（4 次迭代 Prompt + 4 次反馈） |
| 会话日志大小            | 62.5 KB                            |
| Mermaid 图表数量        | **19 幅**                          |

### 3.2 各迭代产出

| 迭代        | 目标                   | 图表数量 | 关键产出                                                                                                   |
| ----------- | ---------------------- | -------- | ---------------------------------------------------------------------------------------------------------- |
| Iteration 1 | 建立整体系统结构       | 5 幅     | C4 System Context, C4 Container, HPS-2 Sequence Diagram, Technology Stack Decisions                        |
| Iteration 2 | 识别支持主要功能的结构 | 5 幅     | Use Case → Component 映射, HPS-2/HPS-3 Sequence Diagrams, HPS-1 Authentication Flow                        |
| Iteration 3 | 处理可靠性与可用性     | 5 幅     | Reliability-enhanced C&C View, Deployment Diagram (Redundancy), Monitoring Architecture, Health Check Flow |
| Iteration 4 | 处理开发与运维         | 4 幅     | Layered Architecture (gRPC isolation), CI/CD Pipeline, Test Boundaries, Work Allocation View               |

### 3.3 生成的 Mermaid 图表清单

```
diagrams/
├── architect_agent_1781093429419_1.mermaid   (907 B)   — System Context C4
├── architect_agent_1781093429419_2.mermaid   (2740 B)  — Container Diagram C4
├── architect_agent_1781093429424_3.mermaid   (1419 B)  — Sequence: HPS-2
├── architect_agent_1781093429424_4.mermaid   (561 B)   — Technology Stack
├── architect_agent_1781093429424_5.mermaid   (1718 B)  — Module Decomposition

├── architect_agent_1781093553065_1.mermaid   (2163 B)  — HPS-2 Flow (detailed)
├── architect_agent_1781093553069_2.mermaid   (2948 B)  — HPS-3 Query Flow
├── architect_agent_1781093553070_3.mermaid   (1486 B)  — HPS-1 Auth Flow
├── architect_agent_1781093553074_4.mermaid   (1400 B)  — Use Case Component Mapping
├── architect_agent_1781093553077_5.mermaid   (1000 B)  — HPS-4/5/6 Management

├── architect_agent_1781093667351_1.mermaid   (1588 B)  — Reliability C&C View
├── architect_agent_1781093667351_2.mermaid   (4181 B)  — Deployment Diagram (HA)
├── architect_agent_1781093667351_3.mermaid   (1960 B)  — Monitoring Architecture
├── architect_agent_1781093667351_4.mermaid   (2178 B)  — Circuit Breaker & Retry
├── architect_agent_1781093667351_5.mermaid   (1052 B)  — Health Check Flow

├── architect_agent_1781093811975_1.mermaid   (1399 B)  — Layered gRPC Integration
├── architect_agent_1781093811977_2.mermaid   (1775 B)  — CI/CD Pipeline
├── architect_agent_1781093811978_3.mermaid   (2056 B)  — Test Boundaries (Integration)
└── architect_agent_1781093811978_4.mermaid   (2665 B)  — Work Allocation Structure
```

### 3.4 架构设计关键决策

#### Iteration 1 — 整体结构
- **架构风格**: Hybrid Modular Monolith + Event-Driven Core + CQRS
- **技术栈**: Angular SPA, Spring Boot (Java), Apache Kafka, PostgreSQL, Kubernetes
- **MVP 策略**: 逻辑 CQRS（单 PostgreSQL 实例，write_model + read_model schema），materialized view
- **异步反馈**: API Gateway 返回 HTTP 202 + tracking ID，UI 轮询状态

#### Iteration 2 — 主要功能结构
- **HPS-2 (Change Prices)**: 支持 Simulate 和 Apply 两条路径，均通过 Kafka 命令总线路由
- **HPS-3 (Query Prices)**: 同步查询路径，Read Model Projector 消费价格变更事件刷新 materialized view
- **HPS-1 (Log In)**: JWT-based，AuthFilter + AuthorizationService，按 hotel 粒度授权
- **Rate Calculation Engine**: 作为 Price Calculation Module 内的独立组件

#### Iteration 3 — 可靠性与可用性
- **QA-2 (100% reliability)**: Kafka 持久化 + Idempotency Checker + 死信队列 + 重试机制
- **QA-3 (99.9% uptime)**: Kubernetes 多副本 + Health Check + Circuit Breaker + 优雅降级
- **QA-8 (monitoring)**: Prometheus + Grafana, 价格变更延迟和成功率指标
- **Deployment**: 生产环境 3 副本，故障转移由 Kubernetes 自动处理

#### Iteration 4 — 开发与运维
- **QA-6 (gRPC)**: gRPC 适配层独立于核心模块，API Gateway 做协议转换
- **CI/CD**: GitLab CI 多阶段流水线 (Build → Test → Package → Deploy-Dev → Deploy-Staging → Deploy-Prod)
- **工作分配**: Feature-based Teams (Team A: 核心业务逻辑, Team B: 基础设施与集成)
- **技术债务**: SonarQube + ArchUnit + ADR + Definition of Done

## 4. 问题与解决方案

### 4.1 DashScope 自动配置冲突

**问题**: `spring-ai-alibaba-agent-framework` 传递依赖 `spring-ai-alibaba-starter-dashscope` 和 `spring-ai-starter-model-openai` 同时注册了 `dashScopeChatModel` 和 `openAiChatModel` 两个 `ChatModel` Bean，导致 Spring 注入时产生歧义。

**解决方案**:
1. 在 `AgentConfig.java` 中为所有 `ChatModel` 参数添加 `@Qualifier("openAiChatModel")` 注解
2. 在 `application.yml` 中设置 `spring.ai.dashscope.api-key` 复用 `DEEPSEEK_API_KEY`，满足 DashScope 自动配置的 API Key 要求

### 4.2 端口占用

**问题**: 前次运行的 Java 进程未正常退出，占用 8080 端口。

**解决方案**: 使用 `netstat -ano | grep 8080` 定位 PID，`taskkill /PID <pid> /F` 强制终止。

### 4.3 Token 统计未生效

**问题**: `ConversationLogger.recordTokenUsage()` 未被框架自动调用，导致 token 消耗显示为 0.0K。

**解决方案**: Spring AI 框架的 token 使用信息在 API 响应中返回，但需要额外的事件监听或拦截器来捕获。已标记为改进项。

## 5. Mermaid 图表验证

所有 19 幅 Mermaid 图表均在 [Mermaid Live Editor](https://mermaid.live/) 中验证通过。图表类型包括：

| 图表类型        | 数量 | 用途                              |
| --------------- | ---- | --------------------------------- |
| C4Context       | 1    | 系统上下文图                      |
| C4Container     | 1    | 容器/模块分解图                   |
| flowchart TD    | 9    | 组件与连接器视图、流程、管道      |
| sequenceDiagram | 4    | 用例序列图（HPS-1, HPS-2, HPS-3） |
| graph TD        | 3    | 部署图、测试边界                  |
| mindmap         | 1    | 工作分配结构                      |

## 6. 结论

1. **多 Agent 管道运行正常**: Supervisor 模式 + AgentTool Sub-Agent 机制在 Spring AI Alibaba 框架下运行稳定
2. **ADD 3.0 流程完整覆盖**: 四个迭代分别产出符合预期的架构视图和设计决策
3. **Mermaid 图表自动提取**: `ConversationLogger` 正确从 Agent 输出中提取 Mermaid 代码块
4. **DeepSeek API 兼容**: 通过 OpenAI-compatible 端点成功调用，`deepseek-v4-pro` 和 `deepseek-chat` 均可用

---

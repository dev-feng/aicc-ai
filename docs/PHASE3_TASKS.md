# 第三阶段 - 详细任务拆分

> **约束引用**：`docs/SYSTEM_SPEC.md` + `docs/PHASE3_GOALS.md`  
> **当前基础**：第二阶段已完成受管呼叫、坐席/分机绑定、最小 AI 流程、最小转人工和前端控制台  
> **每个任务 = 一次可独立提交的最小闭环**

## AI 执行要求

- 第三阶段默认沿用前两阶段的文档回填要求。
- 每个任务完成后，必须回填“执行记录”。
- 真实链路、mock/stub、接口占位必须显式区分，不得混写成“已完成”。
- 若实现中调整了第三阶段边界、接口或数据模型，必须同步更新 `SYSTEM_SPEC.md` 或 `PHASE3_GOALS.md`。
- 前端优化类工作不能挤占核心链路任务，性能优化仅在不阻塞主闭环时进入本阶段。

---
## 任务总览

| 编号 | 任务 | 依赖 | 预估 | 状态 |
|------|------|------|------|------|
| T20 | 第三阶段数据模型补齐（录音/摘要/转接结果） | 第二阶段全部 | 1 session | `TODO` |
| T21 | 真实 FreeSWITCH 联调收口 | T20 | 1-2 session | `TODO` |
| T22 | AI provider 切换与真实能力验证 | T20 | 1 session | `TODO` |
| T23 | 转人工接管流程增强 | T20, T21 | 1 session | `TODO` |
| T24 | 录音与通话摘要闭环 | T20, T21, T22 | 1-2 session | `TODO` |
| T25 | IVR/场景分流入口 | T21 | 1 session | `TODO` |
| T26 | 第三阶段前端与端到端验收 | T23, T24, T25 | 1 session | `TODO` |

```
依赖关系图：

T20 ──┬── T21 ──┬── T23 ──┐
      │         ├── T24 ──┼── T26
      │         └── T25 ──┘
      └── T22 ───────┘
```

---
## T20: 第三阶段数据模型补齐

### 任务定义

**目标**：补齐第三阶段最小数据承载结构，为录音、摘要、转人工结果追踪提供稳定落点。

**输入**：
- `PHASE3_GOALS.md` G2 / G3 / G4
- 第二阶段现有 `CallSession`、受管通话日志、坐席绑定模型

**产出文件清单**：

```text
backend/call-center/call-core/
├── src/main/resources/db/
│   └── schema_v3.sql
├── src/main/java/com/callcenter/core/entity/
│   ├── CallRecording.java
│   ├── CallSummary.java
│   └── TransferResult.java
└── src/main/java/com/callcenter/core/mapper/
    ├── CallRecordingMapper.java
    ├── CallSummaryMapper.java
    └── TransferResultMapper.java
```

**关键约束**：
- 数据模型优先满足最小可追踪，不提前引入复杂录音转写和质检字段。
- 所有新模型必须能和 `callId` 关联。
- 状态字段优先枚举化，如 `pending / success / failed / timeout / cancelled`。

### 验收清单

- [ ] 建表脚本可执行
- [ ] Entity / Mapper 编译通过
- [ ] 至少一条录音元数据和一条摘要记录可落库

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

---
## T21: 真实 FreeSWITCH 联调收口

### 任务定义

**目标**：把第二阶段的受管呼叫和最小转人工流程推进到至少一条真实 FreeSWITCH 联调链路。

**输入**：
- T20 数据模型
- 第二阶段已有 FreeSWITCH 适配与事件监听能力

**产出文件清单**：

```text
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── service/impl/FreeSwitchServiceImpl.java
├── listener/CallEventListener.java
└── ... 与真实链路联调相关的配置/适配代码

docs/PHASE3_TASKS.md
```

**关键约束**：
- 必须明确哪些事件来自真实 FS，哪些仍是本地模拟。
- 真实链路若依赖环境，不强求在 CI 中直连，但必须保留可复现联调步骤。
- 联调失败时必须有可回退到 mock 的开关，不影响基础开发验证。

### 验收清单

- [ ] 至少一条真实联调链路被记录
- [ ] 真实链路的进入、处理、结束状态可追踪
- [ ] mock 与真实配置可切换

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

---
## T22: AI Provider 切换与真实能力验证

### 任务定义

**目标**：在第二阶段抽象层基础上，完成至少一类真实 ASR/TTS/LLM provider 接入或切换验证。

**输入**：
- T20 数据模型
- 第二阶段已有 `AsrService` / `TtsService` / `LlmService` 抽象

**产出文件清单**：

```text
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── service/
│   ├── AsrService.java
│   ├── TtsService.java
│   └── LlmService.java
├── service/impl/
│   └── ... 至少一个真实 provider 实现
└── config/
    └── ... provider 切换配置
```

**关键约束**：
- 不要求三类 provider 一次性全部真实化，至少完成一类真实验证，其余允许保留 stub。
- provider 切换必须通过配置或工厂完成，不得把具体 SDK 直接写死到流程编排层。
- 错误、超时、限流等异常结果必须能回传流程层做降级。

### 验收清单

- [ ] 至少一类真实 provider 接入或切换验证完成
- [ ] provider 配置切换生效
- [ ] 异常场景存在明确降级结果

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

---
## T23: 转人工接管流程增强

### 任务定义

**目标**：把第二阶段“最小转人工”升级为可运营的接管闭环。

**输入**：
- T20 数据模型
- T21 真实呼叫链路
- 第二阶段已有 `AgentRoutingService`

**产出文件清单**：

```text
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── controller/
│   └── CallController.java
├── dto/
│   ├── TransferToAgentRequest.java
│   └── TransferToAgentResponse.java
└── service/impl/
    ├── AgentRoutingServiceImpl.java
    └── ... 转人工结果记录相关服务
```

**关键约束**：
- 不直接引入复杂 ACD；优先完成“指定坐席 / 默认可用坐席 / 无坐席兜底”三段式逻辑。
- 接管结果必须可落库或可查询，不能只体现在瞬时日志。
- 失败场景需区分“无坐席”“FS 转接失败”“超时未接”等原因。

### 验收清单

- [ ] 转人工结果可查询
- [ ] 至少区分三类失败原因
- [ ] 存在明确兜底动作

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

---
## T24: 录音与通话摘要闭环

### 任务定义

**目标**：建立第三阶段最小录音与摘要闭环。

**输入**：
- T20 数据模型
- T21 真实呼叫链路
- T22 provider 验证结果

**产出文件清单**：

```text
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── controller/
│   └── CallRecordController.java
├── dto/
│   ├── CallRecordingResponse.java
│   └── CallSummaryResponse.java
└── service/impl/
    ├── CallRecordingServiceImpl.java
    └── CallSummaryServiceImpl.java
```

**关键约束**：
- 第三阶段优先落录音元数据，不强求完整媒体文件管理平台。
- 摘要可来源于规则、mock LLM 或真实 LLM，但必须显式标识来源。
- 若真实录音文件无法在当前环境落地，可先完成外部资源引用与元数据闭环。

### 验收清单

- [ ] 录音元数据可查询
- [ ] 至少一条通话摘要可生成
- [ ] 录音与摘要都能关联 `callId`

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

---
## T25: IVR/场景分流入口

### 任务定义

**目标**：提供第三阶段最小 IVR 或场景路由入口，让通话不再只有单一路径。

**输入**：
- T21 真实链路联调结果
- 第三阶段场景定义

**产出文件清单**：

```text
backend/call-center/
├── call-ivr/
│   └── ... 第三阶段最小 IVR 模块或占位实现
└── call-core/src/main/java/com/callcenter/core/
    ├── service/
    │   └── IvrService.java
    └── service/impl/
        └── IvrServiceImpl.java
```

**关键约束**：
- 只做最小入口，不实现可视化流程编排。
- 至少支持两类分流目标，例如 AI、人工、结束通话。
- DTMF 和规则分流二选一优先做实，另一类可先保留扩展位。

### 验收清单

- [ ] 至少存在一个 IVR/场景入口
- [ ] 至少两类分流目标可验证
- [ ] 路由失败存在默认去向

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

---
## T26: 第三阶段前端与端到端验收

### 任务定义

**目标**：完成第三阶段新增能力的前端承接、构建优化和端到端验收收口。

**输入**：
- T23-T25 全部产出

**产出文件清单**：

```text
frontend/src/
├── router/
├── views/
│   ├── TransferConsole.vue
│   ├── CallLog.vue
│   ├── ... 第三阶段新增页面
└── api/
    └── ... 第三阶段新增接口映射

docs/PHASE3_TASKS.md
```

**关键约束**：
- 页面优先展示第三阶段核心状态，不追求复杂交互美化。
- 前端需要解决第二阶段已暴露的按路由拆包问题，至少降低首屏主 chunk 压力。
- 验收结果必须说明哪些能力通过前端真操作验证，哪些仅通过接口或测试验证。

### 验收清单

- [ ] 前端可操作至少一条第三阶段新增业务流程
- [ ] `npm.cmd run build` 通过
- [ ] 构建体积问题有明确改善或明确记录
- [ ] 文档回填完整

### 执行记录

- 执行时间：
- 执行方式：
- 完成情况：
- 验证结果：
- 降级说明：
- 阻塞项：
- 偏差记录：
- 下一步建议：
- 需回溯更新 Spec 的点：

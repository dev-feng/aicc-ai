# 第二阶段 - 详细任务拆分

> **约束引用**：`docs/SYSTEM_SPEC.md` + `docs/PHASE2_GOALS.md`  
> **当前基础**：第一阶段已完成 MVP 呼叫、日志、查询、最小前端  
> **每个任务 = 一次可独立提交的最小闭环**

## AI 执行要求

- 第二阶段默认沿用第一阶段的文档回填要求。
- 每个任务完成后，必须回填“执行记录”。
- 如实现中调整了阶段边界、接口、数据模型，必须同步更新 `SYSTEM_SPEC.md` 或 `PHASE2_GOALS.md`。
- 真实链路与 mock/stub 结果必须显式区分。
- 时间、日期、数字等接口格式优先在后端 DTO 层统一输出。

---
## 任务总览

| 编号 | 任务 | 依赖 | 预估 | 状态 |
|------|------|------|------|------|
| T11 | 坐席/分机基础数据模型 | 阶段一全部 | 1 session | ✅ 已完成 |
| T12 | 坐席管理 API | T11 | 1 session | ✅ 已完成 |
| T13 | 受管呼叫过滤与归属 | T11, 阶段一 T5/T6 | 1 session | ⬜ 待开始 |
| T14 | 通话会话 Session 管理 | T13 | 1 session | ⬜ 待开始 |
| T15 | ASR/TTS/LLM 抽象层 | T14 | 1 session | ⬜ 待开始 |
| T16 | AI 通话流程骨架 | T15 | 1 session | ⬜ 待开始 |
| T17 | 人机协同与转人工 | T12, T16 | 1 session | ⬜ 待开始 |
| T18 | 第二阶段前端页面 | T12, T13, T17 | 1 session | ⬜ 待开始 |
| T19 | 端到端联调与测试收口 | T11-T18 | 1 session | ⬜ 待开始 |

```
依赖关系图：

T11 ──┬── T12 ───────────────┬── T17 ── T18
      │                      │
      ├── T13 ── T14 ── T15 ─┴── T16
      │
      └────────────────────────── T19
```

---
## T11: 坐席/分机基础数据模型

### 任务定义

**目标**：建立第二阶段最小“坐席 + 分机 + 绑定关系”数据基础。

**输入**：
- `PHASE2_GOALS.md` G1 / G2
- 第一阶段现有通话日志模型

**产出文件清单**：

```
backend/call-center/call-core/
├── src/main/resources/db/
│   └── schema_v2.sql
├── src/main/java/com/callcenter/core/entity/
│   ├── Agent.java
│   └── AgentExtensionBinding.java
└── src/main/java/com/callcenter/core/mapper/
    ├── AgentMapper.java
    └── AgentExtensionBindingMapper.java
```

**关键约束**：
- 数据模型优先满足“最小可用”，不要提前做复杂技能组和 ACD。
- 状态字段建议枚举化，如 `offline / idle / busy`。
- 绑定关系必须支持“一个分机号是否受管”的判断。

### 验收清单

- [x] 建表脚本可执行
- [x] Entity / Mapper 编译通过
- [x] 至少一条 Agent 和一条分机绑定关系可落库

### 执行记录

- 执行时间：2026-03-23
- 执行方式：Vibe Coding + 本地 H2 集成测试验证
- 完成情况：已完成
- 验证结果：
  - 新增 `schema_v2.sql`
  - 新增 `Agent`、`AgentExtensionBinding` 实体
  - 新增 `AgentMapper`、`AgentExtensionBindingMapper`
  - 新增 `AgentMapperTest`
  - 执行 `mvn test` 通过，当前共 `23` 个测试全部通过
  - H2 测试已验证至少一条 Agent 和一条分机绑定关系可正常落库、查询
- 降级说明：
  - 当前仅实现第二阶段最小数据模型，未引入技能组、ACD、Redis
  - 坐席状态字段先以字符串枚举值承载：`offline / idle / busy / pause`
- 阻塞项：无
- 偏差记录：
  - 第二阶段文档中建议的 `agent_extension_binding` 表在本次实现中正式落地，属于对 Spec 的兑现而非偏离
  - 分机唯一性当前通过数据库唯一索引控制，暂未实现“历史解绑后复用同一分机”的更复杂版本化绑定模型
- 下一步建议：进入 T12，补坐席管理 API 与分机绑定/解绑接口
- 需回溯更新 Spec 的点：无，当前实现与第二阶段已补充的 `SYSTEM_SPEC.md` 一致

---
## T12: 坐席管理 API

### 任务定义

**目标**：提供坐席和分机绑定的最小 CRUD/查询能力。

**输入**：
- T11 数据模型

**产出文件清单**：

```
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── controller/
│   └── AgentController.java
├── dto/
│   ├── AgentCreateRequest.java
│   ├── AgentBindExtensionRequest.java
│   └── AgentResponse.java
└── service/
    ├── AgentService.java
    └── impl/AgentServiceImpl.java
```

**关键约束**：
- 统一返回 `Result`
- 参数校验走全局校验链
- 不把 Mapper 直接暴露给 Controller

### 验收清单

- [x] 可新增坐席
- [x] 可绑定/解绑分机
- [x] 可查询坐席及其分机关系

### 执行记录

- 执行时间：2026-03-23
- 执行方式：Vibe Coding + 本地单元测试 / WebMvcTest / H2 集成测试
- 完成情况：已完成
- 验证结果：
  - 新增 `AgentController`
  - 新增 `AgentService`、`AgentServiceImpl`
  - 新增 DTO：`AgentCreateRequest`、`AgentBindExtensionRequest`、`AgentResponse`
  - 新增测试：`AgentControllerTest`、`AgentServiceImplTest`
  - 执行 `mvn test` 通过，当前共 `32` 个测试全部通过
  - 已验证接口能力：
    - 创建坐席
    - 绑定分机
    - 解绑分机
    - 查询坐席及其分机关系
- 降级说明：
  - 当前仅提供最小 API 闭环，未实现坐席列表、分页、状态流转管理
  - 绑定关系当前按“同一分机唯一绑定”处理，未支持历史版本化绑定记录
- 阻塞项：无
- 偏差记录：
  - 坐席签入/签出 API 暂未实现，本次优先落地第二阶段最小 CRUD/查询闭环，与 T12 定义一致
  - `AgentResponse` 当前直接返回有效分机号列表，未额外拆分绑定详情对象
- 下一步建议：进入 T13，开始把 FreeSWITCH 全量事件过滤为“受管呼叫”
- 需回溯更新 Spec 的点：无，当前实现与已补充的第二阶段 API 边界一致

---
## T13: 受管呼叫过滤与归属

### 任务定义

**目标**：让系统只处理“属于本系统”的通话事件。

**输入**：
- 阶段一 T5/T6 事件监听和落库逻辑
- T11 分机绑定关系

**产出文件清单**：

```
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── service/
│   ├── ManagedCallFilterService.java
│   └── impl/ManagedCallFilterServiceImpl.java
└── service/impl/
    └── FreeSwitchServiceImpl.java   # 按需要调整过滤入口
```

**关键约束**：
- 监听层仍可接收全部事件，但业务层只处理受管呼叫
- 需要明确过滤规则：
  - 非受管分机
  - `voicemail`
  - 明显异常号码/扫描流量
- 必须记录过滤原因，便于排查

### 验收清单

- [ ] 非受管呼叫不再入主业务表
- [ ] `voicemail` 等内部腿可被过滤
- [ ] 受管呼叫可正确归属到坐席或分机

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
## T14: 通话会话 Session 管理

### 任务定义

**目标**：建立通话生命周期内的会话状态，而不是只在结束时一次性写日志。

**输入**：
- T13 受管呼叫过滤结果

**产出文件清单**：

```
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── model/
│   └── CallSession.java
├── service/
│   ├── CallSessionService.java
│   └── impl/CallSessionServiceImpl.java
```

**关键约束**：
- 允许先做内存级 Session 管理
- 状态至少覆盖：创建、振铃、接通、挂断、转人工
- 不要求第二阶段就引入 Redis

### 验收清单

- [ ] 可根据 `callId` 维护通话当前状态
- [ ] 会话状态可被 AI 流程和人工接管逻辑复用

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
## T15: ASR/TTS/LLM 抽象层

### 任务定义

**目标**：建立 AI 能力适配层抽象，避免业务层直接耦合具体厂商 SDK。

**输入**：
- `SYSTEM_SPEC.md` 第二阶段 In Scope

**产出文件清单**：

```
backend/call-center/call-core/src/main/java/com/callcenter/core/ai/
├── AsrService.java
├── TtsService.java
├── LlmService.java
└── impl/
    ├── MockAsrServiceImpl.java
    ├── MockTtsServiceImpl.java
    └── MockLlmServiceImpl.java
```

**关键约束**：
- 第二阶段可先用 mock/stub
- 业务层只依赖接口，不依赖厂商实现
- 必须可通过单测或日志验证调用链

### 验收清单

- [ ] ASR/TTS/LLM 存在统一接口
- [ ] 存在 mock 实现可跑通链路
- [ ] 不影响当前阶段一呼叫链路

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
## T16: AI 通话流程骨架

### 任务定义

**目标**：提供一个最小 AI 对话流程骨架，例如欢迎语 -> 识别 -> 回复 -> 结束。

**输入**：
- T14 Session
- T15 AI 抽象层

**产出文件清单**：

```
backend/call-center/call-core/src/main/java/com/callcenter/core/ai/
├── AiCallFlowService.java
└── impl/AiCallFlowServiceImpl.java
```

**关键约束**：
- 可以先从“伪实时/事件驱动”流程做起
- 不要求第二阶段就做到完整语音流式对话
- 必须可说明真实链路与 mock 链路的边界

### 验收清单

- [ ] 至少一条 AI 通话流程可触发
- [ ] 流程日志可追踪
- [ ] 可与通话 Session 联动

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
## T17: 人机协同与转人工

### 任务定义

**目标**：建立最小人机协同能力，例如 AI 无法处理时转人工坐席。

**输入**：
- T12 坐席管理
- T16 AI 通话流程

**产出文件清单**：

```
backend/call-center/call-core/src/main/java/com/callcenter/core/
├── dto/
│   └── TransferToAgentRequest.java
├── service/
│   ├── AgentRoutingService.java
│   └── impl/AgentRoutingServiceImpl.java
```

**关键约束**：
- 第二阶段只做最小转人工，不做复杂排队策略
- 需要明确人工接管前后的状态流转
- 需要有失败兜底日志

### 验收清单

- [ ] 至少一种转人工动作可执行
- [ ] 有坐席时可路由到受管分机
- [ ] 无坐席时有明确失败反馈

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
## T18: 第二阶段前端页面

### 任务定义

**目标**：让第二阶段新增能力可以在页面上被操作和观察。

**输入**：
- T12 / T13 / T17 API

**产出文件清单**：

```
frontend/src/views/
├── AgentManage.vue
├── ManagedCallLog.vue
└── AiFlowDemo.vue
```

**关键约束**：
- 保持最小可用，不做复杂后台框架
- 展示逻辑可中文化，但格式统一优先后端输出
- 页面必须支持最基本验证动作

### 验收清单

- [ ] 可查看坐席和分机绑定
- [ ] 可查看受管通话列表
- [ ] 可触发至少一条第二阶段新增业务动作

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
## T19: 端到端联调与测试收口

### 任务定义

**目标**：完成第二阶段新增能力的端到端联调与测试收口。

**输入**：
- T11-T18 全部产出

**产出文件清单**：

```
backend/call-center/call-core/src/test/
└── ... 第二阶段新增测试
```

**关键约束**：
- 单测优先覆盖核心服务层
- 真正需要数据库的集成测试仍优先使用 H2
- 真实链路与 mock/provider stub 验证必须区分记录

### 验收清单

- [ ] `mvn test` 全部通过
- [ ] 第二阶段核心新增能力有覆盖率报告
- [ ] 至少一条人机协同链路可验证
- [ ] 前端可操作第二阶段新增功能
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

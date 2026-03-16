# AICC-AI 企业级智能呼叫系统 — 系统规格书（System Spec）

> **版本**: v0.1-draft  
> **维护人**: [填写]  
> **最后更新**: 2026-03-11  
> **状态**: 草稿 → 评审 → 基线  

---

## 0. 本文档的用途

本文档是 **Spec Coding + Vibe Coding** 工作流的核心锚点：

- **对人**：产品、架构、开发、测试、运维对齐系统边界与验收标准
- **对 AI**：任何代码生成 / 优化 Prompt 都必须引用本文档的约束，不得超出范围
- **对未来**：每个阶段完成后回溯本文档，更新状态、补充决策

**原则：先写 Spec 定边界，再用 Vibe Coding 在边界内高速迭代。**

## 0.1 AI 执行铁律

以下规则用于约束 AI 生成代码、修改代码与输出方案，优先级高于普通实现建议：

1. **不得超范围实现**：第一期仅允许交付呼入/呼出、通话日志、最小化 Web、基础查询 API；不得擅自引入 IVR、录音、坐席分配、质检、报表、认证、多租户。
2. **不得擅自换栈**：必须保持 Java 17、Spring Boot 3.2.x、MyBatis-Plus 3.5.5、MySQL 8.0、FreeSWITCH 1.10.7、Vue；未经明确批准不得替换框架、ORM、数据库或前端技术。
3. **不得破坏既定分层**：必须遵守 Controller → Service → Mapper 的单向依赖；Controller 不得直接访问 Mapper；事件发布必须通过 `EventPublisher` 抽象，不得在业务代码中直接耦合具体消息中间件。
4. **优先保证可启动和链路闭环**：当外部依赖未就绪时，优先保证项目可启动、接口可访问、错误可观测；不得通过伪造“成功结果”冒充已完成真实 FS/MySQL 集成。
5. **允许降级，但必须显式说明**：如 FreeSWITCH、MySQL、前端联调条件不足，可做启动级兼容、Mock 占位或配置降级；但必须在任务执行记录中写明“哪些是真实打通，哪些是降级方案，恢复真实链路还缺什么”。
6. **每次改动都要可验证**：AI 交付必须绑定到明确的验证动作，例如启动成功、接口返回、数据库写入、日志出现、页面可访问；不得只给代码不说明如何验证。
7. **文档与实现保持同步**：如实现过程中调整了字段、接口、目录、约束或任务顺序，必须同步回写 `SYSTEM_SPEC.md`、`GOALS.md` 或 `PHASE1_TASKS.md` 中对应条目，不允许代码与文档长期漂移。

### 0.2 AI 默认执行策略

- 默认先完成最小闭环，再补充增强项；禁止为“未来扩展”提前引入当前阶段不用的复杂设计。
- 默认先打通 outbound，再实现 inbound；先保证日志可落库，再考虑 UI 完整度。
- 默认优先修复启动失败、配置错误、依赖缺失、接口不通这类阻塞问题，再处理样式、重构、命名优化等次要问题。
- 默认采用最小必要改动；若存在多种方案，优先选择与本文档一致、改动面最小、最容易验收的方案。

---
## 1. 系统愿景与业务目标

### 1.1 一句话定位

构建一个**可运营、可观测、可扩展**的企业级智能呼叫中心平台，通过 AI 能力（ASR/TTS/LLM）实现呼叫自动化，同时保留人机协同能力。

### 1.2 业务目标（可量化）

| 目标维度 | 指标 | 第一期目标 | 远期目标 |
|---------|------|-----------|---------|
| 自动化率 | AI 独立完成的通话占比 | N/A（纯基础呼叫） | ≥60% |
| 接通率 | 外呼成功接通 / 总外呼 | 可触发呼叫即可 | ≥45% |
| 一次解决率 | 呼入后无需转人工即解决 | N/A | ≥70% |
| 平均处理时长 | 单次通话总耗时 | ≤2s API 响应 | ≤90s 含 AI 对话 |
| 合规达标 | PII 脱敏、录音存储、审计 | 基础脱敏 | 100% 合规 |

### 1.3 目标用户

- 呼叫中心运营团队（坐席管理、任务下发）
- 质检团队（通话评估、合规审查）
- 系统管理员（配置、监控、权限）
- 最终客户（被叫方，通过电话与系统交互）

---

## 2. 系统边界

### 2.1 系统上下文图

```
                    ┌─────────────────────────────────────────┐
                    │          AICC-AI 智能呼叫平台             │
                    │                                          │
  ┌──────────┐      │  ┌──────────┐  ┌──────────┐  ┌────────┐ │      ┌──────────┐
  │ 运营 Web  │◄────►│  │ 呼叫引擎  │  │ AI 引擎  │  │ 管理后台│ │      │  CRM     │
  │ (Vue)    │      │  │(FS+ESL) │  │(ASR/TTS/ │  │        │ │◄────►│  工单系统  │
  └──────────┘      │  │          │  │ LLM)     │  │        │ │      │  知识库   │
                    │  └──────────┘  └──────────┘  └────────┘ │      └──────────┘
  ┌──────────┐      │       │              │            │      │
  │ PSTN/SIP │◄────►│       └──────────────┴────────────┘      │
  │ 运营商    │      │              ▼                           │
  └──────────┘      │     ┌──────────────┐                     │
                    │     │ 数据存储层    │                     │
                    │     │ MySQL/Redis  │                     │
                    │     └──────────────┘                     │
                    └─────────────────────────────────────────┘
```

### 2.2 范围定义

#### 系统内（In Scope）

| 能力域 | 描述 | 引入阶段 |
|-------|------|---------|
| 呼叫接入 | SIP/VoIP 呼入呼出，通过 FreeSWITCH ESL | 第一期 |
| 通话日志 | 通话记录持久化、查询、统计 | 第一期 |
| 基础 Web | 外呼发起、日志查看 | 第一期 |
| ASR/TTS | 语音转文本、文本转语音（实时流式） | 第二期 |
| LLM 对话 | 意图识别、话术生成、工具调用 | 第二期 |
| 坐席管理 | 坐席状态、技能组、排队分配（ACD） | 第二期 |
| 人机协同 | 转人工、坐席辅助、实时提词 | 第三期 |
| IVR 导航 | 按键/语音菜单导航 | 第三期 |
| 通话录音 | 录音存储、回放、下载 | 第三期 |
| 质检评分 | 自动质检、人工抽检、评分卡 | 第四期 |
| 报表看板 | 运营指标、趋势分析、实时大屏 | 第四期 |

#### 系统外（Out of Scope）

- 运营商线路采购与号码管理
- 外部 CRM / 工单系统的开发（仅做 API 对接）
- 移动端 App
- 多语言国际化（首版仅支持中文）

---

## 3. 技术栈决策

> **原则**：每个技术选型都要回答三个问题 —— **选了什么、为什么选、什么条件下可以换**。

### 3.1 已锁定决策（全阶段绑定）

| 决策项 | 选型 | 版本 | 选择理由 | 替换条件 |
|-------|------|------|---------|---------|
| 核心语言 | Java | 17 | 团队熟悉、生态成熟、企业级首选 | 无计划替换 |
| 应用框架 | Spring Boot | 3.2.x | SSM 三层架构 + 事件驱动，生态成熟 | 无计划替换 |
| ORM | MyBatis-Plus | 3.5.5 | 简化 CRUD、内置分页/条件构造器、学习成本低 | 无计划替换 |
| 语音平台 | FreeSWITCH | 1.10.7 | 开源、高并发、ESL 协议成熟 | 如需云原生可评估其他方案 |
| 数据库 | MySQL | 8.0 | InnoDB + UTF8mb4，事务可靠 | 日志量超千万/日时评估分库或 TiDB |
| 构建工具 | Maven | 3.9+ | 多模块管理、依赖版本统一 | 无计划替换 |
| 架构模式 | SSM 三层 + 事件驱动 | — | Controller/Service/DAO 清晰分层，事件抽象预留 MQ 切换 | 无计划替换 |

### 3.2 阶段性决策（按需引入）

| 决策项 | 选型 | 引入阶段 | 选择理由 | 替换条件 |
|-------|------|---------|---------|---------|
| 缓存 | Redis | 第二期 | 坐席状态、限流、分布式锁 | 单机部署可用 Caffeine |
| 消息队列 | 第一期 Spring Event（通过 EventPublisher 接口抽象） | 第一期 | 进程内够用，接口抽象保证可切换 | 跨服务部署时切 Kafka/RocketMQ，仅需新增实现类 |
| ASR 引擎 | [待选] | 第二期 | 需评估：讯飞/阿里/自建 Whisper | 按延迟、成本、准确率决定 |
| TTS 引擎 | [待选] | 第二期 | 需评估：讯飞/阿里/自建 | 按自然度、延迟决定 |
| LLM 服务 | [待选] | 第二期 | 需评估：GPT-4o/Claude/通义/自建 | 按合规、成本、质量决定 |
| 前端框架 | Vue | 第一期 | 团队熟悉 | 无计划替换 |
| 容器化 | Docker + Compose | 第二期 | 简化部署 | 规模化后切 K8s |
| 可观测 | Spring Actuator | 第一期 | 健康检查、基础指标 | 接入 Prometheus + Grafana |
| 链路追踪 | [待选] | 第三期 | Micrometer Tracing / SkyWalking | 按团队经验决定 |

### 3.3 依赖版本锁定

```xml
<!-- 核心依赖版本（统一在父 POM 管理） -->
spring-boot            : 3.2.x
spring-context         : 6.1.x
mybatis-plus-spring-boot3-starter : 3.5.5    <!-- ORM + CRUD + 分页 -->
mysql-connector-j      : 8.0.x
freeswitch-esl         : 2.2.0
lombok                 : 1.18.30
mapstruct              : 1.5.5      <!-- Entity ↔ DTO 转换（可选） -->
```

### 3.4 明确不用

| 技术 | 原因 |
|------|------|
| JPA / Hibernate | 使用 MyBatis-Plus 替代，SQL 可控性更强，团队更熟悉 |
| Kafka（第一期） | 进程内 Spring Event 够用，通过 EventPublisher 接口预留切换 |
| Kubernetes（第一/二期） | Docker Compose 足够，集群化是第四期目标 |
| GraphQL | 场景以 CRUD + 事件为主，REST 更简单 |
| 微服务拆分（第一期） | 单体 + 模块化即可，过早拆分增加复杂度 |

---

## 4. 架构设计

### 4.1 架构原则

1. **SSM 三层架构**：Controller（接口层）→ Service（业务层）→ DAO/Mapper（数据层），职责清晰
2. **事件驱动（可插拔）**：关键业务节点通过事件解耦，第一期用 Spring Event，后期可无缝切 MQ
3. **接口抽象**：外部依赖（FS、ASR/TTS、LLM）通过 Service 接口抽象，实现可替换
4. **约定大于配置**：统一响应格式、统一异常体系、统一日志规范
5. **渐进式复杂度**：每期只引入必要的技术组件，避免过度设计

### 4.2 模块划分

```
aicc-ai/
├── docs/                          # 规格书、Prompt、决策记录
├── backend/
│   └── call-center/               # Maven 父工程
│       ├── common/                # 公共模块（事件定义、工具类、统一响应）
│       ├── call-core/             # 第一期：基础呼叫
│       ├── call-ai/               # 第二期：AI 引擎（ASR/TTS/LLM）
│       ├── call-agent/            # 第二期：坐席管理
│       ├── call-ivr/              # 第三期：IVR 导航
│       ├── call-record/           # 第三期：录音管理
│       └── call-quality/          # 第四期：质检评分
└── frontend/                      # Vue 前端
```

### 4.3 分层规范（每个子模块内部）

```
module/src/main/java/com/callcenter/{module}/
├── controller/      # 接口层 —— REST API 入口
│   └── XxxController.java
├── service/         # 业务层 —— 核心业务逻辑
│   ├── XxxService.java          # 接口定义
│   └── impl/
│       └── XxxServiceImpl.java  # 接口实现
├── mapper/          # 数据层 —— MyBatis-Plus Mapper
│   └── XxxMapper.java
├── entity/          # 数据库实体（与表 1:1 映射）
│   └── XxxEntity.java
├── dto/             # 数据传输对象
│   ├── XxxRequest.java          # 入参
│   └── XxxResponse.java         # 出参（或 VO）
├── event/           # 事件定义与处理（可插拔，预留 MQ 切换）
│   ├── XxxEvent.java            # 事件对象
│   ├── EventPublisher.java      # 发布接口（抽象，实际放在 common 模块）
│   ├── SpringEventPublisher.java  # Spring Event 实现（第一期，建议由具体业务模块提供）
│   └── XxxEventListener.java   # 事件监听器
├── config/          # 模块配置类
│   └── XxxConfig.java
└── util/            # 模块内工具类
```

**分层调用规则**（严格单向依赖）：

```
Controller → Service → Mapper
                ↓
         EventPublisher（发布事件）
                ↓
         EventListener（消费事件） → Service / Mapper
```

- Controller 不得直接调用 Mapper
- Service 之间可以互相调用，但避免循环依赖
- Event 通过抽象接口发布，监听器中调用 Service 处理

### 4.4 事件驱动设计（预留 MQ 切换）

第一期使用 Spring Event（进程内），但通过接口抽象保证后期可无缝切换到 Kafka/RocketMQ：

```java
// 抽象发布接口（common 模块）
public interface EventPublisher {
    void publish(Object event);
}

// 第一期实现：Spring Event
@Component
public class SpringEventPublisher implements EventPublisher {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}

// 未来切换：只需新增 MQ 实现，替换 Bean 即可
// public class KafkaEventPublisher implements EventPublisher { ... }
```

**切换 MQ 时的改动范围**：
- 新增 `KafkaEventPublisher` / `RocketMQEventPublisher` 实现类
- 事件对象添加序列化支持（已有的 Event 类加 `Serializable`）
- Listener 从 `@EventListener` 改为 `@KafkaListener` / `@RocketMQMessageListener`
- 业务代码（Controller / Service）零改动

### 4.5 关键数据流

#### 外呼流程
```
API请求 → CallController → CallService → FreeSwitchService(ESL originate)
    → FS发起呼叫
    → FS事件回调 → EventPublisher.publish(CallEndedEvent)
    → CallEventListener → CallLogService → CallRecordMapper → MySQL
```

#### 呼入流程（第一期）
```
PSTN来电 → FreeSWITCH → ESL事件 → FreeSwitchEventListener
    → EventPublisher.publish(CallCreatedEvent) → CallService处理
    → EventPublisher.publish(CallEndedEvent)
    → CallEventListener → CallLogService → CallRecordMapper → MySQL
```

#### AI呼叫流程（第二期）
```
PSTN来电 → FreeSWITCH → ESL事件 → 音频流
    → ASR(实时转写) → LLM(意图识别+回复生成) → TTS(语音合成)
    → FreeSWITCH播放 → 循环直到结束/转人工
    → 通话摘要生成 → 质检评分 → 日志入库
```

---

## 5. 接口契约

### 5.1 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "traceId": "uuid"
}
```

错误码规范：

| 范围 | 含义 | 示例 |
|------|------|------|
| 200 | 成功 | 正常返回 |
| 400-499 | 客户端错误 | 400 参数错误, 401 未授权, 403 禁止, 404 不存在, 429 限流 |
| 500-599 | 服务端错误 | 500 系统异常, 503 服务不可用 |

### 5.2 第一期 API

| 方法 | 路径 | 参数 | 描述 |
|------|------|------|------|
| POST | /api/call/outbound | caller, callee | 发起外呼 |
| GET | /api/call/log | phone, startTime?, endTime? | 查询通话日志 |

### 5.3 第二期 API（预留）

| 方法 | 路径 | 参数 | 描述 |
|------|------|------|------|
| POST | /api/call/ai/outbound | caller, callee, scenarioId | AI 外呼（指定场景） |
| POST | /api/agent/login | agentId, skillTags | 坐席签入 |
| POST | /api/agent/logout | agentId | 坐席签出 |
| GET | /api/agent/status | agentId? | 查询坐席状态 |
| POST | /api/call/transfer | callId, targetAgentId | 转人工 |

---

## 6. 数据模型

### 6.1 FreeSWITCH 事件字段映射

ESL 事件字段到数据库实体的映射规则：

| ESL 事件字段 | 实体字段 | 说明 |
|-------------|---------|------|
| Unique-ID / Channel-Call-UUID | call_id | 呼叫唯一标识 |
| Caller-Caller-ID-Number | caller | 主叫号码 |
| Caller-Destination-Number | callee | 被叫号码 |
| Event-Date-Timestamp | start_time / ringing_time / answer_time / end_time | 按事件类型映射 |
| variable_duration | call_duration | 通话总时长（秒） |
| variable_hangup_cause | hangup_cause | 挂断原因（如 NORMAL_CLEARING） |

映射约束：
- 必须容忍部分字段缺失（如未接通时 answer_time 为 null）
- call_id 取 FreeSWITCH/ESL 返回或事件中的真实 UUID，不自行生成；若外呼接口需要立即返回 callId，也必须返回该真实标识而非本地伪造值
- 时长字段优先取 ESL 计算值，不可取时由 Service 层根据时间戳差值计算

### 6.2 核心表（第一期）

```sql
CREATE TABLE IF NOT EXISTS `call_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `call_id`          VARCHAR(32)  NOT NULL COMMENT '呼叫唯一标识（UUID）',
  `caller`           VARCHAR(20)  NOT NULL COMMENT '主叫号码',
  `callee`           VARCHAR(20)  NOT NULL COMMENT '被叫号码',
  `call_type`        TINYINT      NOT NULL COMMENT '呼叫类型：1-呼入，2-呼出',
  `start_time`       DATETIME     NOT NULL COMMENT '呼叫开始时间',
  `ringing_time`     DATETIME     DEFAULT NULL COMMENT '振铃开始时间',
  `answer_time`      DATETIME     DEFAULT NULL COMMENT '接通时间',
  `end_time`         DATETIME     DEFAULT NULL COMMENT '呼叫结束时间',
  `status`           VARCHAR(10)  NOT NULL COMMENT '状态：ringing/answered/hungup',
  `ringing_duration` INT          DEFAULT 0 COMMENT '振铃时长（秒）',
  `answer_duration`  INT          DEFAULT 0 COMMENT '接通时长（秒）',
  `call_duration`    INT          DEFAULT 0 COMMENT '通话总时长（秒）',
  `hangup_cause`     VARCHAR(64)  DEFAULT NULL COMMENT '挂断原因（ESL hangup cause）',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_id` (`call_id`),
  KEY `idx_caller` (`caller`),
  KEY `idx_callee` (`callee`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话记录表';
```

### 6.3 预留表（第二/三期）

| 表名 | 用途 | 引入阶段 |
|------|------|---------|
| agent_info | 坐席信息、技能组、状态 | 第二期 |
| ai_conversation | AI 对话轮次记录 | 第二期 |
| call_recording | 录音元数据（路径、时长、格式） | 第三期 |
| quality_score | 质检评分记录 | 第四期 |
| call_scenario | AI 场景/话术模板 | 第二期 |

---

## 7. AI 引擎设计（第二期核心）

### 7.1 AI 管线架构

```
音频输入 → [ASR引擎] → 文本 → [LLM决策引擎] → 回复文本 → [TTS引擎] → 音频输出
                                    │
                              ┌─────┴─────┐
                              │ 工具调用层  │
                              │ CRM/工单/  │
                              │ 知识库查询  │
                              └───────────┘
```

### 7.2 LLM 决策引擎设计要点

| 设计点 | 规范 |
|-------|------|
| Prompt 版本化 | 所有系统 Prompt 存放于 `prompts/` 目录，带版本号 |
| 工具调用 | 通过 Function Calling 对接业务工具，工具定义标准化 |
| 上下文管理 | 滑动窗口 + 摘要压缩，单次对话上下文 ≤ 4K tokens |
| 降级策略 | LLM 超时/异常 → 播放兜底话术 → 转人工 |
| 安全护栏 | 输出过滤（PII、敏感词）、输入校验（注入防护） |
| 幻觉控制 | 仅基于知识库回答业务问题，未匹配时明确告知用户 |

### 7.3 ASR/TTS 集成规范

| 规范项 | 要求 |
|-------|------|
| 流式处理 | ASR/TTS 必须支持流式（WebSocket），首包延迟 ≤ 300ms |
| 接口抽象 | AsrService / TtsService 接口，实现层可切换不同供应商 |
| 降级 | ASR 识别置信度 < 0.6 时请求用户重复 |
| 并发 | 单实例支持 ≥ 50 路并发流式连接 |

---

## 8. 非功能性需求

### 8.1 性能

| 指标 | 第一期 | 远期 |
|------|-------|------|
| API 响应时间 | ≤ 2s | ≤ 500ms |
| 呼叫建立延迟 | ≤ 3s（FS originate） | ≤ 2s |
| 日志查询响应 | ≤ 1s | ≤ 500ms（含分页） |
| AI 首次回复延迟 | N/A | ≤ 2s（ASR+LLM+TTS） |
| 并发通话数 | 10（开发验证） | 500+ |

### 8.2 可靠性

| 指标 | 要求 |
|------|------|
| 可用性 | 第一期：单机可用；远期：99.9% |
| 数据不丢失 | 通话日志 100% 写入，事件幂等 |
| 故障降级 | AI 异常 → 转人工；DB 异常 → 本地缓存兜底 |

### 8.3 安全

| 安全项 | 第一期 | 远期 |
|-------|-------|------|
| 认证 | 无 | JWT / OAuth2 |
| 授权 | 无 | RBAC（角色权限） |
| 数据脱敏 | 日志中号码脱敏 | 全链路脱敏 |
| 审计 | 无 | 操作审计日志 |
| 传输加密 | HTTP | HTTPS + WSS |

### 8.4 可观测性

| 层次 | 手段 | 引入阶段 |
|------|------|---------|
| 健康检查 | Spring Actuator /health | 第一期 |
| 业务日志 | 结构化 JSON 日志 + traceId | 第一期 |
| 指标采集 | Micrometer → Prometheus | 第二期 |
| 链路追踪 | Micrometer Tracing / SkyWalking | 第三期 |
| 大屏看板 | Grafana | 第三期 |

---

## 9. 分期路线图

### 第一期：基础呼叫（当前）

**目标**：可运行的 MVP，验证 FS + SpringBoot 集成链路

- [x] 项目骨架搭建（Maven 多模块 + SSM 三层架构）
- [ ] 外呼 API → FS ESL originate
- [ ] 呼入事件监听 → 事件发布（EventPublisher 抽象）
- [ ] 通话日志持久化 + 查询 API
- [ ] 最小化 Vue UI（外呼 + 日志）
- [ ] 统一响应格式 + 全局异常处理

**前端页面规范（Vue）**：

| 页面 | 功能 | 关键交互 |
|------|------|---------|
| 外呼发起 | 输入主叫/被叫号码，点击发起呼叫 | 调用 POST /api/call/outbound，显示成功/失败提示 |
| 通话日志 | 按号码/时间范围查询通话记录列表 | 调用 GET /api/call/log，展示 direction、caller、callee、startTime、endTime、durationSec；其中 direction 来自 call_type 映射，durationSec 来自 call_duration 映射 |

**验收**：外呼可触发、呼入可记录、日志可查询、UI 可操作

### 第二期：AI 引擎 + 坐席管理

**目标**：接入 ASR/TTS/LLM，实现一个完整的 AI 对话外呼场景

- [ ] ASR/TTS 引擎集成（Service 接口 + 实现）
- [ ] LLM 对话引擎（Prompt 管理 + Function Calling）
- [ ] AI 外呼场景（选一个：催收提醒 / 售后回访 / 预约确认）
- [ ] 坐席签入/签出/状态管理
- [ ] 基础 ACD（自动呼叫分配）
- [ ] Redis 引入（坐席状态 + 限流）

**验收**：AI 外呼可完成 80% 标准对话，异常场景安全降级

### 第三期：人机协同 + 录音 + IVR

**目标**：完善呼叫中心核心能力

- [ ] 转人工流程（AI → 坐席无缝切换）
- [ ] 坐席辅助（实时提词、知识推荐）
- [ ] IVR 导航（按键 + 语音菜单）
- [ ] 通话录音（存储 + 回放）
- [ ] 通话摘要自动生成

**验收**：完整呼叫链路可运营

### 第四期：质检 + 报表 + 高可用

**目标**：生产级运营支撑

- [ ] 自动质检（规则 + AI 评分）
- [ ] 运营报表 + 实时看板
- [ ] 集群部署 + 高可用
- [ ] 全链路安全（RBAC + 审计 + 加密）
- [ ] 性能优化 + 压测

**验收**：可交付给真实运营团队使用

---

## 10. 评估与质量门禁

### 10.1 代码质量

| 门禁项 | 标准 | 工具 |
|-------|------|------|
| 编译 | 零 warning | Maven Compiler |
| 单测覆盖 | ≥ 60%（核心逻辑 100%） | JUnit 5 + JaCoCo |
| 代码规范 | 零 Critical | Alibaba P3C / Checkstyle |
| 架构合规 | Controller 不跨层调用 Mapper，Service 无循环依赖 | Code Review / ArchUnit |
| 安全扫描 | 零 High 漏洞 | OWASP Dependency-Check |

### 10.2 AI 质量（第二期起）

| 指标 | 定义 | 目标 |
|------|------|------|
| 任务成功率 | AI 独立完成对话目标的比例 | ≥ 80% |
| 幻觉率 | AI 回复中事实错误的比例 | ≤ 5% |
| 转人工率 | 需要转人工的通话比例 | ≤ 20% |
| 用户满意度 | 通话后评分（CSAT） | ≥ 4.0/5.0 |
| 平均对话轮次 | 完成任务的平均交互次数 | ≤ 6 轮 |
| 首次回复延迟 | ASR → LLM → TTS 总延迟 | ≤ 2s |

### 10.3 评估方法

1. **回归测试集**：维护 100+ 条标准对话样本，每次 Prompt 变更后跑全量
2. **A/B 测试**：新版本先切 10% 流量验证
3. **人工抽检**：每周随机抽取 5% 通话录音质检
4. **压力测试**：上线前模拟 500 并发通话

---

## 11. Vibe Coding 工作规范

> 以下规范约束 AI 辅助编码（Vibe Coding）的行为边界。

### 11.1 Prompt 引用规则

每个代码生成/优化 Prompt 必须在头部声明：

```
## 约束引用
- 系统规格书：docs/SYSTEM_SPEC.md
- 阶段目标：docs/GOALS.md
- 当前阶段：第 N 期
- 范围限制：仅限 [模块名]
```

### 11.2 生成代码的硬约束

- 必须遵循 SSM 三层分层（Controller → Service → Mapper），不得跨层调用
- 数据层必须使用 MyBatis-Plus，不得手写 XML（除复杂 SQL 外）
- 事件发布必须通过 EventPublisher 接口，不得直接依赖 Spring Event API
- 必须使用统一响应格式（Result）
- 必须包含 public 方法的 JavaDoc
- 必须提供对应的单元测试
- 不得生成超出当前阶段范围的功能
- 不得引入未在技术栈决策中批准的依赖

### 11.3 迭代节奏

```
1. 写/更新 Spec（定义做什么、不做什么）
2. 写 Prompt（引用 Spec，生成代码）
3. AI 生成代码（Vibe Coding）
4. 人工评审（架构合规性、业务正确性）
5. 自动化验证（编译 → 测试 → 规范检查）
6. 真实场景验证（发起真实呼叫测试）
7. 回溯更新 Spec（补充发现的问题和决策）
```

---

## 12. 风险与降级策略

| 风险 | 概率 | 影响 | 降级策略 |
|------|------|------|---------|
| FreeSWITCH 连接断开 | 中 | 高 | 自动重连 + 本地事件缓存 |
| LLM 响应超时（>3s） | 中 | 中 | 播放"请稍候"→ 重试一次 → 转人工 |
| ASR 识别率低 | 低 | 中 | 请求重复 → 超过 3 次转人工 |
| 数据库写入失败 | 低 | 高 | 本地文件兜底 → 异步补偿写入 |
| AI 生成违规内容 | 低 | 极高 | 输出过滤 → 即时断话 → 告警 |
| 并发超限 | 中 | 中 | 队列排队 → 语音提示等待 |

---

## 13. 术语表

| 术语 | 定义 |
|------|------|
| ACD | Automatic Call Distribution，自动呼叫分配 |
| ASR | Automatic Speech Recognition，自动语音识别 |
| CTI | Computer Telephony Integration，计算机电话集成 |
| ESL | Event Socket Library，FreeSWITCH 事件协议 |
| IVR | Interactive Voice Response，交互式语音应答 |
| LLM | Large Language Model，大语言模型 |
| PII | Personally Identifiable Information，个人可识别信息 |
| SIP | Session Initiation Protocol，会话初始协议 |
| TTS | Text-to-Speech，文本转语音 |
| CSAT | Customer Satisfaction Score，客户满意度评分 |
| AHT | Average Handle Time，平均处理时长 |

---

## 附录 A：决策记录模板（ADR）

每当做出重要技术决策时，在 `docs/adr/` 目录下新增一条记录：

```markdown
# ADR-NNN: [决策标题]

## 状态
[提议 | 已批准 | 已废弃]

## 背景
[为什么需要做这个决策？]

## 决策
[选了什么方案？]

## 备选方案
[还考虑过什么？为什么没选？]

## 影响
[这个决策会带来什么约束或后果？]
```

---

## 附录 B：文档体系

```
docs/
├── SYSTEM_SPEC.md          # 系统全景规格书（唯一权威源）
├── GOALS.md                # 阶段目标定义（v2.0，SSM 三层架构）
├── PHASE1_TASKS.md         # 第一阶段任务拆分（含验收清单 + 执行记录）
├── ARCHITECTURE.md         # 详细架构图（待补充）
├── EVAL_SPEC.md            # 评估指标与测试集（待补充）
├── adr/                    # 架构决策记录
│   └── ADR-001-xxx.md
├── archive/                # 已归档文档（不再生效）
│   ├── TASKS_MVP.md        # 已合并入 SYSTEM_SPEC.md
│   ├── ALPHA_PROMPT_v1.md  # 六边形架构版生成器（已失效）
│   └── OMEGA_PROMPT_v1.md  # 六边形架构版优化器（已失效）
└── prompt/                 # AI 生成/优化 Prompt（待按新架构重写）
```


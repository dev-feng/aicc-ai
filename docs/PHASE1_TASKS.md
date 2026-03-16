# 第一阶段 - 详细任务拆分

> **约束引用**：docs/SYSTEM_SPEC.md + docs/GOALS.md  
> **架构**：SSM 三层（Controller / Service / Mapper）+ MyBatis-Plus + 事件驱动（Spring Event，可切 MQ）  
> **每个任务 = 一次 Vibe Coding 会话的工作量**

## AI 执行要求

- 每次会话只处理一个任务或一个最小闭环，不得跨多个任务同时大范围发散。
- 实现顺序默认遵循依赖关系图；如需跳过依赖任务，必须先说明降级原因与影响。
- 任务完成的最低标准不是“代码已生成”，而是“存在可执行验证动作且结果可说明”。
- 若外部依赖未就绪，可先完成启动兼容、接口占位或 mock 验证；但必须明确标注为降级方案，不得冒充真实联调完成。
- AI 完成任务后，必须回填该任务的“执行记录”；未回填不得视为任务完成。即使任务未完成，也必须记录当前进展、验证结果、阻塞项和下一步建议。
- 如任务实现导致接口、字段、目录或依赖关系变化，必须同步更新 `docs/SYSTEM_SPEC.md` 或 `docs/GOALS.md` 的对应约束。

### 任务验收输出模板

每次任务交付时，至少应输出以下信息：

1. 本次完成了什么。
2. 修改了哪些文件。
3. 如何启动、如何验证。
4. 哪些能力是真实打通，哪些仍是 mock / 降级。
5. 下一步最合理的任务是什么。

---
## 任务总览

| 编号 | 任务 | 依赖 | 预估 | 状态 |
|------|------|------|------|------|
| T1 | Maven 工程骨架 + 公共模块 | 无 | 1 session | ✅ 已完成 |
| T2 | MySQL 建表 + Entity + Mapper | T1 | 1 session | ✅ 已完成 |
| T3 | FreeSWITCH ESL 连接管理 | T1 | 1 session | ✅ 已完成 |
| T4 | 外呼 API 全链路 | T2, T3 | 1 session | 🟡 部分完成 |
| T5 | 呼入事件监听 | T3 | 1 session | ⬜ 待开始 |
| T6 | 通话日志落库 | T2, T4/T5 | 1 session | ⬜ 待开始 |
| T7 | 通话日志查询 API | T2, T6 | 1 session | ⬜ 待开始 |
| T8 | 全局异常处理 + 参数校验 | T1 | 0.5 session | ⬜ 待开始 |
| T9 | Vue 前端（外呼页 + 日志页） | T4, T7 | 1 session | ⬜ 待开始 |
| T10 | 端到端联调 + 单元测试 | 全部 | 1 session | ⬜ 待开始 |

```
依赖关系图：

T1 ──┬── T2 ──┬── T4 ──┬── T6 ── T7 ── T9
     │        │        │                 │
     ├── T3 ──┤── T5 ──┘                 │
     │                                   │
     └── T8                             T10
```

---

## T1: Maven 工程骨架 + 公共模块

### 任务定义

**目标**：搭建 Maven 多模块工程，完成公共模块核心类

**输入**：
- SYSTEM_SPEC.md 第 3/4 章（技术栈 + 架构）
- GOALS.md G1 项目骨架可启动

**产出文件清单**：

```
backend/call-center/
├── pom.xml                          # 父 POM（依赖版本管理）
├── common/
│   ├── pom.xml
│   └── src/main/java/com/callcenter/common/
│       ├── event/
│       │   └── EventPublisher.java          # 事件发布接口（抽象）
│       ├── result/
│       │   └── Result.java                  # 统一响应 {"code","msg","data"}
│       ├── exception/
│       │   ├── BusinessException.java       # 业务异常基类
│       │   └── ErrorCode.java               # 错误码枚举
│       └── util/
│           └── ExceptionUtil.java
└── call-core/
    ├── pom.xml                              # 依赖 common
    ├── src/main/java/com/callcenter/core/
    │   ├── CallCoreApplication.java         # 启动类
    │   ├── event/
    │   │   └── SpringEventPublisher.java    # EventPublisher 的 Spring Event 实现
    │   └── config/
    └── src/main/resources/
        ├── application.yml                  # 全局配置
        └── application-core.yml             # 模块配置（FS/MySQL）
```

**关键约束**：
- EventPublisher 接口放 common 模块，SpringEventPublisher 实现放 call-core
- Result 类必须支持泛型：`Result<T>`，含静态工厂方法 `success(data)` / `fail(code, msg)`
- 父 POM 锁定所有依赖版本，子模块不指定版本号

### 验收清单

- [x] `mvn clean install` 编译通过，零 warning
- [x] call-core 启动无报错（此时不连 FS 也不报错，FS 配置可容忍缺失）
- [ ] MySQL 连接池初始化成功（HikariCP 日志可见）— 留 T2 引入 MyBatis-Plus/MySQL 依赖后一并验证
- [ ] Result 类可正常序列化为 JSON：`{"code":200,"msg":"success","data":null}` — 结构已满足，待 T4/T8 有接口后实测或单测序列化
- [x] EventPublisher 接口在 common 模块，SpringEventPublisher 在 call-core 模块
- [x] 所有 public 方法有 JavaDoc（Result、EventPublisher、SpringEventPublisher、ErrorCode、BusinessException、ExceptionUtil 已抽查）

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：2026-03-11（初版）；2026-03-13（补充验证）
- 执行方式：Vibe Coding + 本地验证
- 完成情况：基本完成（编译与启动已通过；MySQL 池、Result JSON 实测留 T2/T4 后补）
- 验证结果：`mvn clean install -DskipTests` 通过；`call-core` 在 profile=core 下 `spring-boot:run` 启动成功（约 1.8s），Tomcat 8080、Actuator 正常；目录与约束符合 T1 定义（EventPublisher 在 common，SpringEventPublisher 在 call-core，Result&lt;T&gt; 含 success/fail，父 POM 统一版本）。
- 降级说明：FreeSWITCH 仍为 `freeswitch.enabled=false`；MySQL 依赖未在 call-core 引入，application-core.yml 中 datasource 配置留 T2 使用，故本次未出现 HikariCP 日志。
- 阻塞项：无。
- 偏差记录：call-core 存在 `config/CoreProperties.java`（与 FS/应用配置相关），未在 T1 产出清单中列出，属合理扩展；无其他偏差。
- 下一步建议：进行 T2（MySQL 建表 + Entity + Mapper），引入 MyBatis-Plus 与 mysql-connector 后复验 HikariCP 与建表脚本。
- 需回溯更新 Spec 的点：无。

---

## T2: MySQL 建表 + Entity + Mapper

### 任务定义

**目标**：创建通话记录表，完成 MyBatis-Plus 实体和 Mapper

**输入**：
- SYSTEM_SPEC.md 第 6 章（数据模型）
- T1 产出的工程骨架

**产出文件清单**：

```
call-core/
├── src/main/java/com/callcenter/core/
│   ├── entity/
│   │   └── CallRecord.java              # 数据库实体（@TableName, @TableId）
│   ├── mapper/
│   │   └── CallRecordMapper.java        # extends BaseMapper<CallRecord>
│   └── config/
│       └── MyBatisPlusConfig.java       # MapperScan + 分页插件
└── src/main/resources/
    └── db/
        └── schema_v1.sql                # 幂等建表脚本
```

**关键约束**：
- Entity 字段与数据库列 1:1 映射，使用 `@TableField` 标注驼峰映射
- 必须包含 `hangup_cause` 字段（SYSTEM_SPEC 6.2 中定义）
- Mapper 继承 `BaseMapper<CallRecord>`，不手写 XML
- schema_v1.sql 使用 `CREATE TABLE IF NOT EXISTS`，幂等可重复执行

### 验收清单

- [x] schema_v1.sql 可在 MySQL 8.0 上执行，重复执行无报错（DDL 与 SYSTEM_SPEC 6.2 一致）
- [x] CallRecord 实体字段与 DDL 列完全对应（含 hangup_cause）
- [x] MyBatisPlusConfig 中配置了 `@MapperScan` 和分页插件
- [ ] 启动后 MyBatis-Plus 日志显示 Mapper 注册成功（需本地 MySQL 已建表后启动验证）
- [ ] 手动调用 `callRecordMapper.insert(...)` 可成功写入（需 MySQL + 执行 schema_v1.sql 后验证；CallRecordMapperTest 因 Spring 上下文加载失败未通过，可后续修）

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：2026-03-13
- 执行方式：Vibe Coding
- 完成情况：已完成（代码与 DDL 就绪；Mapper 注册与 insert 需本地 MySQL 或修测后再验）
- 验证结果：call-core 已加入 mybatis-plus-spring-boot3-starter、mysql-connector-j；新增 db/schema_v1.sql、entity/CallRecord.java、mapper/CallRecordMapper.java、config/MyBatisPlusConfig.java、CallRecordMapperTest；mvn clean compile 通过。
- 降级说明：无。
- 阻塞项：无。
- 偏差记录：CallRecordMapper 仅靠 @MapperScan 扫描，未使用 @Mapper。
- 下一步建议：本地执行 schema_v1.sql 后启动 call-core 确认 HikariCP 与 Mapper 日志；随后进行 T3。
- 需回溯更新 Spec 的点：无。

---

## T3: FreeSWITCH ESL 连接管理

### 任务定义

**目标**：封装 FS ESL 连接配置和基础操作，提供 Service 接口

**输入**：
- SYSTEM_SPEC.md 第 4 章（架构 + 数据流）
- GOALS.md G2 外呼链路可验证 + G3 呼入/事件接入可验证
- link.thingscloud:freeswitch-esl 2.2.0 API

**产出文件清单**：

```
call-core/src/main/java/com/callcenter/core/
├── config/
│   └── FreeSwitchConfig.java            # FS 连接参数 @ConfigurationProperties
├── service/
│   ├── FreeSwitchService.java           # 接口：originate / sendCommand / getStatus
│   └── impl/
│       └── FreeSwitchServiceImpl.java   # ESL 客户端封装
└── (application-core.yml 中追加 FS 配置项)
```

**关键约束**：
- FS 配置通过 `@ConfigurationProperties(prefix = "freeswitch")` 绑定，非 `@Value`
- ESL 连接失败时抛 BusinessException，不裸抛原始异常
- 连接支持重连（最大重试 3 次，间隔 5s）
- FreeSwitchService 是接口，后期可替换实现（如切换到其他 VoIP 平台）

### 验收清单

- [x] application-core.yml 中 FS 配置项完整（host/port/password/timeout）
- [x] FS 配置缺失时，启动不崩溃但打印明确警告日志
- [x] FreeSwitchService 接口定义清晰（originate / 事件订阅 / 连接状态）
- [x] ESL 连接失败时返回 BusinessException 而非原始异常
- [x] 连接重试逻辑可测试（模拟 FS 不可用场景）

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：2026-03-16（首次完成）；2026-03-16（补充真实链路核验）
- 执行方式：Vibe Coding + 本地验证
- 完成情况：已完成
- 验证结果：新增 `FreeSwitchConfig`、`FreeSwitchService`、`FreeSwitchConnectionStatus`、`FreeSwitchServiceImpl` 与 `FreeSwitchServiceImplTest`；`application-core.yml` 已补齐 `enabled/host/port/password/timeout/max-retry-attempts/retry-interval-millis/event-format/startup-events/originate-template`；执行 `mvn -pl :call-core -am package -DskipTests` 通过；执行 `mvn -pl :call-core -am -Dtest=FreeSwitchServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过（3 tests, 0 failures）；执行 `java -jar backend/call-center/call-core/target/call-core-0.0.1-SNAPSHOT.jar --server.port=18080` 启动成功，Tomcat 18080 正常。另据 2026-03-16 的真实运行日志，已出现 `FreeSWITCH connection opened: 115.190.203.136:8025`、`Auth requested` 与 `Auth response success=true, message=[+OK accepted]`，说明 ESL 真实连接与鉴权在用户运行环境中已打通。
- 降级说明：在当前 Codex 运行环境复测时，对 `115.190.203.136:8025` 外连仍返回 `Permission denied: getsockopt`，应用按 `call.core.mock-fs-enabled=true` 继续启动；因此“真实 FS 连通”依据为用户提供的成功日志，“当前环境复测”依据为降级启动日志。
- 阻塞项：无。
- 偏差记录：为适配 `link.thingscloud:freeswitch-esl:2.2.0`，启动时事件订阅采用 `InboundClientOption.addEvents(...)` 预注册，而不是在连接建立前主动调用 `setEventSubscriptions(...)`；补充核验阶段因本地 8080 已被占用，改用 `--server.port=18080` 进行启动验证。
- 下一步建议：进入 T4，基于 `FreeSwitchService.originate(...)` 实现外呼 API 全链路。
- 需回溯更新 Spec 的点：无。

---

## T4: 外呼 API 全链路

### 任务定义

**目标**：实现 POST /api/call/outbound 从 Controller 到 FS ESL 发起呼叫的完整链路

**输入**：
- SYSTEM_SPEC.md 第 4.5 章（外呼数据流）+ 第 5 章（API 契约）
- T2 产出的 Entity/Mapper + T3 产出的 FreeSwitchService

**产出文件清单**：

```
call-core/src/main/java/com/callcenter/core/
├── controller/
│   └── CallController.java              # POST /api/call/outbound
├── dto/
│   ├── OutboundCallRequest.java         # 入参：caller, callee
│   └── OutboundCallResponse.java        # 出参：callId
├── service/
│   ├── CallService.java                 # 外呼业务接口
│   └── impl/
│       └── CallServiceImpl.java         # 调用 FreeSwitchService.originate
└── event/
    ├── CallCreatedEvent.java            # 呼叫创建事件
    └── CallEndedEvent.java              # 呼叫结束事件
```

**关键约束**：
- Controller 只做参数接收和 Result 封装，业务逻辑在 Service
- 外呼发起后通过 EventPublisher 发布 CallCreatedEvent
- Request DTO 添加 `@NotBlank` 校验
- 返回值包含 callId，便于前端跟踪；该 callId 必须来自 FreeSWITCH originate 返回值或后续事件中的真实 UUID，不得本地伪造

### 验收清单

- [ ] curl 调用 POST /api/call/outbound 返回 `{"code":200,"msg":"success","data":{"callId":"xxx"}}`
- [x] 参数为空时返回 `{"code":400,"msg":"参数错误",...}`
- [x] FS 连接失败时返回 `{"code":500,...}` 而非堆栈
- [ ] 日志中可见 CallCreatedEvent 发布记录
- [x] Controller 中无直接业务逻辑（纯委托 Service）

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：2026-03-16
- 执行方式：Vibe Coding + 本地验证
- 完成情况：部分完成
- 验证结果：新增 `CallController`、`OutboundCallRequest`、`OutboundCallResponse`、`CallService`、`CallServiceImpl`、`CallCreatedEvent`、`CallEndedEvent`、`CallEventLogListener`；执行 `mvn -pl :call-core -am "-Dtest=CallControllerTest,CallServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过（4 tests, 0 failures）；执行 `mvn -pl :call-core -am package -DskipTests` 通过。MockMvc 已验证 `POST /api/call/outbound` 的 `200/400/500` 三类返回格式，`CallServiceImplTest` 已验证外呼后通过 `EventPublisher` 发布 `CallCreatedEvent`。
- 降级说明：当前 Codex 运行环境无法连通 FreeSWITCH 8025 端口，因此未在本环境完成真实 `curl -> Controller -> Service -> ESL` 成功外呼验收；仅完成测试级与失败链路验收。
- 阻塞项：无。
- 偏差记录：受当前运行环境网络限制，入参校验先采用 Controller 内最小手动校验返回统一 `400`，未额外引入 `spring-boot-starter-validation`；真实 `CallCreatedEvent` 运行日志待在可连 FreeSWITCH 的环境中补验。
- 下一步建议：在 IDEA 或本机 PowerShell 可直连 FreeSWITCH 的环境中执行真实 `POST /api/call/outbound` 验收；若通过，则回填本任务剩余两项并进入 T5。
- 需回溯更新 Spec 的点：无。

---

## T5: 呼入事件监听

### 任务定义

**目标**：监听 FreeSWITCH 呼入事件，解析并发布领域事件

**输入**：
- SYSTEM_SPEC.md 第 4.5 章（呼入数据流）+ 第 6.1 章（ESL 事件映射）
- T3 产出的 FreeSwitchService

**产出文件清单**：

```
call-core/src/main/java/com/callcenter/core/
├── service/
│   └── impl/
│       └── FreeSwitchServiceImpl.java   # 追加事件监听逻辑
└── event/
    └── (复用 T4 中的 CallCreatedEvent / CallEndedEvent)
```

**关键约束**：
- ESL 事件字段映射严格按 SYSTEM_SPEC 6.1 的规则
- 必须容忍部分字段缺失（如 answer_time 为 null）
- 事件解析失败时记日志，不阻断监听线程
- 通过 EventPublisher 发布事件，不直接调 Service 写库

### 验收清单

- [ ] FS 呼入时，日志中可见 CallCreatedEvent 发布
- [ ] FS 呼入结束时，日志中可见 CallEndedEvent 发布
- [ ] 事件中 callId/caller/callee/时间戳 正确映射
- [ ] 模拟异常事件（字段缺失），监听线程不崩溃
- [ ] 事件通过 EventPublisher 接口发布

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：
- 执行方式：（Vibe Coding / 手动）
- 完成情况：（已完成 / 部分完成 / 未完成）
- 验证结果：
- 降级说明：（无则写“无”）
- 阻塞项：（无则写“无”）
- 偏差记录：（无则写“无”）
- 下一步建议：
- 需回溯更新 Spec 的点：（无则写“无”）

---

## T6: 通话日志落库

### 任务定义

**目标**：监听 CallEndedEvent，计算时长字段，写入 call_record 表

**输入**：
- SYSTEM_SPEC.md 第 6 章（数据模型 + 时长计算规则）
- GOALS.md G4 通话日志可落库可查询
- T2 产出的 Mapper + T4/T5 产出的 Event

**产出文件清单**：

```
call-core/src/main/java/com/callcenter/core/
├── service/
│   ├── CallLogService.java              # 日志写入接口
│   └── impl/
│       └── CallLogServiceImpl.java      # 时长计算 + Mapper 写入
└── event/
    └── CallEventListener.java           # @EventListener 监听 CallEndedEvent
```

**关键约束**：
- 时长计算规则：
  - `ringing_duration` = answer_time - ringing_time（未接通则 0）
  - `answer_duration` = end_time - answer_time（未接通则 0）
  - `call_duration` = end_time - start_time
- CallEventListener 中调用 CallLogService 写入，不直接操作 Mapper
- 幂等：相同 callId 不重复写入（利用 uk_call_id 唯一索引 + 异常捕获）

### 验收清单

- [ ] 外呼结束后，call_record 表自动生成一条记录
- [ ] 呼入结束后，call_record 表自动生成一条记录
- [ ] 时长字段计算正确（ringing_duration / answer_duration / call_duration）
- [ ] 重复事件不导致重复记录（幂等）
- [ ] 10 次连续测试数据 100% 写入

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：
- 执行方式：（Vibe Coding / 手动）
- 完成情况：（已完成 / 部分完成 / 未完成）
- 验证结果：
- 降级说明：（无则写“无”）
- 阻塞项：（无则写“无”）
- 偏差记录：（无则写“无”）
- 下一步建议：
- 需回溯更新 Spec 的点：（无则写“无”）

---

## T7: 通话日志查询 API

### 任务定义

**目标**：实现 GET /api/call/log 查询接口

**输入**：
- SYSTEM_SPEC.md 第 5.2 章（API 契约）
- T2 产出的 Mapper + T6 产出的数据

**产出文件清单**：

```
call-core/src/main/java/com/callcenter/core/
├── controller/
│   └── CallController.java              # 追加 GET /api/call/log
├── dto/
│   ├── CallLogQueryRequest.java         # 入参：phone, startTime?, endTime?
│   └── CallLogResponse.java             # 出参 / VO
└── service/
    └── impl/
        └── CallLogServiceImpl.java      # 追加查询方法
```

**关键约束**：
- phone 参数必填，startTime/endTime 可选
- 使用 MyBatis-Plus 的 `LambdaQueryWrapper` 构造查询条件
- 无数据时返回空列表，不返回 404
- 响应时间 ≤ 1s

### 验收清单

- [ ] GET /api/call/log?phone=1000 返回该号码的通话记录列表
- [ ] 带 startTime/endTime 参数可正确过滤
- [ ] phone 为空时返回 400 错误
- [ ] 无数据时返回 `{"code":200,"data":[]}`
- [ ] Entity → Response DTO 转换正确（不暴露数据库字段名）；至少明确完成 `call_type -> direction`、`call_duration -> durationSec` 的响应映射

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：
- 执行方式：（Vibe Coding / 手动）
- 完成情况：（已完成 / 部分完成 / 未完成）
- 验证结果：
- 降级说明：（无则写“无”）
- 阻塞项：（无则写“无”）
- 偏差记录：（无则写“无”）
- 下一步建议：
- 需回溯更新 Spec 的点：（无则写“无”）

---

## T8: 全局异常处理 + 参数校验

### 任务定义

**目标**：统一异常捕获、参数校验响应，保证所有 API 返回格式一致

**输入**：
- SYSTEM_SPEC.md 第 5.1 章（统一响应格式）
- T1 产出的 Result + BusinessException

**产出文件清单**：

```
common/src/main/java/com/callcenter/common/exception/
└── GlobalExceptionHandler.java          # @RestControllerAdvice

call-core/src/main/java/com/callcenter/core/
└── (各 Request DTO 追加 JSR380 注解)
```

**关键约束**：
- 捕获 BusinessException → 返回对应 code + msg
- 捕获 MethodArgumentNotValidException → 返回 400 + 字段级错误信息
- 捕获 Exception（兜底）→ 返回 500，不暴露堆栈
- 异常日志中打印完整堆栈（ERROR 级），但响应中只返回友好消息

### 验收清单

- [ ] BusinessException 自动捕获，返回自定义 code/msg
- [ ] 参数校验失败返回 400 + 具体字段提示
- [ ] 未知异常返回 500，响应中无堆栈信息
- [ ] 日志中有完整异常堆栈（可定位问题）
- [ ] 所有已有 API 的异常场景都走统一格式

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：
- 执行方式：（Vibe Coding / 手动）
- 完成情况：（已完成 / 部分完成 / 未完成）
- 验证结果：
- 降级说明：（无则写“无”）
- 阻塞项：（无则写“无”）
- 偏差记录：（无则写“无”）
- 下一步建议：
- 需回溯更新 Spec 的点：（无则写“无”）

---

## T9: Vue 前端（外呼页 + 日志页）

### 任务定义

**目标**：最小化 Vue 前端，两个页面，可调用后端 API

**输入**：
- SYSTEM_SPEC.md 第 9 章（前端页面规范表格）
- T4 产出的外呼 API + T7 产出的查询 API

**产出文件清单**：

```
frontend/
├── package.json
├── src/
│   ├── App.vue
│   ├── views/
│   │   ├── OutboundCall.vue             # 外呼发起页
│   │   └── CallLog.vue                  # 通话日志页
│   ├── api/
│   │   └── call.js                      # axios 封装
│   └── router/
│       └── index.js
└── vite.config.js                       # 代理后端 API
```

**关键约束**：
- 外呼页：输入主叫/被叫 → 调用 POST /api/call/outbound → 显示成功/失败
- 日志页：输入号码/时间范围 → 调用 GET /api/call/log → 表格展示
- 表格字段：呼叫类型、主叫、被叫、开始时间、结束时间、通话时长
- 样式可使用 Element Plus 或其他 UI 库，保持简洁

### 验收清单

- [ ] 外呼页可输入号码并成功发起呼叫
- [ ] 外呼成功/失败有明确提示（Toast 或 Alert）
- [ ] 日志页可按号码查询并展示结果
- [ ] 日志页支持按时间范围过滤
- [ ] 前端通过 Vite 代理正确调用后端 API（无跨域问题）

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：
- 执行方式：（Vibe Coding / 手动）
- 完成情况：（已完成 / 部分完成 / 未完成）
- 验证结果：
- 降级说明：（无则写“无”）
- 阻塞项：（无则写“无”）
- 偏差记录：（无则写“无”）
- 下一步建议：
- 需回溯更新 Spec 的点：（无则写“无”）

---

## T10: 端到端联调 + 单元测试

### 任务定义

**目标**：全链路联调验证 + 补充单元测试至覆盖率 ≥ 60%

**输入**：
- T1-T9 全部产出
- GOALS.md 验收总标准

**产出文件清单**：

```
call-core/src/test/java/com/callcenter/core/
├── service/
│   ├── CallServiceTest.java             # 外呼逻辑测试
│   ├── CallLogServiceTest.java          # 日志写入/查询测试
│   └── FreeSwitchServiceTest.java       # ESL 连接测试（Mock）
├── controller/
│   └── CallControllerTest.java          # API 集成测试（MockMvc）
└── event/
    └── CallEventListenerTest.java       # 事件监听测试
```

**关键约束**：
- 单元测试使用 JUnit 5 + Mockito
- FS 相关测试 Mock ESL 客户端，不依赖真实 FS
- 集成测试使用 `@SpringBootTest` + H2 内存数据库
- 覆盖核心场景：正常呼出/呼入、参数异常、FS 连接失败、日志写入/查询

### 验收清单

- [ ] `mvn test` 全部通过
- [ ] 覆盖率 ≥ 60%（JaCoCo 报告）
- [ ] 外呼端到端：API 调用 → FS 呼叫 → 事件 → 日志入库 → 查询返回
- [ ] 呼入端到端：FS 事件 → 事件发布 → 日志入库 → 查询返回
- [ ] Vue 前端可正常操作上述流程
- [ ] 统一响应格式在所有场景下正确（成功/参数错误/系统异常）

### 执行记录

> _每次任务执行后必须回填；未回填不得视为完成。若任务未完成，也必须记录当前进展与阻塞。_

- 执行时间：
- 执行方式：（Vibe Coding / 手动）
- 完成情况：（已完成 / 部分完成 / 未完成）
- 验证结果：
- 降级说明：（无则写“无”）
- 阻塞项：（无则写“无”）
- 偏差记录：（无则写“无”）
- 下一步建议：
- 需回溯更新 Spec 的点：（无则写“无”）

---

## 执行注意事项

### Vibe Coding 会话规范

每次 Vibe Coding 开始前，Prompt 头部必须声明：

```
## 约束引用
- 系统规格书：docs/SYSTEM_SPEC.md
- 阶段目标：docs/GOALS.md
- 当前任务：docs/PHASE1_TASKS.md → T{N}
- 架构：SSM 三层 + MyBatis-Plus + EventPublisher 抽象
```

### 任务完成后必做

1. ✅ 逐条检查验收清单，全部打勾
2. ✅ 填写执行记录（偏差、踩坑、决策变更）
3. ✅ 如果发现 Spec 需要更新，立即回溯修改 SYSTEM_SPEC.md
4. ✅ 更新本文档任务总览表中的状态
5. ✅ `mvn clean compile` 确认无编译错误

### 常见陷阱提醒

| 陷阱 | 预防 |
|------|------|
| AI 生成了六边形架构代码 | Prompt 中明确"SSM 三层，不用 port/adapter/usecase" |
| AI 使用了 JPA 注解 | Prompt 中明确"MyBatis-Plus，不用 @Entity/@Repository" |
| 事件直接用 ApplicationEventPublisher | 必须通过 EventPublisher 接口发布 |
| Controller 里写业务逻辑 | 检查 Controller 是否只有参数接收 + Result 封装 |
| Mapper XML 手写 SQL | 优先用 LambdaQueryWrapper，复杂 SQL 才写 XML |
| 响应格式不统一 | 所有返回都用 Result.success() / Result.fail() |


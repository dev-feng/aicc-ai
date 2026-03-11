# 企业级呼叫系统 - 第一阶段目标定义（Goals.md）

## 文档说明
- 版本：v2.0（对齐 SYSTEM_SPEC.md SSM 三层架构）
- 核心约束：所有阶段均基于「FreeSWITCH + SpringBoot + MySQL + MyBatis-Plus」技术栈
- 架构范式：**SSM 三层架构（Controller / Service / Mapper）+ 事件驱动（可插拔）**
- 第一阶段子模块：call-core，公共模块：common

## 元目标锚定
构建一个高可用、可扩展、安全合规的企业级呼叫中心平台，支持 inbound/outbound 场景，适用于客服、销售、技术支持等业务。系统采用前后端分离架构，通过 AI 协作开发（Spec Coding + Vibe Coding）实现快速迭代。

## 技术栈约束（全局绑定）
- 核心语言：Java 17（Lombok、Record 特性）
- 框架：Spring Boot 3.2.x（Web、Validation、Actuator）
- ORM：**MyBatis-Plus 3.5.5**（Mapper 接口 + 条件构造器，不使用 JPA）
- 架构：**SSM 三层**（Controller → Service → Mapper），严格单向依赖
- 事件驱动：通过 **EventPublisher 接口抽象**，第一期用 Spring Event 实现，后期可切 MQ
- 通信中间件：FreeSWITCH 1.10.7（ESL 协议对接）
- 数据库：MySQL 8.0（InnoDB 引擎，UTF8mb4 编码）
  - **所有新建或修改的数据库必须提供建表语句（强制）**
- 依赖版本：
  - freeswitch-esl-client：0.9.7
  - mybatis-plus-boot-starter：3.5.5
  - lombok：1.18.30
  - spring-context：6.1.x
- 工程结构：Maven 多模块工程（call-center 父工程 + common + call-core）

### 注释规范（强制）
- 所有 **public 方法** 必须包含 JavaDoc，说明用途、参数、返回值、异常
- **复杂业务逻辑** 添加行内注释，解释"为什么这么做"
- 注释语言：中文，JavaDoc 的 `@param` / `@return` 用英文术语
- 禁止无意义注释（如 `// 设置用户名`）

### 非目标（第一期不做）
1. 不做坐席分配、IVR 导航、通话录音
2. 不做高可用、集群部署、深度性能优化
3. 不做前端界面的复杂交互，仅最小化 Vue 页面
4. 不做异常重试、告警通知等运维能力
5. 不做认证/授权（RBAC/JWT）

## 核心子目标（可验证、可量化）

### 子目标 1：项目骨架搭建
- **目标**：完成 SpringBoot 多模块项目基础结构，集成 FS ESL 客户端、MySQL + MyBatis-Plus
- **验收标准**：
  1. 父工程 + common + call-core 模块结构完整，pom.xml 依赖无冲突
  2. 配置文件完整：
     - 全局配置：application.yml
     - 模块配置：application-core.yml（FS/MySQL 连接参数）
     - FS 连接：IP、端口、密码、心跳超时（默认 30s）
     - MySQL 连接：URL、用户名、密码、连接池参数（初始 5，最大 20）
  3. call-core 模块遵循 SSM 三层分层，目录结构如下：
     ```plaintext
     call-core/src/main/java/com/callcenter/core/
     ├── controller/          # 接口层：REST API
     │   └── CallController.java
     ├── service/             # 业务层
     │   ├── CallService.java
     │   ├── CallLogService.java
     │   └── impl/
     ├── mapper/              # 数据层：MyBatis-Plus Mapper
     │   └── CallRecordMapper.java
     ├── entity/              # 数据库实体
     │   └── CallRecord.java
     ├── dto/                 # 传输对象
     ├── event/               # 事件（可插拔）
     │   ├── CallCreatedEvent.java
     │   ├── CallEndedEvent.java
     │   └── CallEventListener.java
     ├── config/              # 配置类
     │   ├── FreeSwitchConfig.java
     │   └── MyBatisPlusConfig.java
     └── util/                # 工具类
     ```
     common 模块：
     ```plaintext
     common/src/main/java/com/callcenter/common/
     ├── event/
     │   └── EventPublisher.java       # 事件发布抽象接口
     ├── result/
     │   └── Result.java               # 统一响应封装
     ├── exception/
     │   ├── BusinessException.java    # 业务异常基类
     │   └── GlobalExceptionHandler.java
     └── util/
         └── ExceptionUtil.java
     ```
  4. 项目启动无报错，MySQL 连接池初始化成功
  5. 所有 public 方法符合注释规范

### 子目标 2：基础呼出能力实现
- **目标**：提供 API 接口触发 FS 发起呼出，接通后播放固定语音
- **验收标准**：
  1. POST /api/call/outbound（参数：caller, callee）能触发 FS 呼出
  2. 被叫接听后播放固定语音（如"您好，测试呼叫成功"）
  3. 呼出过程日志完整（发起时间、振铃时间、接通时间、结束时间、状态）
  4. 事件发布：
     - 呼出发起 → EventPublisher.publish(CallCreatedEvent)
     - 呼出结束 → EventPublisher.publish(CallEndedEvent)
  5. 异常处理：参数为空 / FS 连接失败 → 统一错误响应（code=400/500）

### 子目标 3：基础呼入能力实现
- **目标**：FS 接收呼入后，通过 ESL 事件传入 SpringBoot，关键节点发布事件
- **验收标准**：
  1. FS 呼入事件通过 ESL 监听接入 call-core，无丢事件、无重复
  2. 呼入事件解析完整（主叫、被叫、呼入时间、振铃状态）
  3. 事件发布：
     - 呼入振铃 → EventPublisher.publish(CallCreatedEvent)
     - 呼入结束 → EventPublisher.publish(CallEndedEvent)
  4. 异常处理：事件解析失败时记录日志，不阻断核心链路

### 子目标 4：通话日志落地 MySQL
- **目标**：核心通话数据写入 MySQL，支持基础查询
- **验收标准**：
  1. 提供幂等建表脚本（schema_v1.sql），含索引和注释
  2. 日志写入规则：
     - 监听 CallEndedEvent 触发写入
     - 时长计算：ringing_duration / answer_duration / call_duration
  3. 查询 API：GET /api/call/log（phone 必填，startTime/endTime 可选）
  4. 数据完整性：10 次连续测试 100% 写入，无丢失/重复

## 验收总标准
1. **统一响应格式**：`{"code":200,"msg":"success","data":{...}}`，错误码：400/404/500
2. **性能**：单次呼入/呼出全流程耗时 ≤ 2s
3. **测试**：单元测试覆盖率 ≥ 60%，覆盖 ESL 连接、呼叫触发、日志写入
4. **架构合规**：Controller 不跨层调用 Mapper，Service 无循环依赖
5. **事件合规**：事件发布通过 EventPublisher 接口，不直接依赖 Spring Event API

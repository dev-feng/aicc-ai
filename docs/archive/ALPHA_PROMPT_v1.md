# 企业级呼叫系统 - 第一阶段生成器.md
## 生成指令（核心）
请严格遵循以下要求，生成企业级呼叫系统第一阶段（`call-core`模块）完整可运行代码，代码需100%匹配《第一阶段目标定义（Goals.md）》的所有约束和验收标准。

## 一、基础约束（不可修改）
### 1. 技术栈
- 核心语言：Java 17（模块化、Lombok、Record特性）
- 框架：SpringBoot 3.2.x（Web、Data JPA、Validation、Actuator）
- 架构范式：六边形架构（Ports & Adapters）+ 事件驱动（Spring Event）
- 通信中间件：FreeSWITCH 1.10.7（ESL协议对接）
- 数据库：MySQL 8.0（InnoDB引擎，UTF8mb4编码）
- 依赖版本：
    - freeswitch-esl-client：0.9.7
    - mybatis-plus：3.5.5
    - lombok：1.18.30
    - spring-context：6.1.x

### 2. 工程结构（强制按此目录生成）
```plaintext
call-center（父工程）
├── pom.xml（父 POM，管理依赖版本）
├── common（公共模块）
│ ├── pom.xml
│ └── src/main/java/com/callcenter/common
│ ├── domain/event # 通用领域事件
│ │ ├── CallCreatedEvent.java
│ │ ├── CallAnsweredEvent.java
│ │ └── CallEndedEvent.java
│ ├── port # 通用输出端口
│ │ ├── DatabasePort.java
│ │ └── FreeswitchPort.java
│ └── util # 工具类
│ ├── Result.java # 统一响应封装
│ └── ExceptionUtil.java
└── call-core（第一阶段核心模块）
├── pom.xml
├── src/main/java/com/callcenter/core
│ ├── domain # 领域层（纯净，无外部依赖）
│ │ ├── model
│ │ │ ├── CallLog.java
│ │ │ └── CallStatus.java
│ │ ├── service
│ │ │ └── CallDomainService.java
│ │ └── event # 模块内事件（复用 common）
│ ├── application # 应用层
│ │ ├── usecase
│ │ │ ├── OutboundCallUseCase.java
│ │ │ └── CallLogSaveUseCase.java
│ │ └── event
│ │ └── CallEventPublisher.java
│ ├── port # 端口层
│ │ ├── inbound
│ │ │ └── CallApiPort.java
│ │ └── outbound
│ │ ├── FreeswitchPort.java # 复用 common 或重定义
│ │ └── CallLogPersistencePort.java
│ └── adapter # 适配器层
│ ├── inbound
│ │ └── CallRestAdapter.java
│ └── outbound
│ ├── FreeswitchAdapterImpl.java
│ └── CallLogJpaAdapter.java
└── src/main/resources
├── application-core.yml # 模块专属配置
└── schema_v1.sql # 数据库脚本
```
### 3. 注释规范（强制遵守）
- 所有public方法必须包含JavaDoc，说明用途、参数、返回值、异常
- 复杂业务逻辑添加行内注释（解释“为什么做”而非“做了什么”）
- 注释语言：中文（主体），JavaDoc的`@param`/`@return`用英文术语
- 禁止无意义注释（如`// 设置用户名`）

## 二、核心功能生成要求
### 1. 项目骨架生成
- 生成完整的Maven多模块POM文件（父工程+common+call-core）
- 生成`application.yml`（全局）+`application-core.yml`（模块）配置文件：
  - FS连接：IP、端口、密码、心跳超时（30s）
  - MySQL连接：URL、用户名、密码、连接池参数（初始5，最大20）
- 保证领域层（`com.callcenter.core.domain`）无任何外部框架依赖（Spring/JPA）

### 2. 基础呼出能力生成
- 生成POST `/api/call/outbound`接口（CallRestAdapter实现）：
  - 请求参数：`caller`（String）、`callee`（String）
  - 响应格式：统一`Result`封装
- 生成FS呼出核心逻辑（FreeswitchAdapterImpl）：
  - 调用FS ESL API发起呼出
  - 被叫接听后播放固定语音“您好，测试呼叫成功”
- 事件发布：
  - 呼出发起：发布`CallCreatedEvent`（callId、caller、callee、callType=2）
  - 呼出结束：发布`CallEndedEvent`（callId、endTime、status）
- 异常处理：参数为空/FS连接失败时返回对应错误码（400/500）

### 3. 基础呼入能力生成
- 生成FS呼入事件监听逻辑（FreeswitchAdapterImpl）：
  - 接收FS转发的呼入事件，解析主叫、被叫、时间、状态
- 事件发布：
  - 呼入振铃：发布`CallCreatedEvent`（callId、caller、callee、callType=1）
  - 呼入结束：发布`CallEndedEvent`（callId、endTime、status）
- 异常处理：事件解析失败时记录日志，不阻断核心链路

### 4. 通话日志落地生成
- 生成`schema_v1.sql`完整数据库脚本（幂等、含索引/注释）
- 生成CallLog领域模型（与表字段1:1映射）
- 生成`CallLogPersistencePort`及JPA实现（CallLogJpaAdapter）：
  - 实现`saveCallLog`（写入日志）、`queryCallLogByPhone`（查询日志）
- 生成日志写入触发逻辑：
  - 监听`CallEndedEvent`，自动计算时长字段（ringing_duration/answer_duration/call_duration）
  - 保证10次连续测试数据100%写入，无丢失/重复
- 生成GET `/api/call/log`查询接口：
  - 参数：`phone`（必填）、`startTime`（可选）、`endTime`（可选）
  - 响应时间≤1秒，返回统一`Result`格式

## 三、验收标准校验（生成代码需满足）
1. **响应格式**：所有API返回`{"code":XXX,"msg":"XXX","data":XXX}`
   - 成功：code=200，msg="success"
   - 失败：400（参数错误）、404（资源不存在）、500（系统异常）
2. **性能**：单次呼入/呼出全流程耗时≤2秒
3. **测试**：生成单元测试代码，覆盖率≥60%（覆盖ESL连接、呼叫触发、日志写入）
4. **架构合规**：
   - 六边形架构分层无跨层依赖
   - 领域层无外部框架依赖
   - 事件驱动逻辑与业务解耦
5. **文档**：生成第一阶段运行手册（启动步骤、API示例、问题排查）

## 四、生成输出要求
### 1. 代码输出形式
- 按工程结构分文件生成，每个文件标注完整包名+文件名
- 代码可直接复制到本地运行，无需额外修改
- 关键代码（如ESL对接、事件发布）添加详细注释

### 2. 附加输出
- 生成`README.md`（模块说明、启动命令）
- 生成单元测试代码（JUnit 5）
- 生成数据库脚本执行说明
- 生成常见问题排查指南（FS连接失败、日志写入失败）

## 五、禁止生成内容
1. 不生成坐席分配、IVR导航、通话录音相关代码
2. 不生成分布式事件总线（如Kafka）相关代码
3. 不拆分`call_log`为呼入/呼出分表
4. 不生成前端代码
5. 不生成高可用、集群部署相关代码
# MVP 端到端实现标准（统一规范）

> 目标：模型按本规范直接生成可运行 MVP，不新增范围外功能。

## 1. 统一交付约束
- 必须可运行、可调用、可演示
- 仅实现 PROJECT_SPEC.md MVP 范围
- 所有 API 采用统一响应结构
- 关键日志与异常应可定位

## 2. 数据模型与字段规范（CallLog）
最小字段集（可扩展但不可缺少）：
- id: 自增主键
- callId: 通话唯一标识（来自 ESL 或系统生成）
- direction: INBOUND / OUTBOUND
- caller: 主叫号码
- callee: 被叫号码
- startTime: 通话开始时间
- answerTime: 接通时间（可为空）
- endTime: 通话结束时间
- durationSec: 通话时长（秒）
- hangupCause: 挂断原因（可为空）
- createdAt: 记录创建时间
- updatedAt: 记录更新时间

索引建议：
- idx_callId
- idx_caller
- idx_callee
- idx_startTime

## 3. FreeSWITCH 事件映射规范
- 必须订阅并处理可用的呼入/呼出事件
- 事件字段映射到 CallLog：
  - callId: 事件中的唯一 ID（如 UUID）
  - caller/callee: 事件字段映射
  - startTime/answerTime/endTime: 事件时间戳
  - durationSec: endTime - answerTime 或事件字段
  - hangupCause: 对应 ESL 字段
- 必须容忍部分字段缺失（如 answerTime）

## 4. API 规范
统一响应结构：
- code: 0 为成功，其它为错误码
- message: 文本消息
- data: 业务数据

### 4.1 POST /api/call/outbound
- 参数：caller, callee
- 行为：调用 FreeSWITCH 发起外呼
- 返回：标准响应，data 可返回 callId

### 4.2 GET /api/call/log
- 参数：phone, startTime, endTime（可选）
- 行为：查询通话日志，支持过滤
- 返回：日志列表（分页可选）

## 5. 服务层规范
- 外呼 Service：封装外呼 ESL 调用
- 日志 Service：负责事件落库
- 查询 Service：按条件过滤查询

## 6. DAO 规范
- 必须提供：insert(CallLog)、query(filters)
- 所有 SQL 写法集中在 DAO 层

## 7. 前端规范（Vue）
- 页面 1：外呼发起
  - 输入主叫/被叫，调用外呼 API
  - 显示成功/失败提示
- 页面 2：通话日志列表
  - 支持按 phone/startTime/endTime 查询
  - 列表展示字段：direction、caller、callee、startTime、endTime、durationSec

## 8. 运行与验证标准
- 外呼 API 可触发真实呼叫
- 呼入事件能生成日志
- 查询 API 返回正确结果
- UI 可正常调用 API 与展示数据


# 项目规格 - 企业呼叫系统

## 总体目标
构建一个安全、可扩展、企业级的呼叫系统，支持呼入与呼出，提供可靠的通话日志与查询 API，并包含实用的 Web 操作界面。系统应便于维护、可扩展，并符合真实呼叫中心的流程。

## 第一期目标（MVP）
交付一个可运行的 MVP：通过 FreeSWITCH 支持呼入与呼出，使用 MySQL 持久化通话日志，并提供一个最小化的 Vue 界面用于发起呼叫与查看日志。

## 范围边界
### 必须包含（第一期）
- 呼出：API 触发 FreeSWITCH 发起呼叫
- 呼入：接收 FreeSWITCH 事件并转化为通话日志
- 通话日志持久化：MySQL 表与 DAO 写入链路
- 查询 API：通话日志列表与过滤
- 最小化 UI：发起呼出并查看日志
- 统一 API 响应格式与基础错误处理

### 不在范围内（第一期）
- 坐席分配、IVR、录音、质检评分、报表
- 高可用、集群、深度性能调优
- RBAC/认证与多租户功能

## 架构选择
- SSM 三层架构（Controller / Service / DAO）
- Service 层通过 helper/adapter 集成 FreeSWITCH ESL 客户端
- 通话事件统一规范为 CallLog DTO，并通过 DAO 持久化
- Vue 前端消费 REST API

## 技术栈决策
- 后端：Java（SSM 三层）
- 语音平台：FreeSWITCH（ESL）
- 数据库：MySQL
- 前端：Vue
- 认证：第一期不包含
- 缓存/NoSQL：仅在确有必要时使用 Redis 或 Mongo，否则延后

## 第一期模块
### 后端
- api（Controller）：REST 端点
- service：呼叫编排、ESL 交互、日志处理
- dao：MySQL 持久化
- model/dto：请求/响应与日志结构

### 前端
- 呼叫发起页面
- 通话日志列表页面

## 第一期 API（草案）
- POST /api/call/outbound
  - 参数：caller, callee
  - 行为：触发 FreeSWITCH 外呼
- GET /api/call/log
  - 参数：phone, startTime, endTime（可选）
  - 行为：通话日志列表

## 执行计划（第一期）
1. 创建后端骨架（SSM 分层、配置、响应模型）
2. 设计 MySQL Schema 与 DAO 层
3. 集成 FreeSWITCH ESL，处理呼出/呼入事件
4. 实现通话日志持久化与查询 API
5. 构建最小化 Vue UI：外呼与日志列表
6. 端到端验证与基础文档

## 验收标准
1. 外呼 API 可触发 FreeSWITCH 呼叫
2. 正常情况下呼入事件无丢失
3. 呼叫结束后在 MySQL 生成日志记录
4. Vue UI 可发起呼叫并读取日志
5. 成功与错误都使用统一响应格式

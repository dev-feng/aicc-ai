package com.callcenter.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通话记录实体，与 call_record 表 1:1 映射。
 * <p>
 * 字段与 SYSTEM_SPEC.md 6.2 核心表 DDL 一致，含 hangup_cause。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("call_record")
public class CallRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 呼叫唯一标识（UUID） */
    private String callId;

    /** 主叫号码 */
    private String caller;

    /** 被叫号码 */
    private String callee;

    /** 呼叫类型：1-呼入，2-呼出 */
    private Integer callType;

    /** 呼叫开始时间 */
    private LocalDateTime startTime;

    /** 振铃开始时间 */
    private LocalDateTime ringingTime;

    /** 接通时间 */
    private LocalDateTime answerTime;

    /** 呼叫结束时间 */
    private LocalDateTime endTime;

    /** 状态：ringing/answered/hungup */
    private String status;

    /** 振铃时长（秒） */
    private Integer ringingDuration;

    /** 接通时长（秒） */
    private Integer answerDuration;

    /** 通话总时长（秒） */
    private Integer callDuration;

    /** 挂断原因（ESL hangup cause） */
    private String hangupCause;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录更新时间 */
    private LocalDateTime updateTime;
}

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
 * 转人工结果实体，对应 transfer_result 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("transfer_result")
public class TransferResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String callId;

    private Integer attemptNo;

    private Long targetAgentId;

    private String targetExtensionNo;

    /**
     * 状态：pending/success/failed/timeout/cancelled
     */
    private String transferStatus;

    private String failureReason;

    private String fallbackAction;

    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

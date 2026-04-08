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
 * 通话摘要实体，对应 call_summary 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("call_summary")
public class CallSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String callId;

    private String summaryText;

    private String summaryStatus;

    private String resultCode;

    private String tags;

    private String sourceType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

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
 * 坐席基础信息实体，对应 agent_info 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_info")
public class Agent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentCode;

    private String agentName;

    private String status;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

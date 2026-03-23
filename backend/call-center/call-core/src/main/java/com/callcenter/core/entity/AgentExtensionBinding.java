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
 * 坐席与分机绑定关系实体，对应 agent_extension_binding 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_extension_binding")
public class AgentExtensionBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private String extensionNo;

    private String bindingStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

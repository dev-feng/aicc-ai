package com.callcenter.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第一期核心模块基础配置。
 */
@ConfigurationProperties(prefix = "call.core")
public class CoreProperties {

    private boolean mockFsEnabled = true;

    /**
     * 获取是否允许在缺少 FS 配置时以降级模式启动。
     *
     * @return mock fs flag
     */
    public boolean isMockFsEnabled() {
        return mockFsEnabled;
    }

    /**
     * 设置是否允许在缺少 FS 配置时以降级模式启动。
     *
     * @param mockFsEnabled mock fs flag
     */
    public void setMockFsEnabled(boolean mockFsEnabled) {
        this.mockFsEnabled = mockFsEnabled;
    }
}
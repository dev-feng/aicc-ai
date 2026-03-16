package com.callcenter.core.config;

import link.thingscloud.freeswitch.esl.constant.EventNames;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * FreeSWITCH ESL 连接配置。
 */
@ConfigurationProperties(prefix = "freeswitch")
public class FreeSwitchConfig {

    private boolean enabled = true;
    private String host;
    private Integer port;
    private String password;
    private int timeoutSeconds = 30;
    private int heartbeatTimeoutSeconds = 30;
    private int maxRetryAttempts = 3;
    private long retryIntervalMillis = 5000L;
    private String eventFormat = "plain";
    private String originateTemplate = "user/%s";
    private List<String> startupEvents = new ArrayList<>(List.of(
            EventNames.CHANNEL_CREATE,
            EventNames.CHANNEL_HANGUP_COMPLETE
    ));

    /**
     * 是否启用 FreeSWITCH 连接管理。
     *
     * @return enabled flag
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 FreeSWITCH 连接管理。
     *
     * @param enabled enabled flag
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 FreeSWITCH 主机地址。
     *
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置 FreeSWITCH 主机地址。
     *
     * @param host host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取 FreeSWITCH ESL 端口。
     *
     * @return port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * 设置 FreeSWITCH ESL 端口。
     *
     * @param port port
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * 获取 ESL 密码。
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置 ESL 密码。
     *
     * @param password password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取命令超时时间。
     *
     * @return timeout seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 设置命令超时时间。
     *
     * @param timeoutSeconds timeout seconds
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 获取心跳超时时间。
     *
     * @return heartbeat timeout seconds
     */
    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    /**
     * 设置心跳超时时间。
     *
     * @param heartbeatTimeoutSeconds heartbeat timeout seconds
     */
    public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }

    /**
     * 获取最大重试次数。
     *
     * @return max retry attempts
     */
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    /**
     * 设置最大重试次数。
     *
     * @param maxRetryAttempts max retry attempts
     */
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    /**
     * 获取重试间隔。
     *
     * @return retry interval millis
     */
    public long getRetryIntervalMillis() {
        return retryIntervalMillis;
    }

    /**
     * 设置重试间隔。
     *
     * @param retryIntervalMillis retry interval millis
     */
    public void setRetryIntervalMillis(long retryIntervalMillis) {
        this.retryIntervalMillis = retryIntervalMillis;
    }

    /**
     * 获取事件订阅格式。
     *
     * @return event format
     */
    public String getEventFormat() {
        return eventFormat;
    }

    /**
     * 设置事件订阅格式。
     *
     * @param eventFormat event format
     */
    public void setEventFormat(String eventFormat) {
        this.eventFormat = eventFormat;
    }

    /**
     * 获取外呼目标模板。
     *
     * @return originate template
     */
    public String getOriginateTemplate() {
        return originateTemplate;
    }

    /**
     * 设置外呼目标模板。
     *
     * @param originateTemplate originate template
     */
    public void setOriginateTemplate(String originateTemplate) {
        this.originateTemplate = originateTemplate;
    }

    /**
     * 获取启动时订阅事件列表。
     *
     * @return startup events
     */
    public List<String> getStartupEvents() {
        return startupEvents;
    }

    /**
     * 设置启动时订阅事件列表。
     *
     * @param startupEvents startup events
     */
    public void setStartupEvents(List<String> startupEvents) {
        this.startupEvents = startupEvents == null ? new ArrayList<>() : new ArrayList<>(startupEvents);
    }

    /**
     * 配置是否具备发起连接的最小条件。
     *
     * @return true if host/port/password are available
     */
    public boolean hasRequiredConnectionInfo() {
        return host != null && !host.isBlank()
                && port != null && port > 0
                && password != null && !password.isBlank();
    }

    /**
     * 获取当前服务器地址。
     *
     * @return server address
     */
    public String getServerAddress() {
        if (host == null || port == null) {
            return "";
        }
        return host + ":" + port;
    }
}

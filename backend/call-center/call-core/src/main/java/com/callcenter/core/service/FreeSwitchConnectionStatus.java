package com.callcenter.core.service;

import java.util.List;

/**
 * FreeSWITCH 当前连接状态快照。
 */
public record FreeSwitchConnectionStatus(
        boolean enabled,
        boolean configured,
        boolean degraded,
        boolean connected,
        String serverAddress,
        String connectState,
        int lastConnectAttempts,
        String lastErrorMessage,
        List<String> subscribedEvents
) {
}

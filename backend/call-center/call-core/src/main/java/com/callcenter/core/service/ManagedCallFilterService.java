package com.callcenter.core.service;

/**
 * Filters raw FreeSWITCH events down to calls managed by this system.
 */
public interface ManagedCallFilterService {

    ManagedCallDecision evaluate(String callId, String caller, String callee);

    record ManagedCallDecision(
            boolean accepted,
            String reason,
            Long agentId,
            String extensionNo
    ) {
        public static ManagedCallDecision accepted(Long agentId, String extensionNo) {
            return new ManagedCallDecision(true, "accepted", agentId, extensionNo);
        }

        public static ManagedCallDecision rejected(String reason) {
            return new ManagedCallDecision(false, reason, null, null);
        }
    }
}

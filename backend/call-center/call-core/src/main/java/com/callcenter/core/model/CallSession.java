package com.callcenter.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSession {

    public static final String STATUS_RINGING = "ringing";
    public static final String STATUS_ANSWERED = "answered";
    public static final String STATUS_THINKING = "thinking";
    public static final String STATUS_SPEAKING = "speaking";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_TRANSFER_PENDING = "transfer_pending";
    public static final String STATUS_TRANSFERRED = "transferred";
    public static final String STATUS_TRANSFER_FAILED = "transfer_failed";

    private String callId;

    private String caller;

    private String callee;

    private Integer callType;

    private String currentStatus;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime ringingTime;

    private LocalDateTime answerTime;

    private LocalDateTime endTime;

    private String hangupCause;

    private LocalDateTime updatedAt;
}

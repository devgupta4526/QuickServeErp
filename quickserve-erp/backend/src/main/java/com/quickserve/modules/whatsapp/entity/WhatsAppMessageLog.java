package com.quickserve.modules.whatsapp.entity;

import com.quickserve.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_message_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppMessageLog extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "to_number", nullable = false)
    private String toNumber;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "template_variables", columnDefinition = "jsonb")
    private String templateVariables;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageStatus status = MessageStatus.PENDING;

    @Column(name = "wamid")
    private String wamid;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    public enum MessageStatus {
        PENDING, SENT, DELIVERED, READ, FAILED, PERMANENTLY_FAILED
    }
}

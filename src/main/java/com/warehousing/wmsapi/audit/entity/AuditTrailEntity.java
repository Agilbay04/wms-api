package com.warehousing.wmsapi.audit.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_trails")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditTrailEntity extends AuditableEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}

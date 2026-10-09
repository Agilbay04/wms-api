package com.warehousing.wmsapi.iam.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions", uniqueConstraints =
        @UniqueConstraint(name = "uq_permissions_operation_resource", columnNames = {"operation_id", "resource_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermissionEntity extends AuditableEntity {
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;
}

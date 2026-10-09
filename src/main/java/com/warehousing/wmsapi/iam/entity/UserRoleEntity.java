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
@Table(name = "user_roles", uniqueConstraints =
        @UniqueConstraint(name = "uq_user_roles_user_role", columnNames = {"user_id", "role_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoleEntity extends AuditableEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;
}

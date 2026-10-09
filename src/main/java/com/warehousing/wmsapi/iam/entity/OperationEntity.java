package com.warehousing.wmsapi.iam.entity;

import com.warehousing.wmsapi.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "operations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperationEntity extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}

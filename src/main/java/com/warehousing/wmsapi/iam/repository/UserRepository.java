package com.warehousing.wmsapi.iam.repository;

import com.warehousing.wmsapi.iam.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmail(String email);
}

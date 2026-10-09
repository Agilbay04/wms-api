package com.warehousing.wmsapi.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final AccessControlRepository repository;

    @Override
    public boolean hasPermission(Authentication authentication, String resource, String operation) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return repository.hasPermission(authentication.getName(), resource, operation);
    }
}

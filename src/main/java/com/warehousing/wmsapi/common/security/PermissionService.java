package com.warehousing.wmsapi.common.security;

import org.springframework.security.core.Authentication;

public interface PermissionService {
    boolean hasPermission(Authentication authentication, String resource, String operation);
}

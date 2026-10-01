package com.warehousing.wmsapi.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class SecurityErrorHandlerTest {

    @Test
    void shouldWriteUnauthenticatedEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ApiAuthenticationEntryPoint().commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("missing token"));

        assertEquals(401, response.getStatus());
        assertEquals("{\"success\":false,\"code\":401,\"message\":\"Authentication is required.\",\"data\":null,\"errors\":{}}\n", response.getContentAsString());
    }

    @Test
    void shouldWriteForbiddenEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ApiAccessDeniedHandler().handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("permission missing"));

        assertEquals(403, response.getStatus());
        assertEquals("{\"success\":false,\"code\":403,\"message\":\"You do not have permission to perform this action.\",\"data\":null,\"errors\":{}}\n", response.getContentAsString());
    }
}

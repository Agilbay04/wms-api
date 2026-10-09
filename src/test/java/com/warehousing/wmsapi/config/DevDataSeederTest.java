package com.warehousing.wmsapi.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.warehousing.wmsapi.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class DevDataSeederTest {

    @Test
    void shouldSkipExistingSuperadminUsingUserRepository() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AppProperties properties = mock(AppProperties.class);
        when(properties.seedAdmin()).thenReturn(new AppProperties.SeedAdminProperties(
                "admin@example.com", "Admin", "password"));
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        new DevDataSeeder(new DevSeedRepository(jdbcTemplate), userRepository, passwordEncoder, properties)
                .run(mock(ApplicationArguments.class));

        verify(userRepository).existsByEmail("admin@example.com");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldInsertMissingSuperadminAfterRepositoryCheck() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AppProperties properties = mock(AppProperties.class);
        when(properties.seedAdmin()).thenReturn(new AppProperties.SeedAdminProperties(
                "admin@example.com", "Admin", "password"));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        new DevDataSeeder(new DevSeedRepository(jdbcTemplate), userRepository, passwordEncoder, properties)
                .run(mock(ApplicationArguments.class));

        verify(userRepository).existsByEmail("admin@example.com");
        verify(jdbcTemplate).update(
                "INSERT INTO users(email, name, password, is_active) VALUES (?, ?, ?, true)",
                "admin@example.com", "Admin", "encoded-password");
    }
}

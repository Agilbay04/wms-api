package com.warehousing.wmsapi.config;

import com.warehousing.wmsapi.iam.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {
    private static final List<String> OPERATIONS = List.of("CREATE", "READ", "UPDATE", "DELETE", "APPROVE", "REJECT", "EXPORT");
    private static final List<String> RESOURCES = List.of("USERS", "ROLES", "PRODUCTS", "PRODUCT_CATEGORIES", "WAREHOUSES", "WAREHOUSE_LOCATIONS", "INVENTORY", "INBOUNDS", "OUTBOUNDS", "STOCK_TRANSFERS", "STOCK_ADJUSTMENTS", "REPORTS");
    private static final List<String> TRANSACTION_RESOURCES = List.of("INBOUNDS", "OUTBOUNDS", "STOCK_TRANSFERS", "STOCK_ADJUSTMENTS");
    private static final List<String> MASTER_READ_RESOURCES = List.of("PRODUCTS", "PRODUCT_CATEGORIES", "WAREHOUSES", "WAREHOUSE_LOCATIONS", "INVENTORY");
    private final DevSeedRepository seedRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        seedReferenceData();
        if (hasSeedAdminConfiguration()) seedSuperadmin();
    }

    private void seedReferenceData() {
        seedRepository.seedReferenceData(OPERATIONS, RESOURCES);
        seedRepository.grantPermissions("STAFF", TRANSACTION_RESOURCES, List.of("CREATE", "READ", "UPDATE"));
        seedRepository.grantPermissions("STAFF", MASTER_READ_RESOURCES, List.of("READ"));
        seedRepository.grantPermissions("SUPERVISOR", TRANSACTION_RESOURCES, List.of("READ", "APPROVE", "REJECT"));
        seedRepository.grantPermissions("SUPERVISOR", MASTER_READ_RESOURCES, List.of("READ"));
        seedRepository.grantPermissions("SUPERVISOR", List.of("REPORTS"), List.of("READ", "EXPORT"));
    }

    private void seedSuperadmin() {
        AppProperties.SeedAdminProperties admin = appProperties.seedAdmin();
        if (!userRepository.existsByEmail(admin.email())) {
            seedRepository.insertSeedAdmin(admin.email(), admin.name(), passwordEncoder.encode(admin.password()));
        }
        seedRepository.assignSuperadmin(admin.email());
    }

    private boolean hasSeedAdminConfiguration() {
        AppProperties.SeedAdminProperties admin = appProperties.seedAdmin();
        return admin != null && !admin.email().isBlank() && !admin.name().isBlank() && !admin.password().isBlank();
    }
}

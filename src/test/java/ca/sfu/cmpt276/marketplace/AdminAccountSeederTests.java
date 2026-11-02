package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Role;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "admin.email=seeded-admin@sfu.ca",
        "admin.password=seed-admin-pass"
})
class AdminAccountSeederTests {

    @Autowired
    private UserAccountRepository repository;

    @Test
    void seedsAdminAccountFromEnvironment() {
        UserAccount admin = repository.findByEmail("seeded-admin@sfu.ca").orElseThrow();

        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(new BCryptPasswordEncoder().matches("seed-admin-pass", admin.getPasswordHash()));
    }
}

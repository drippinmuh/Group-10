package ca.sfu.cmpt276.marketplace.config;

import ca.sfu.cmpt276.marketplace.model.Role;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

// Admins are never created through public registration; the account comes from
// the ADMIN_EMAIL / ADMIN_PASSWORD environment variables (set on Render).
@Component
public class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final UserAccountRepository repository;
    private final PasswordEncoder encoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminAccountSeeder(UserAccountRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set; skipping admin account seeding");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            return;
        }
        repository.save(new UserAccount("Administrator", email,
                encoder.encode(adminPassword), Role.ADMIN, Instant.now()));
        log.info("Seeded admin account {}", email);
    }
}

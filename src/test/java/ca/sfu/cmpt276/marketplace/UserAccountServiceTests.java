package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Role;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.RegistrationException;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class UserAccountServiceTests {

    @Autowired
    private UserAccountService service;

    @Autowired
    private UserAccountRepository repository;

    @BeforeEach
    void clearUsers() {
        repository.deleteAll();
    }

    @Test
    void registersUserWithUserRoleAndHashedPassword() {
        UserAccount saved = service.register("Ethan Cratchley", "ethan@sfu.ca", "password123");

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(Role.USER, saved.getRole());
        assertNotEquals("password123", saved.getPasswordHash());
        assertTrue(new BCryptPasswordEncoder().matches("password123", saved.getPasswordHash()));
    }

    @Test
    void normalizesEmailToLowercaseAndTrimsWhitespace() {
        UserAccount saved = service.register("Ethan Cratchley", "  Ethan@SFU.ca ", "password123");

        assertEquals("ethan@sfu.ca", saved.getEmail());
    }

    @Test
    void rejectsNonSfuEmail() {
        assertThrows(RegistrationException.class,
                () -> service.register("Ethan Cratchley", "ethan@gmail.com", "password123"));
        assertEquals(0, repository.count());
    }

    @Test
    void rejectsSfuDomainWithEmptyLocalPart() {
        assertThrows(RegistrationException.class,
                () -> service.register("Ethan Cratchley", "@sfu.ca", "password123"));
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        service.register("Ethan Cratchley", "ethan@sfu.ca", "password123");

        assertThrows(RegistrationException.class,
                () -> service.register("Other Person", "ETHAN@sfu.ca", "password456"));
        assertEquals(1, repository.count());
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        assertThrows(RegistrationException.class,
                () -> service.register("Ethan Cratchley", "ethan@sfu.ca", "short"));
    }

    @Test
    void rejectsBlankFullName() {
        assertThrows(RegistrationException.class,
                () -> service.register("  ", "ethan@sfu.ca", "password123"));
    }

    @Test
    void loadsUserDetailsByEmailWithUserRole() {
        service.register("Ethan Cratchley", "ethan@sfu.ca", "password123");

        UserDetails details = service.loadUserByUsername("ethan@sfu.ca");

        assertEquals("ethan@sfu.ca", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadingUnknownEmailThrows() {
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nobody@sfu.ca"));
    }
}

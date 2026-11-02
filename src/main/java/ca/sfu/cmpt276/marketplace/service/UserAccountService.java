package ca.sfu.cmpt276.marketplace.service;

import ca.sfu.cmpt276.marketplace.model.Role;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class UserAccountService implements UserDetailsService {

    private static final Pattern SFU_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@sfu\\.ca$");

    private final UserAccountRepository repository;
    private final PasswordEncoder encoder;

    public UserAccountService(UserAccountRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public UserAccount register(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new RegistrationException("Full name is required.");
        }
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!SFU_EMAIL.matcher(normalized).matches()) {
            throw new RegistrationException("Email must be an SFU address ending in @sfu.ca.");
        }
        if (password == null || password.length() < 8) {
            throw new RegistrationException("Password must be at least 8 characters.");
        }
        if (repository.existsByEmail(normalized)) {
            throw new RegistrationException("An account with this email already exists.");
        }
        return repository.save(new UserAccount(fullName.trim(), normalized,
                encoder.encode(password), Role.USER, Instant.now()));
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        UserAccount user = repository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}

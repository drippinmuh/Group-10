package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Role;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
class SecurityTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private UserAccountService service;

    private MockMvc mockMvc;

    @BeforeEach
    void seedUsers() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        repository.deleteAll();
        service.register("Test User", "user@sfu.ca", "password123");
        repository.save(new UserAccount("Test Admin", "admin@sfu.ca",
                new BCryptPasswordEncoder().encode("adminpass123"), Role.ADMIN, Instant.now()));
    }

    @Test
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void listingsPageIsPublic() throws Exception {
        mockMvc.perform(get("/listings")).andExpect(status().isOk());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void registerPageIsPublic() throws Exception {
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void anonymousAdminRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String url = result.getResponse().getRedirectedUrl();
                    assertNotNull(url);
                    assertTrue(url.endsWith("/login"), "expected redirect to login but was " + url);
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotAccessAdminSubpaths() throws Exception {
        mockMvc.perform(get("/admin/reports")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleIsNotBlockedFromAdmin() throws Exception {
        // The /admin page itself lives on feature/admin-dashboard, so assert
        // security lets the request through rather than asserting the view renders.
        mockMvc.perform(get("/admin"))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(302, result.getResponse().getStatus()));
    }

    @Test
    void loginWithValidCredentialsRedirectsUserToHome() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "user@sfu.ca").password("password123"))
                .andExpect(authenticated().withUsername("user@sfu.ca"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminLoginRedirectsToAdminDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "admin@sfu.ca").password("adminpass123"))
                .andExpect(authenticated().withUsername("admin@sfu.ca"))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "user@sfu.ca").password("wrongpass"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginWithUnknownEmailFails() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "ghost@sfu.ca").password("password123"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutEndsSessionAndRedirectsToLogin() throws Exception {
        mockMvc.perform(logout())
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?logout"));
    }
}

package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Role;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private UserAccountService service;

    @BeforeEach
    void clearUsers() {
        repository.deleteAll();
    }

    @Test
    void registerCreatesAccountAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ethan Cratchley")
                        .param("email", "ethan@sfu.ca")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertEquals(Role.USER, repository.findByEmail("ethan@sfu.ca").orElseThrow().getRole());
    }

    @Test
    void registerWithNonSfuEmailShowsErrorAndSavesNothing() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ethan Cratchley")
                        .param("email", "ethan@gmail.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertEquals(0, repository.count());
    }

    @Test
    void registerWithDuplicateEmailShowsError() throws Exception {
        service.register("Ethan Cratchley", "ethan@sfu.ca", "password123");

        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Other Person")
                        .param("email", "ethan@sfu.ca")
                        .param("password", "password456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertEquals(1, repository.count());
    }

    @Test
    void registerWithShortPasswordShowsError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ethan Cratchley")
                        .param("email", "ethan@sfu.ca")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertTrue(repository.findByEmail("ethan@sfu.ca").isEmpty());
    }

    @Test
    void registerWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Ethan Cratchley")
                        .param("email", "ethan@sfu.ca")
                        .param("password", "password123"))
                .andExpect(status().isForbidden());
    }
}

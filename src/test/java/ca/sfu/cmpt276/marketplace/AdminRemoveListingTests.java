package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingStatus;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminRemoveListingTests {

    private static final String SELLER_EMAIL = "seller@sfu.ca";
    private static final String ADMIN_EMAIL = "admin@sfu.ca";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private UserAccountService userService;

    @Autowired
    private ListingService listingService;

    private Listing listing;

    @BeforeEach
    void seed() {
        listingRepository.deleteAll();
        userRepository.deleteAll();

        userService.register("Test Seller", SELLER_EMAIL, "password123");

        listing = listingService.createListing(
                SELLER_EMAIL, "Desk Lamp", "Working desk lamp",
                new BigDecimal("15.00"), "Furniture",
                "Good", "Burnaby", null);
    }

    @AfterEach
    void clearListings() {
        listingRepository.deleteAll();
    }

    private String removeUrl() {
        return "/admin/listings/" + listing.getId() + "/remove";
    }

    private ListingStatus currentStatus() {
        return listingRepository.findById(listing.getId())
                .orElseThrow().getStatus();
    }

    // ADMIN-01
    @Test
    void adminCanRemoveAnotherUsersListing() throws Exception {
        mockMvc.perform(post(removeUrl())
                        .with(csrf())
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings"))
                .andExpect(flash().attribute("message",
                        "Listing removed by administrator."));

        assertEquals(ListingStatus.REMOVED, currentStatus());
    }

    // ADMIN-02
    @Test
    void removedListingDisappearsFromBrowseAndDetails() throws Exception {
        mockMvc.perform(post(removeUrl())
                        .with(csrf())
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/listings"))
                .andExpect(content().string(not(containsString("Desk Lamp"))));

        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(status().isNotFound());
    }

    // ADMIN-03
    @Test
    void regularUserCannotRemoveListing() throws Exception {
        mockMvc.perform(post(removeUrl())
                        .with(csrf())
                        .with(user(SELLER_EMAIL).roles("USER")))
                .andExpect(status().isForbidden());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    // ADMIN-04
    @Test
    void anonymousUserCannotRemoveListing() throws Exception {
        mockMvc.perform(post(removeUrl()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    // ADMIN-05
    @Test
    void adminRemovalOfUnknownListingReturnsNotFound() throws Exception {
        mockMvc.perform(post("/admin/listings/999999/remove")
                        .with(csrf())
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    // ADMIN-06
    @Test
    void adminCanSeeRemovalButton() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId())
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(removeUrl())));
    }

    // ADMIN-07
    @Test
    void regularUserCannotSeeRemovalButton() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId())
                        .with(user(SELLER_EMAIL).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        not(containsString(removeUrl()))));
    }

    // ADMIN-08
    @Test
    void adminRemovalWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post(removeUrl())
                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }
}
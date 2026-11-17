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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ListingStatusTests {

    private static final String SELLER_EMAIL = "seller@sfu.ca";
    private static final String OTHER_EMAIL = "other@sfu.ca";

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
        userService.register("Sam Seller", SELLER_EMAIL, "password123");
        userService.register("Olly Other", OTHER_EMAIL, "password123");
        listing = listingService.createListing(SELLER_EMAIL, "Desk Lamp", "LED, adjustable",
                new BigDecimal("15.00"), "Furniture", "Good", "Burnaby", null);
    }

    @AfterEach
    void clearListings() {
        listingRepository.deleteAll();
    }

    private String statusUrl() {
        return "/listings/" + listing.getId() + "/update-status";
    }

    private String detailsUrl() {
        return "/listings/" + listing.getId();
    }

    private ListingStatus currentStatus() {
        return listingRepository.findById(listing.getId()).orElseThrow().getStatus();
    }

    private void setStatus(ListingStatus status) {
        listing.setStatus(status);
        listingRepository.save(listing);
    }

    private static void assertRedirectsToLogin(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();
        assertNotNull(url);
        assertTrue(url.endsWith("/login"), "expected redirect to login but was " + url);
    }

    @Test
    void ownerMarksListingSold() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "SOLD").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailsUrl()))
                .andExpect(flash().attribute("message", "Listing status updated."));
        assertEquals(ListingStatus.SOLD, currentStatus());
    }

    @Test
    void ownerMarksSoldListingAvailableAgain() throws Exception {
        setStatus(ListingStatus.SOLD);

        mockMvc.perform(post(statusUrl()).param("status", "AVAILABLE").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void statusIsCaseInsensitive() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "sold").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection());

        assertEquals(ListingStatus.SOLD, currentStatus());
    }

    @Test
    void soldListingDisappearsFromBrowseAndReturnsWhenAvailable() throws Exception {
        setStatus(ListingStatus.SOLD);
        mockMvc.perform(get("/listings"))
                .andExpect(content().string(not(containsString("Desk Lamp"))));

        setStatus(ListingStatus.AVAILABLE);
        mockMvc.perform(get("/listings"))
                .andExpect(content().string(containsString("Desk Lamp")));
    }

    @Test
    void soldListingDetailsStillVisibleWithSoldBadge() throws Exception {
        setStatus(ListingStatus.SOLD);

        mockMvc.perform(get(detailsUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"badge bg-secondary\">Sold</")));
    }

    @Test
    void removedStatusIsRejected() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "REMOVED").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailsUrl()))
                .andExpect(flash().attribute("error", "Invalid status: REMOVED"));

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void unknownStatusIsRejected() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "banana").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Invalid status: banana"));

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void missingStatusIsBadRequest() throws Exception {
        mockMvc.perform(post(statusUrl()).with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().isBadRequest());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void nonOwnerIsForbiddenAndStatusStays() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "SOLD").with(csrf()).with(user(OTHER_EMAIL)))
                .andExpect(status().isForbidden());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void anonymousRedirectsToLoginAndStatusStays() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "SOLD").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(ListingStatusTests::assertRedirectsToLogin);

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void withoutCsrfTokenIsForbiddenAndStatusStays() throws Exception {
        mockMvc.perform(post(statusUrl()).param("status", "SOLD").with(user(SELLER_EMAIL)))
                .andExpect(status().isForbidden());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void removedListingIsNotFound() throws Exception {
        setStatus(ListingStatus.REMOVED);

        mockMvc.perform(post(statusUrl()).param("status", "AVAILABLE").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().isNotFound());

        assertEquals(ListingStatus.REMOVED, currentStatus());
    }

    @Test
    void detailsPageShowsErrorFlash() throws Exception {
        mockMvc.perform(get(detailsUrl()).flashAttr("error", "Invalid status: banana"))
                .andExpect(content().string(containsString("Invalid status: banana")));
    }

    @Test
    void toggleButtonShownOnlyToOwnerWithCorrectLabel() throws Exception {
        mockMvc.perform(get(detailsUrl()).with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("Mark as Sold")));

        setStatus(ListingStatus.SOLD);
        mockMvc.perform(get(detailsUrl()).with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("Mark as Available")));

        mockMvc.perform(get(detailsUrl()).with(user(OTHER_EMAIL)))
                .andExpect(content().string(not(containsString(statusUrl()))));
        mockMvc.perform(get(detailsUrl()))
                .andExpect(content().string(not(containsString(statusUrl()))));
    }
}
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
class DeleteListingTests {

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

    private String deleteUrl() {
        return "/listings/" + listing.getId() + "/delete";
    }

    private ListingStatus currentStatus() {
        return listingRepository.findById(listing.getId()).orElseThrow().getStatus();
    }

    private static void assertRedirectsToLogin(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();
        assertNotNull(url);
        assertTrue(url.endsWith("/login"), "expected redirect to login but was " + url);
    }

    @Test
    void ownerDeleteRemovesListingAndRedirectsToBrowse() throws Exception {
        mockMvc.perform(post(deleteUrl()).with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings"))
                .andExpect(flash().attribute("message", "Listing deleted."));

        assertEquals(ListingStatus.REMOVED, currentStatus());
    }

    @Test
    void deletedListingDisappearsFromBrowseAndDetails() throws Exception {
        mockMvc.perform(post(deleteUrl()).with(csrf()).with(user(SELLER_EMAIL)));

        mockMvc.perform(get("/listings"))
                .andExpect(content().string(not(containsString("Desk Lamp"))));
        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanDeleteSoldListing() throws Exception {
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        mockMvc.perform(post(deleteUrl()).with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().is3xxRedirection());

        assertEquals(ListingStatus.REMOVED, currentStatus());
    }

    @Test
    void nonOwnerDeleteIsForbiddenAndListingStays() throws Exception {
        mockMvc.perform(post(deleteUrl()).with(csrf()).with(user(OTHER_EMAIL)))
                .andExpect(status().isForbidden());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void anonymousDeleteRedirectsToLoginAndListingStays() throws Exception {
        mockMvc.perform(post(deleteUrl()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(DeleteListingTests::assertRedirectsToLogin);

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void deleteWithoutCsrfTokenIsForbiddenAndListingStays() throws Exception {
        mockMvc.perform(post(deleteUrl()).with(user(SELLER_EMAIL)))
                .andExpect(status().isForbidden());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void deleteViaGetIsNotAllowed() throws Exception {
        mockMvc.perform(get(deleteUrl()).with(user(SELLER_EMAIL)))
                .andExpect(status().isMethodNotAllowed());

        assertEquals(ListingStatus.AVAILABLE, currentStatus());
    }

    @Test
    void deleteOfUnknownListingIsNotFound() throws Exception {
        mockMvc.perform(post("/listings/999999/delete").with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAlreadyRemovedListingIsNotFound() throws Exception {
        listing.setStatus(ListingStatus.REMOVED);
        listingRepository.save(listing);

        mockMvc.perform(post(deleteUrl()).with(csrf()).with(user(SELLER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void browsePageShowsDeleteConfirmationMessage() throws Exception {
        mockMvc.perform(get("/listings").flashAttr("message", "Listing deleted."))
                .andExpect(content().string(containsString("Listing deleted.")));
    }

    @Test
    void detailsPageShowsDeleteButtonOnlyToOwner() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId()).with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("action=\"" + deleteUrl() + "\"")));
        mockMvc.perform(get("/listings/" + listing.getId()).with(user(OTHER_EMAIL)))
                .andExpect(content().string(not(containsString(deleteUrl()))));
        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(content().string(not(containsString(deleteUrl()))));
    }
}

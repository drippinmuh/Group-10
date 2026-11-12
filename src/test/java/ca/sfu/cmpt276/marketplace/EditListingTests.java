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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EditListingTests {

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
        listing = listingService.createListing(SELLER_EMAIL, "Calculus Textbook", "Stewart 8th edition",
                new BigDecimal("25.00"), "Textbooks", "Good", "Burnaby", null);
    }

    @AfterEach
    void clearListings() {
        listingRepository.deleteAll();
    }

    private Listing reload() {
        return listingRepository.findWithSellerById(listing.getId()).orElseThrow();
    }

    private String editUrl() {
        return "/listings/" + listing.getId() + "/edit";
    }

    private void assertUnchanged() {
        Listing current = reload();
        assertEquals("Calculus Textbook", current.getTitle());
        assertEquals("Stewart 8th edition", current.getDescription());
        assertEquals(0, new BigDecimal("25.00").compareTo(current.getPrice()));
    }

    private static void assertRedirectsToLogin(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();
        assertNotNull(url);
        assertTrue(url.endsWith("/login"), "expected redirect to login but was " + url);
    }

    // Edit form

    @Test
    void ownerSeesEditFormPrefilledWithCurrentValues() throws Exception {
        mockMvc.perform(get(editUrl()).with(user(SELLER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-form"))
                .andExpect(model().attribute("listingId", listing.getId()))
                .andExpect(model().attribute("title", "Calculus Textbook"))
                .andExpect(model().attribute("category", "Textbooks"))
                .andExpect(content().string(containsString("action=\"" + editUrl() + "\"")))
                .andExpect(content().string(containsString("Save Changes")));
    }

    @Test
    void anonymousEditFormRedirectsToLogin() throws Exception {
        mockMvc.perform(get(editUrl()))
                .andExpect(status().is3xxRedirection())
                .andExpect(EditListingTests::assertRedirectsToLogin);
    }

    @Test
    void nonOwnerEditFormIsForbidden() throws Exception {
        mockMvc.perform(get(editUrl()).with(user(OTHER_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void editFormForUnknownListingIsNotFound() throws Exception {
        mockMvc.perform(get("/listings/999999/edit").with(user(SELLER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void editFormForRemovedListingIsNotFound() throws Exception {
        listing.setStatus(ListingStatus.REMOVED);
        listingRepository.save(listing);

        mockMvc.perform(get(editUrl()).with(user(SELLER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createFormStillPostsToCreateEndpoint() throws Exception {
        mockMvc.perform(get("/listings/new").with(user(SELLER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/listings\"")))
                .andExpect(content().string(containsString("Create Listing")));
    }

    // Update

    @Test
    void ownerUpdateSavesAllFieldsAndRedirectsToDetails() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "Calculus Textbook (annotated)")
                        .param("description", "Some highlighting inside")
                        .param("price", "18.50")
                        .param("category", "Books")
                        .param("condition", "Fair")
                        .param("campus", "Surrey")
                        .param("imageUrl", "https://example.com/book.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/" + listing.getId()))
                .andExpect(flash().attribute("message", "Listing updated."));

        Listing updated = reload();
        assertEquals("Calculus Textbook (annotated)", updated.getTitle());
        assertEquals("Some highlighting inside", updated.getDescription());
        assertEquals(0, new BigDecimal("18.50").compareTo(updated.getPrice()));
        assertEquals("Books", updated.getCategory());
        assertEquals("Fair", updated.getCondition());
        assertEquals("Surrey", updated.getCampus());
        assertEquals("https://example.com/book.jpg", updated.getImageUrl());
        assertEquals(SELLER_EMAIL, updated.getSeller().getEmail());
        assertEquals(ListingStatus.AVAILABLE, updated.getStatus());
        assertEquals(1, listingRepository.count());
    }

    @Test
    void ownerCanSetPriceToZeroToGiveItemAway() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "Calculus Textbook")
                        .param("description", "Free to a good home")
                        .param("price", "0"))
                .andExpect(status().is3xxRedirection());

        assertEquals(0, BigDecimal.ZERO.compareTo(reload().getPrice()));
    }

    @Test
    void updateWithBlankTitleShowsErrorKeepsInputAndChangesNothing() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "  ")
                        .param("description", "New description")
                        .param("price", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-form"))
                .andExpect(model().attribute("error", "Title cannot be empty"))
                .andExpect(model().attribute("listingId", listing.getId()))
                .andExpect(model().attribute("description", "New description"));

        assertUnchanged();
    }

    @Test
    void updateWithBlankDescriptionShowsErrorAndChangesNothing() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "New title")
                        .param("description", "")
                        .param("price", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Description cannot be empty"));

        assertUnchanged();
    }

    @Test
    void updateWithNegativePriceShowsErrorAndChangesNothing() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "New title")
                        .param("description", "New description")
                        .param("price", "-1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Price cannot be negative"));

        assertUnchanged();
    }

    @Test
    void nonOwnerUpdateIsForbiddenAndChangesNothing() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf()).with(user(OTHER_EMAIL))
                        .param("title", "Hijacked")
                        .param("description", "Hijacked")
                        .param("price", "1"))
                .andExpect(status().isForbidden());

        assertUnchanged();
    }

    @Test
    void anonymousUpdateRedirectsToLoginAndChangesNothing() throws Exception {
        mockMvc.perform(post(editUrl()).with(csrf())
                        .param("title", "Hijacked")
                        .param("description", "Hijacked")
                        .param("price", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(EditListingTests::assertRedirectsToLogin);

        assertUnchanged();
    }

    @Test
    void updateWithoutCsrfTokenIsForbiddenAndChangesNothing() throws Exception {
        mockMvc.perform(post(editUrl()).with(user(SELLER_EMAIL))
                        .param("title", "New title")
                        .param("description", "New description")
                        .param("price", "1"))
                .andExpect(status().isForbidden());

        assertUnchanged();
    }

    @Test
    void updateOfUnknownListingIsNotFound() throws Exception {
        mockMvc.perform(post("/listings/999999/edit").with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "New title")
                        .param("description", "New description")
                        .param("price", "1"))
                .andExpect(status().isNotFound());
    }

    // Details page

    @Test
    void detailsPageShowsEditButtonToOwner() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId()).with(user(SELLER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"" + editUrl() + "\"")));
    }

    @Test
    void detailsPageHidesEditButtonFromOtherUsersAndVisitors() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId()).with(user(OTHER_EMAIL)))
                .andExpect(content().string(not(containsString(editUrl()))));
        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(content().string(not(containsString(editUrl()))));
    }
}

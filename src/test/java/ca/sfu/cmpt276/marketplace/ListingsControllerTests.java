package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingStatus;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ListingException;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ListingsControllerTests {

    private static final String SELLER_EMAIL = "seller@sfu.ca";

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

    @BeforeEach
    void seedSeller() {
        listingRepository.deleteAll();
        userRepository.deleteAll();
        userService.register("Sam Seller", SELLER_EMAIL, "password123");
    }

    @AfterEach
    void clearListings() {
        listingRepository.deleteAll();
    }

    private Listing createListing(String title) {
        return listingService.createListing(SELLER_EMAIL, title, "A description",
                new BigDecimal("25.00"), "Textbooks", "Good", "Burnaby", null);
    }

    private static void assertRedirectsToLogin(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();
        assertNotNull(url);
        assertTrue(url.endsWith("/login"), "expected redirect to login but was " + url);
    }


    @Test
    void browsePageShowsAvailableListingsToAnonymousVisitors() throws Exception {
        createListing("Calculus Textbook");

        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(content().string(containsString("Calculus Textbook")));
    }

    @Test
    void browsePageHidesSoldAndRemovedListings() throws Exception {
        createListing("Still Available");
        Listing sold = createListing("Already Sold");
        sold.setStatus(ListingStatus.SOLD);
        listingRepository.save(sold);
        Listing removed = createListing("Taken Down");
        removed.setStatus(ListingStatus.REMOVED);
        listingRepository.save(removed);

        MvcResult result = mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Listing> listings = (List<Listing>) result.getModelAndView().getModel().get("listings");
        assertEquals(1, listings.size());
        assertEquals("Still Available", listings.get(0).getTitle());
    }

    @Test
    void detailsPageShowsListingToAnonymousVisitors() throws Exception {
        Listing listing = createListing("Desk Lamp");

        MvcResult result = mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-details"))
                .andExpect(content().string(containsString("Desk Lamp")))
                .andReturn();
        Listing shown = (Listing) result.getModelAndView().getModel().get("listing");
        assertEquals("Sam Seller", shown.getSeller().getFullName());
    }

    @Test
    void unknownListingReturnsNotFound() throws Exception {
        mockMvc.perform(get("/listings/999999")).andExpect(status().isNotFound());
    }


    @Test
    void newListingFormRequiresLogin() throws Exception {
        mockMvc.perform(get("/listings/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(ListingsControllerTests::assertRedirectsToLogin);
    }

    @Test
    void newListingFormIsShownToLoggedInUser() throws Exception {
        mockMvc.perform(get("/listings/new").with(user(SELLER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-form"));
    }

    // Create

    @Test
    void createSavesOneListingForLoggedInSellerAndRedirectsToIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/listings").with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "TI-84 Calculator")
                        .param("description", "Works great")
                        .param("price", "40.00")
                        .param("category", "Electronics")
                        .param("condition", "Good")
                        .param("campus", "Burnaby"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        List<Listing> saved = listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE);
        assertEquals(1, saved.size());
        Listing listing = saved.get(0);
        assertEquals("/listings/" + listing.getId(), result.getResponse().getRedirectedUrl());
        assertEquals("TI-84 Calculator", listing.getTitle());
        assertEquals(0, new BigDecimal("40.00").compareTo(listing.getPrice()));
        assertEquals(SELLER_EMAIL, listing.getSeller().getEmail());
        assertEquals(ListingStatus.AVAILABLE, listing.getStatus());
        assertNotNull(listing.getCreatedAt());
    }

    @Test
    void createAcceptsZeroPriceForFreeItems() throws Exception {
        mockMvc.perform(post("/listings").with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "Free Couch")
                        .param("description", "Pick up only")
                        .param("price", "0"))
                .andExpect(status().is3xxRedirection());

        assertEquals(1, listingRepository.count());
    }

    @Test
    void anonymousCreateRedirectsToLoginAndSavesNothing() throws Exception {
        mockMvc.perform(post("/listings").with(csrf())
                        .param("title", "TI-84 Calculator")
                        .param("description", "Works great")
                        .param("price", "40.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(ListingsControllerTests::assertRedirectsToLogin);

        assertEquals(0, listingRepository.count());
    }

    @Test
    void createWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/listings").with(user(SELLER_EMAIL))
                        .param("title", "TI-84 Calculator")
                        .param("description", "Works great")
                        .param("price", "40.00"))
                .andExpect(status().isForbidden());

        assertEquals(0, listingRepository.count());
    }

    @Test
    void createWithNegativePriceShowsFormWithErrorAndKeepsInput() throws Exception {
        mockMvc.perform(post("/listings").with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "TI-84 Calculator")
                        .param("description", "Works great")
                        .param("price", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-form"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("title", "TI-84 Calculator"));

        assertEquals(0, listingRepository.count());
    }

    @Test
    void createWithBlankTitleShowsFormWithError() throws Exception {
        mockMvc.perform(post("/listings").with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "   ")
                        .param("description", "Works great")
                        .param("price", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-form"))
                .andExpect(model().attributeExists("error"));

        assertEquals(0, listingRepository.count());
    }

    @Test
    void createWithBlankDescriptionShowsFormWithError() throws Exception {
        mockMvc.perform(post("/listings").with(csrf()).with(user(SELLER_EMAIL))
                        .param("title", "TI-84 Calculator")
                        .param("description", "")
                        .param("price", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-form"))
                .andExpect(model().attributeExists("error"));

        assertEquals(0, listingRepository.count());
    }

    // Service

    @Test
    void createListingForUnknownSellerIsRejected() {
        assertThrows(ListingException.class, () -> listingService.createListing("ghost@sfu.ca",
                "Title", "Description", BigDecimal.TEN, null, null, null, null));

        assertEquals(0, listingRepository.count());
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MyListingsTests {

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

    @BeforeEach
    void seed() {
        listingRepository.deleteAll();
        userRepository.deleteAll();
        userService.register("Sam Seller", SELLER_EMAIL, "password123");
        userService.register("Olly Other", OTHER_EMAIL, "password123");
    }

    @AfterEach
    void clearListings() {
        listingRepository.deleteAll();
    }

    private Listing create(String email, String title) {
        return listingService.createListing(email, title, "Some description",
                new BigDecimal("10.00"), "Misc", "Good", "Burnaby", null);
    }

    private void setStatus(Listing listing, ListingStatus status) {
        listing.setStatus(status);
        listingRepository.save(listing);
    }

    private static void assertRedirectsToLogin(MvcResult result) {
        String url = result.getResponse().getRedirectedUrl();
        assertNotNull(url);
        assertTrue(url.endsWith("/login"), "expected redirect to login but was " + url);
    }

    @Test
    void showsOwnAvailableAndSoldListings() throws Exception {
        create(SELLER_EMAIL, "desk lamp");
        setStatus(create(SELLER_EMAIL, "Office Chair"), ListingStatus.SOLD);

        mockMvc.perform(get("/my-listings").with(user(SELLER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(view().name("my-listings"))
                .andExpect(content().string(containsString("desk lamp")))
                .andExpect(content().string(containsString("Office Chair")));
    }

    @Test
    void soldListingHasSoldBadge() throws Exception {
        setStatus(create(SELLER_EMAIL, "Office Chair"), ListingStatus.SOLD);

        mockMvc.perform(get("/my-listings").with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("class=\"badge bg-secondary\">Sold</")));
    }

    @Test
    void availableListingHasNoSoldBadge() throws Exception {
        create(SELLER_EMAIL, "desk lamp");

        mockMvc.perform(get("/my-listings").with(user(SELLER_EMAIL)))
                .andExpect(content().string(not(containsString("class=\"badge bg-secondary\">Sold</"))));
    }

    @Test
    void hidesRemovedListings() throws Exception {
        create(SELLER_EMAIL, "desk lamp");
        setStatus(create(SELLER_EMAIL, "kettle"), ListingStatus.REMOVED);

        mockMvc.perform(get("/my-listings").with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("desk lamp")))
                .andExpect(content().string(not(containsString("kettle"))));
    }

    @Test
    void hidesOtherUsersListings() throws Exception {
        create(SELLER_EMAIL, "desk lamp");
        create(OTHER_EMAIL, "Bike");

        mockMvc.perform(get("/my-listings").with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("desk lamp")))
                .andExpect(content().string(not(containsString("Bike"))));
    }

    @Test
    void showsEmptyStateWhenUserHasNoListings() throws Exception {
        create(SELLER_EMAIL, "desk lamp");

        mockMvc.perform(get("/my-listings").with(user(OTHER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You haven't listed anything yet")))
                .andExpect(content().string(not(containsString("desk lamp"))));
    }

    @Test
    void noEmptyStateWhenUserHasListings() throws Exception {
        create(SELLER_EMAIL, "desk lamp");

        mockMvc.perform(get("/my-listings").with(user(SELLER_EMAIL)))
                .andExpect(content().string(not(containsString("You haven't listed anything yet"))));
    }

    @Test
    void anonymousRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/my-listings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(MyListingsTests::assertRedirectsToLogin);
    }

    @Test
    void navbarShowsMyListingsLinkOnlyWhenLoggedIn() throws Exception {
        mockMvc.perform(get("/listings").with(user(SELLER_EMAIL)))
                .andExpect(content().string(containsString("href=\"/my-listings\"")));
        mockMvc.perform(get("/listings"))
                .andExpect(content().string(not(containsString("href=\"/my-listings\""))));
    }
}
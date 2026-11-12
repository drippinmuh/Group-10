package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.repository.FavouriteRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.FavouriteService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FavouriteControllerTests {

    private static final String ALICE = "alice@sfu.ca";
    private static final String BOB = "bob@sfu.ca";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountService userService;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private ListingService listingService;
    @Autowired private FavouriteRepository favouriteRepository;
    @Autowired private FavouriteService favouriteService;

    @BeforeEach
    void setUp() {
        favouriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        userService.register("Alice", ALICE, "password123");
        userService.register("Bob", BOB, "password123");
    }

    @AfterEach
    void tearDown() {
        favouriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Listing listing(String title) {
        return listingService.createListing(ALICE, title, "Test description",
                new BigDecimal("19.99"), "Books", "Good", "Burnaby", null);
    }

    @Test
    void userCanAddFavourite() throws Exception {
        Listing item = listing("Desk Lamp");
        mockMvc.perform(post("/favourites/" + item.getId()).with(user(ALICE)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/" + item.getId()));
        assertTrue(favouriteService.isFavourite(ALICE, item.getId()));
    }

    @Test
    void addingSameFavouriteTwiceDoesNotCreateDuplicates() throws Exception {
        Listing item = listing("Desk Lamp");
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/favourites/" + item.getId()).with(user(ALICE)).with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
        assertEquals(1, favouriteRepository.count());
    }

    @Test
    void userCanRemoveFavourite() throws Exception {
        Listing item = listing("Desk Lamp");
        favouriteService.addFavourite(ALICE, item.getId());
        mockMvc.perform(post("/favourites/" + item.getId() + "/remove")
                        .with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/listings/" + item.getId()));
        assertFalse(favouriteService.isFavourite(ALICE, item.getId()));
    }

    @Test
    void oneUserCannotRemoveAnotherUsersFavourite() throws Exception {
        Listing item = listing("Desk Lamp");
        favouriteService.addFavourite(ALICE, item.getId());
        mockMvc.perform(post("/favourites/" + item.getId() + "/remove")
                        .with(user(BOB)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertTrue(favouriteService.isFavourite(ALICE, item.getId()));
    }

    @Test
    void guestCannotAddFavourite() throws Exception {
        Listing item = listing("Desk Lamp");
        mockMvc.perform(post("/favourites/" + item.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(0, favouriteRepository.count());
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        Listing item = listing("Desk Lamp");
        mockMvc.perform(post("/favourites/" + item.getId()).with(user(ALICE)))
                .andExpect(status().isForbidden());
        assertEquals(0, favouriteRepository.count());
    }

    @Test
    void nonexistentListingCannotBeFavourited() throws Exception {
        mockMvc.perform(post("/favourites/999999").with(user(ALICE)).with(csrf()))
                .andExpect(status().isNotFound());
        assertEquals(0, favouriteRepository.count());
    }

    @Test
    void favouritesPageShowsOnlyCurrentUsersListings() throws Exception {
        Listing aliceItem = listing("Alice's Lamp");
        Listing bobItem = listing("Bob's Textbook");
        favouriteService.addFavourite(ALICE, aliceItem.getId());
        favouriteService.addFavourite(BOB, bobItem.getId());
        mockMvc.perform(get("/favourites").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(view().name("favourites"))
                .andExpect(content().string(containsString("Lamp")))
                .andExpect(content().string(not(containsString("Textbook"))));
    }

    @Test
    void favouritesPageShowsEmptyState() throws Exception {
        mockMvc.perform(get("/favourites").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No favourites saved yet")));
    }

    @Test
    void guestCannotAccessFavouritesPage() throws Exception {
        mockMvc.perform(get("/favourites"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void detailPageShowsFavouriteActionForLoggedInUser() throws Exception {
        Listing item = listing("Desk Lamp");
        mockMvc.perform(get("/listings/" + item.getId()).with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add to Favourites")));
        favouriteService.addFavourite(ALICE, item.getId());
        mockMvc.perform(get("/listings/" + item.getId()).with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Remove from Favourites")));
    }
}

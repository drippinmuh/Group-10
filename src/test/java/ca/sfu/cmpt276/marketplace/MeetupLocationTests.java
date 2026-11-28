package ca.sfu.cmpt276.marketplace;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.MeetupLocation;
import ca.sfu.cmpt276.marketplace.model.SfuCampus;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.MeetupLocationService;
import ca.sfu.cmpt276.marketplace.service.NominatimClient;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

/**
 * US-20: Search Meetup Locations. The real Nominatim client is replaced with a fake,
 * so these tests never call the external API.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
// The fake API bean gives this class its own Spring context, so it also gets its own in-memory
// database. Otherwise starting this context would recreate the tables other test classes share.
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:meetup-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class MeetupLocationTests {

    private static final String USER_EMAIL = "buyer@sfu.ca";
    private static final String SELLER_EMAIL = "seller@sfu.ca";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeNominatimClient fakeClient;

    @Autowired
    private MeetupLocationService meetupLocationService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private UserAccountService userService;

    @Autowired
    private ListingService listingService;

    @BeforeEach
    void reset() {
        meetupLocationService.clearCache();
        fakeClient.calls.set(0);
        fakeClient.lastCampus = null;
    }

    @AfterEach
    void clearData() {
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    // MEET-01: logged-in user searches near Burnaby and sees results with map links
    @Test
    void searchShowsResultsWithMapLinks() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "library").param("campus", "Burnaby")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("W.A.C. Bennett Library")))
                .andExpect(content().string(containsString("https://www.openstreetmap.org/?mlat=49.2796")))
                .andExpect(content().string(containsString("OpenStreetMap contributors")));
        assertEquals(SfuCampus.BURNABY, fakeClient.lastCampus);
    }

    // MEET-02: selecting Surrey limits the search to the Surrey campus area
    @Test
    void searchUsesSelectedCampus() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "library").param("campus", "Surrey")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk());
        assertEquals(SfuCampus.SURREY, fakeClient.lastCampus);
    }

    // MEET-03: no matching places shows the empty-state message
    @Test
    void noMatchesShowsEmptyState() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "nothing").param("campus", "Vancouver")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No places found near")));
    }

    // MEET-04: a blank search is rejected without calling the API
    @Test
    void blankSearchIsRejected() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "   ").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a place to search for")));
        assertEquals(0, fakeClient.calls.get());
    }

    // MEET-05: a search over 100 characters is rejected without calling the API
    @Test
    void tooLongSearchIsRejected() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "a".repeat(101)).with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("100 characters or fewer")));
        assertEquals(0, fakeClient.calls.get());
    }

    // MEET-06: if Nominatim is down, the page shows a friendly error instead of crashing
    @Test
    void apiOutageShowsFriendlyError() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "outage").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Location search is unavailable right now")));
    }

    // MEET-07: logged-out visitors are sent to the login page
    @Test
    void loggedOutVisitorIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/meetup-locations").param("q", "library"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertEquals(0, fakeClient.calls.get());
    }

    // MEET-08: opening the page without a search shows the form and no error
    @Test
    void pageWithoutQueryShowsFormOnly() throws Exception {
        mockMvc.perform(get("/meetup-locations").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Find a Meetup Spot")))
                .andExpect(content().string(not(containsString("alert-danger"))));
        assertEquals(0, fakeClient.calls.get());
    }

    // MEET-09: listing details shows "Find a Meetup Spot" for logged-in users, with the listing's campus
    @Test
    void listingDetailsLinksToMeetupSearch() throws Exception {
        Listing listing = createListing();

        mockMvc.perform(get("/listings/" + listing.getId()).with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/meetup-locations?campus=Surrey")))
                .andExpect(content().string(containsString("Find a Meetup Spot")));
    }

    // MEET-10: the meetup button is hidden from logged-out visitors
    @Test
    void listingDetailsHidesMeetupLinkWhenLoggedOut() throws Exception {
        Listing listing = createListing();

        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Find a Meetup Spot"))));
    }

    private Listing createListing() {
        userService.register("Test Seller", SELLER_EMAIL, "password123");
        return listingService.createListing(SELLER_EMAIL, "Desk Lamp", "Working desk lamp",
                new BigDecimal("15.00"), "Furniture", "Good", "Surrey", null);
    }

    /** Stands in for the real Nominatim API during tests. */
    static class FakeNominatimClient extends NominatimClient {
        final AtomicInteger calls = new AtomicInteger();
        volatile SfuCampus lastCampus;

        FakeNominatimClient() {
            super("http://unused.test", "test");
        }

        @Override
        public List<MeetupLocation> search(String query, SfuCampus campus) {
            calls.incrementAndGet();
            lastCampus = campus;
            if (query.equals("outage")) {
                throw new ResourceAccessException("Nominatim unavailable");
            }
            if (query.equals("nothing")) {
                return List.of();
            }
            return List.of(new MeetupLocation("W.A.C. Bennett Library",
                    "W.A.C. Bennett Library, University Drive, Burnaby", 49.2796, -122.9189));
        }
    }

    @TestConfiguration
    static class FakeNominatimConfig {
        @Bean
        @Primary
        FakeNominatimClient fakeNominatimClient() {
            return new FakeNominatimClient();
        }
    }
}
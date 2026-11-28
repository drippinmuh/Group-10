package ca.sfu.cmpt276.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import ca.sfu.cmpt276.marketplace.model.MeetupLocation;
import ca.sfu.cmpt276.marketplace.model.SfuCampus;
import ca.sfu.cmpt276.marketplace.service.MeetupLocationException;
import ca.sfu.cmpt276.marketplace.service.MeetupLocationService;
import ca.sfu.cmpt276.marketplace.service.NominatimClient;

/** US-20 unit tests for input checks, caching and error handling. No network or Spring needed. */
class MeetupLocationServiceTests {

    private FakeClient client;
    private MeetupLocationService service;

    @BeforeEach
    void setUp() {
        client = new FakeClient();
        service = new MeetupLocationService(client);
    }

    // MEET-SVC-01: blank search is rejected without calling the API
    @Test
    void blankSearchIsRejected() {
        MeetupLocationException e = assertThrows(MeetupLocationException.class,
                () -> service.search("   ", SfuCampus.BURNABY));
        assertTrue(e.getMessage().contains("Enter a place to search for"));
        assertEquals(0, client.calls);
    }

    // MEET-SVC-02: search over 100 characters is rejected without calling the API
    @Test
    void tooLongSearchIsRejected() {
        assertThrows(MeetupLocationException.class,
                () -> service.search("a".repeat(101), SfuCampus.BURNABY));
        assertEquals(0, client.calls);
    }

    // MEET-SVC-03: surrounding spaces are trimmed before the API call
    @Test
    void searchIsTrimmed() {
        service.search("  library  ", SfuCampus.BURNABY);
        assertEquals("library", client.lastQuery);
    }

    // MEET-SVC-04: repeating a search (any capitalization) is served from the cache
    @Test
    void repeatedSearchUsesCache() {
        List<MeetupLocation> first = service.search("library", SfuCampus.BURNABY);
        List<MeetupLocation> second = service.search("LIBRARY", SfuCampus.BURNABY);

        assertEquals(1, client.calls);
        assertEquals(first, second);
    }

    // MEET-SVC-05: the same search near another campus is a separate request
    @Test
    void differentCampusIsNotCached() {
        service.search("library", SfuCampus.BURNABY);
        service.search("library", SfuCampus.SURREY);

        assertEquals(2, client.calls);
        assertEquals(SfuCampus.SURREY, client.lastCampus);
    }

    // MEET-SVC-06: an API failure becomes a friendly message
    @Test
    void apiFailureBecomesFriendlyMessage() {
        client.fail = true;

        MeetupLocationException e = assertThrows(MeetupLocationException.class,
                () -> service.search("library", SfuCampus.BURNABY));
        assertTrue(e.getMessage().contains("Location search is unavailable right now"));
    }

    // MEET-SVC-07: a failed search is not cached, so the next attempt tries the API again
    @Test
    void failedSearchIsNotCached() {
        client.fail = true;
        assertThrows(MeetupLocationException.class, () -> service.search("library", SfuCampus.BURNABY));

        client.fail = false;
        service.search("library", SfuCampus.BURNABY);

        assertEquals(2, client.calls);
    }

    /** Fake Nominatim client that records calls instead of using the network. */
    static class FakeClient extends NominatimClient {
        int calls;
        String lastQuery;
        SfuCampus lastCampus;
        boolean fail;

        FakeClient() {
            super("http://unused.test", "test");
        }

        @Override
        public List<MeetupLocation> search(String query, SfuCampus campus) {
            calls++;
            lastQuery = query;
            lastCampus = campus;
            if (fail) {
                throw new ResourceAccessException("Nominatim unavailable");
            }
            return List.of(new MeetupLocation("W.A.C. Bennett Library",
                    "W.A.C. Bennett Library, University Drive, Burnaby", 49.2796, -122.9189));
        }
    }
}
package ca.sfu.cmpt276.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ca.sfu.cmpt276.marketplace.model.MeetupLocation;
import ca.sfu.cmpt276.marketplace.model.SfuCampus;
import ca.sfu.cmpt276.marketplace.service.NominatimClient;

/**
 * US-20 automated tests for the Nominatim API call. Uses MockRestServiceServer, so no real
 * network request is made and the tests pass offline.
 */
class NominatimClientTests {

    private static final String BASE_URL = "https://nominatim.test";
    private static final String USER_AGENT = "SFU-Marketplace-Test/1.0";

    private MockRestServiceServer server;
    private NominatimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NominatimClient(builder, BASE_URL, USER_AGENT);
    }

    // MEET-API-01: request is sent with the campus viewbox, jsonv2 format and our User-Agent
    @Test
    void sendsCorrectRequestAndParsesResults() {
        server.expect(requestTo(startsWith(BASE_URL + "/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("q", "library"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("bounded", "1"))
                .andExpect(queryParam("viewbox", SfuCampus.BURNABY.getViewbox()))
                .andExpect(header("User-Agent", USER_AGENT))
                .andRespond(withSuccess("""
                        [
                          {"place_id": 1, "name": "W.A.C. Bennett Library",
                           "display_name": "W.A.C. Bennett Library, University Drive, Burnaby",
                           "lat": "49.2796", "lon": "-122.9189"},
                          {"place_id": 2, "name": "Belzberg Library",
                           "display_name": "Belzberg Library, West Hastings Street, Vancouver",
                           "lat": "49.2848", "lon": "-123.1118"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<MeetupLocation> results = client.search("library", SfuCampus.BURNABY);

        server.verify();
        assertEquals(2, results.size());
        assertEquals("W.A.C. Bennett Library", results.get(0).name());
        assertEquals("W.A.C. Bennett Library, University Drive, Burnaby", results.get(0).address());
        assertEquals(49.2796, results.get(0).latitude(), 0.00001);
        assertEquals(-122.9189, results.get(0).longitude(), 0.00001);
        assertTrue(results.get(0).mapUrl().startsWith("https://www.openstreetmap.org/?mlat=49.2796"));
    }

    // MEET-API-02: Surrey searches use the Surrey viewbox
    @Test
    void usesSelectedCampusViewbox() {
        server.expect(requestTo(startsWith(BASE_URL + "/search")))
                .andExpect(queryParam("viewbox", SfuCampus.SURREY.getViewbox()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.search("cafe", SfuCampus.SURREY);

        server.verify();
    }

    // MEET-API-03: a result with no name falls back to the first part of its address
    @Test
    void fallsBackToAddressWhenNameMissing() {
        server.expect(requestTo(startsWith(BASE_URL + "/search")))
                .andRespond(withSuccess("""
                        [{"name": "", "display_name": "Production Way Station, Burnaby",
                          "lat": "49.2537", "lon": "-122.9181"}]
                        """, MediaType.APPLICATION_JSON));

        List<MeetupLocation> results = client.search("station", SfuCampus.BURNABY);

        assertEquals("Production Way Station", results.get(0).name());
    }

    // MEET-API-04: Nominatim returns no matches
    @Test
    void returnsEmptyListWhenNoMatches() {
        server.expect(requestTo(startsWith(BASE_URL + "/search")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.search("zzzz", SfuCampus.BURNABY).isEmpty());
    }

    // MEET-API-05: Nominatim server error is reported to the caller
    @Test
    void throwsWhenNominatimFails() {
        server.expect(requestTo(startsWith(BASE_URL + "/search")))
                .andRespond(withServerError());

        assertThrows(RestClientException.class, () -> client.search("library", SfuCampus.BURNABY));
    }
}
package ca.sfu.cmpt276.marketplace.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ca.sfu.cmpt276.marketplace.model.MeetupLocation;
import ca.sfu.cmpt276.marketplace.model.SfuCampus;

/**
 * Calls the OpenStreetMap Nominatim search API (https://nominatim.org/release-docs/latest/api/Search/).
 * Nominatim's usage policy requires an identifying User-Agent and at most one request per second;
 * the rate limit is enforced in MeetupLocationService.
 */
@Component
public class NominatimClient {

    static final int RESULT_LIMIT = 5;

    private final RestClient restClient;

    @Autowired
    public NominatimClient(
            @Value("${nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${nominatim.user-agent:SFU-Marketplace/1.0 (CMPT 276 Group 10; https://group-10-2hy5.onrender.com)}") String userAgent) {
        this(RestClient.builder(), baseUrl, userAgent);
    }

    /** Lets tests pass in a builder bound to MockRestServiceServer. */
    public NominatimClient(RestClient.Builder builder, String baseUrl, String userAgent) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    public List<MeetupLocation> search(String query, SfuCampus campus) {
        NominatimPlace[] places = restClient.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", RESULT_LIMIT)
                        .queryParam("viewbox", campus.getViewbox())
                        .queryParam("bounded", 1)
                        .build())
                .retrieve()
                .body(NominatimPlace[].class);

        List<MeetupLocation> results = new ArrayList<>();
        if (places == null) {
            return results;
        }
        for (NominatimPlace place : places) {
            if (place.lat() == null || place.lon() == null) {
                continue;
            }
            String address = place.display_name() == null ? "" : place.display_name();
            String name = place.name() == null || place.name().isBlank()
                    ? address.split(",")[0].trim()
                    : place.name();
            results.add(new MeetupLocation(name, address,
                    Double.parseDouble(place.lat()), Double.parseDouble(place.lon())));
        }
        return results;
    }

    /** The fields we use from Nominatim's jsonv2 response. Nominatim sends lat/lon as strings. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimPlace(String name, String display_name, String lat, String lon) {
    }
}
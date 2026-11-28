package ca.sfu.cmpt276.marketplace.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import ca.sfu.cmpt276.marketplace.model.MeetupLocation;
import ca.sfu.cmpt276.marketplace.model.SfuCampus;

@Service
public class MeetupLocationService {

    public static final int MAX_QUERY_LENGTH = 100;
    private static final long MIN_INTERVAL_MS = 1000; // Nominatim policy: max 1 request per second
    private static final int MAX_CACHE_ENTRIES = 200;

    private final NominatimClient client;
    private final Map<String, List<MeetupLocation>> cache = new ConcurrentHashMap<>();
    private long lastRequestAt = 0;

    public MeetupLocationService(NominatimClient client) {
        this.client = client;
    }

    public List<MeetupLocation> search(String query, SfuCampus campus) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            throw new MeetupLocationException("Enter a place to search for, such as \"library\" or \"cafe\".");
        }
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new MeetupLocationException("Search must be " + MAX_QUERY_LENGTH + " characters or fewer.");
        }

        // Repeated searches are served from memory so we don't send Nominatim duplicate requests.
        String key = campus.name() + "|" + trimmed.toLowerCase(Locale.ROOT);
        List<MeetupLocation> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            waitForRateLimit();
            List<MeetupLocation> results = List.copyOf(client.search(trimmed, campus));
            if (cache.size() >= MAX_CACHE_ENTRIES) {
                cache.clear();
            }
            cache.put(key, results);
            return results;
        } catch (RestClientException e) {
            throw new MeetupLocationException("Location search is unavailable right now. Please try again in a moment.");
        }
    }

    /** Used by tests so cached results from one test don't affect another. */
    public void clearCache() {
        cache.clear();
    }

    private synchronized void waitForRateLimit() {
        long wait = lastRequestAt + MIN_INTERVAL_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }
}
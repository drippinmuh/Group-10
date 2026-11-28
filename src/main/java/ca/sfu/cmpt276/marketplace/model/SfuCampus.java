package ca.sfu.cmpt276.marketplace.model;

import java.util.Locale;

/**
 * SFU campuses used to limit meetup-location searches to the area around each campus.
 * The viewbox is "west,south,east,north" (longitude,latitude,longitude,latitude),
 * the format Nominatim expects.
 */
public enum SfuCampus {
    BURNABY("Burnaby", "-122.9450,49.2680,-122.8950,49.2870"),
    SURREY("Surrey", "-122.8650,49.1780,-122.8350,49.1980"),
    VANCOUVER("Vancouver", "-123.1250,49.2780,-123.1000,49.2900");

    private final String displayName;
    private final String viewbox;

    SfuCampus(String displayName, String viewbox) {
        this.displayName = displayName;
        this.viewbox = viewbox;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getViewbox() {
        return viewbox;
    }

    /**
     * Matches free-text campus values such as "Burnaby" or "SFU Surrey".
     * Anything unrecognized falls back to Burnaby, SFU's main campus.
     */
    public static SfuCampus fromName(String value) {
        if (value == null || value.isBlank()) {
            return BURNABY;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (SfuCampus campus : values()) {
            if (lower.contains(campus.name().toLowerCase(Locale.ROOT))) {
                return campus;
            }
        }
        return BURNABY;
    }
}
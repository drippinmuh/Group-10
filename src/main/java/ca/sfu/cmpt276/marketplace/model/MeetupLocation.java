package ca.sfu.cmpt276.marketplace.model;

/**
 * A place returned by the OpenStreetMap Nominatim API that buyers and sellers can meet at.
 */
public record MeetupLocation(String name, String address, double latitude, double longitude) {

    public String mapUrl() {
        return "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude
                + "#map=18/" + latitude + "/" + longitude;
    }
}
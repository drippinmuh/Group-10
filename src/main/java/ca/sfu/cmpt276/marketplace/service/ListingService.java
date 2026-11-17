package ca.sfu.cmpt276.marketplace.service;

import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.model.ListingStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;


@Service
public class ListingService {

    private final UserAccountRepository userAccountRepository;
    private final ListingRepository repository;

    public ListingService(ListingRepository repository, UserAccountRepository userAccountRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
    }

    public Listing createListing(
        String sellerEmail,
        String title,
        String description,
        BigDecimal price,
        String category,
        String condition,
        String campus,
        String imageUrl
    ) {
        UserAccount seller = userAccountRepository.findByEmail(sellerEmail).orElseThrow(
            () -> new ListingException("Seller not found")
        );

        validate(title, description, price);

        Listing listing = new Listing(seller, title, description, price, category, condition, campus, imageUrl);
        return repository.save(listing);
    }

    public Listing updateListing(
        Long id,
        String email,
        String title,
        String description,
        BigDecimal price,
        String category,
        String condition,
        String campus,
        String imageUrl
    ) {
        Listing listing = getOwnedListing(id, email);
        validate(title, description, price);

        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setCategory(category);
        listing.setCondition(condition);
        listing.setCampus(campus);
        listing.setImageUrl(imageUrl);

        return repository.save(listing);
    }

    // Soft delete: preserve the row for related records.
    public void deleteListing(Long id, String email) {
        Listing listing = getOwnedListing(id, email);
        listing.setStatus(ListingStatus.REMOVED);
        repository.save(listing);
    }

    public void updateListingStatus(Long id, String email, String status) {
        Listing listing = getOwnedListing(id, email);
        ListingStatus newStatus;
        try {
            newStatus = ListingStatus.valueOf(status.toUpperCase(Locale.ROOT));
            if (newStatus == ListingStatus.REMOVED) {
                throw new ListingException("Invalid status: " + status);
            }
        } catch (IllegalArgumentException e) {
            throw new ListingException("Invalid status: " + status);
        }
        listing.setStatus(newStatus);
        repository.save(listing);
    }

    // Removed listings are treated as gone, so they 404 everywhere.
    public Listing getListingById(Long id) {
        return repository.findWithSellerById(id)
            .filter(listing -> listing.getStatus() != ListingStatus.REMOVED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Listing getOwnedListing(Long id, String email) {
        Listing listing = getListingById(id);
        if (!listing.getSeller().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return listing;
    }

    public List<Listing> getUserListings(String email) {
        UserAccount user = userAccountRepository.findByEmail(email).orElseThrow(
            () -> new ListingException("User not found")
        );
        return repository.findBySellerAndStatusNotOrderByCreatedAtDesc(user, ListingStatus.REMOVED);
    }

    // Keyword search and filters - Iteration 2.
    public List<Listing> searchListings(
        String keyword,
        String category,
        String condition,
        String campus,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        String search = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        

        return getAllListings().stream()
            .filter(listing -> search.isEmpty()
                || listing.getTitle().toLowerCase(Locale.ROOT).contains(search)
                || listing.getDescription().toLowerCase(Locale.ROOT).contains(search))
            .filter(listing -> category == null || category.isBlank()
                || category.trim().equalsIgnoreCase(listing.getCategory()))
            .filter(listing -> condition == null || condition.isBlank()
                || condition.trim().equalsIgnoreCase(listing.getCondition()))
            .filter(listing -> campus == null || campus.isBlank()
                || campus.trim().equalsIgnoreCase(listing.getCampus()))
            .filter(listing -> minPrice == null || listing.getPrice().compareTo(minPrice) >= 0)
            .filter(listing -> maxPrice == null || listing.getPrice().compareTo(maxPrice) <= 0)
            .toList();
    }



    public List<Listing> sortListings(List<Listing> listings, String sort) {
    if (sort == null) {
        return listings;
    }

    return switch (sort) {
        case "price_asc" -> listings.stream()
            .sorted(Comparator.comparing(Listing::getPrice))
            .toList();

        case "price_desc" -> listings.stream()
            .sorted(Comparator.comparing(Listing::getPrice).reversed())
            .toList();

        case "date_asc" -> listings.stream()
            .sorted(Comparator.comparing(Listing::getCreatedAt))
            .toList();

        case "date_desc" -> listings.stream()
            .sorted(Comparator.comparing(Listing::getCreatedAt).reversed())
            .toList();

        default -> listings;
    };
}

    private static void validate(String title, String description, BigDecimal price) {
        if (price == null) {
            throw new ListingException("Price cannot be null");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ListingException("Price cannot be negative");
        }
        if (title == null || title.isBlank()) {
            throw new ListingException("Title cannot be empty");
        }
        if (description == null || description.isBlank()) {
            throw new ListingException("Description cannot be empty");
        }
    }

    public List<Listing> getAllListings() {
        // Pagination later; return available listings newest first.
        return repository.findByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE);
    }
}

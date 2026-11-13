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
    ) 
    {
        UserAccount seller = userAccountRepository.findByEmail(sellerEmail).orElseThrow(
            () -> new ListingException("Seller not found")
        );
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
        
        Listing listing = new Listing(seller, title, description, price, category, condition, campus, imageUrl);

        return repository.save(listing);
    }

    public Listing getListingById(Long id) {
        return repository.findWithSellerById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND)
        );
    }

    public List<Listing> searchListings(String keyword, String category, String condition,
                                        String campus, BigDecimal minPrice, BigDecimal maxPrice) {
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

    public List<Listing> getAllListings() {
        // pagination later, for now just return all listings
        return repository.findByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE);
    }






}

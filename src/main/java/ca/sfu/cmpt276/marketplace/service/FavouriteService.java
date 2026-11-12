package ca.sfu.cmpt276.marketplace.service;

import ca.sfu.cmpt276.marketplace.model.Favourite;
import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingStatus;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.FavouriteRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FavouriteService {

    private final FavouriteRepository favouriteRepository;
    private final UserAccountRepository userRepository;
    private final ListingRepository listingRepository;

    public FavouriteService(FavouriteRepository favouriteRepository,
                            UserAccountRepository userRepository,
                            ListingRepository listingRepository) {
        this.favouriteRepository = favouriteRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    private UserAccount currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User account not found"));
    }

    @Transactional
    public void addFavourite(String email, Long listingId) {
        UserAccount user = currentUser(email);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Listing not found"));
        if (listing.getStatus() == ListingStatus.REMOVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }
        // Prevent repeat clicks from creating a second favourite.
        if (!favouriteRepository.existsByUser_IdAndListing_Id(user.getId(), listingId)) {
            favouriteRepository.save(new Favourite(user, listing));
        }
    }

    @Transactional
    public void removeFavourite(String email, Long listingId) {
        UserAccount user = currentUser(email);
        // Scope the query by the current user's ID: never delete someone else's favourite.
        favouriteRepository.findByUser_IdAndListing_Id(user.getId(), listingId)
                .ifPresent(favouriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFavourite(String email, Long listingId) {
        return userRepository.findByEmail(email)
                .map(user -> favouriteRepository.existsByUser_IdAndListing_Id(user.getId(), listingId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Favourite> getFavourites(String email) {
        UserAccount user = currentUser(email);
        return favouriteRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
    }
}

package ca.sfu.cmpt276.marketplace.repository;

import ca.sfu.cmpt276.marketplace.model.Favourite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavouriteRepository extends JpaRepository<Favourite, Long> {

    boolean existsByUser_IdAndListing_Id(Long userId, Long listingId);

    Optional<Favourite> findByUser_IdAndListing_Id(Long userId, Long listingId);

    @EntityGraph(attributePaths = "listing")
    List<Favourite> findByUser_IdOrderByCreatedAtDesc(Long userId);
}

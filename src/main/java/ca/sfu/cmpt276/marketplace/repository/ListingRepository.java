package ca.sfu.cmpt276.marketplace.repository;


import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingStatus;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;



public interface ListingRepository extends JpaRepository<Listing, Long> {
    @EntityGraph(attributePaths = "seller")
    Optional<Listing> findWithSellerById(Long id);

    @EntityGraph(attributePaths = "seller")
    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);
}

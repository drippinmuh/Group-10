package ca.sfu.cmpt276.marketplace.repository;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByListing_IdAndBuyer_Id(Long listingId, Long buyerId);

    /** Loads the listing, buyer and seller too, so pages can show them (open-in-view is off). */
    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    Optional<Conversation> findWithDetailsById(Long id);

    /** Every conversation the user is part of, most recent activity first. */
    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    List<Conversation> findByBuyer_IdOrSeller_IdOrderByLastMessageAtDesc(Long buyerId, Long sellerId);
}
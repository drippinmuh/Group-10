package ca.sfu.cmpt276.marketplace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A private conversation between one buyer and the seller about one listing.
 * Each buyer has at most one conversation per listing.
 */
@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(
        name = "uk_conversations_listing_buyer", columnNames = {"listing_id", "buyer_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserAccount buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserAccount seller;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastMessageAt;

    protected Conversation() {
    }

    public Conversation(Listing listing, UserAccount buyer) {
        this.listing = listing;
        this.buyer = buyer;
        this.seller = listing.getSeller();
        this.createdAt = Instant.now();
        this.lastMessageAt = this.createdAt;
    }

    /** True if this user is the buyer or the seller in this conversation. */
    public boolean isParticipant(String email) {
        return buyer.getEmail().equals(email) || seller.getEmail().equals(email);
    }

    public void recordMessageAt(Instant sentAt) {
        this.lastMessageAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public UserAccount getBuyer() {
        return buyer;
    }

    public UserAccount getSeller() {
        return seller;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }
}

package ca.sfu.cmpt276.marketplace.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "listing_reports")
public class ListingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ListingReport() {
    }

    public ListingReport(
            Listing listing,
            UserAccount reporter,
            String reason
    ) {
        this.listing = listing;
        this.reporter = reporter;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public UserAccount getReporter() {
        return reporter;
    }

    public String getReason() {
        return reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
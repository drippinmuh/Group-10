package ca.sfu.cmpt276.marketplace.repository;

import ca.sfu.cmpt276.marketplace.model.ListingReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingReportRepository
        extends JpaRepository<ListingReport, Long> {

    @EntityGraph(attributePaths = {"listing", "reporter"})
    List<ListingReport> findAllByOrderByCreatedAtDesc();
}
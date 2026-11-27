
package ca.sfu.cmpt276.marketplace.service;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingReport;
import ca.sfu.cmpt276.marketplace.model.ReportStatus;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.ListingReportRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ListingReportService {

    private final ListingReportRepository reportRepository;
    private final ListingService listingService;
    private final UserAccountRepository userRepository;

    public ListingReportService(
            ListingReportRepository reportRepository,
            ListingService listingService,
            UserAccountRepository userRepository) {
        this.reportRepository = reportRepository;
        this.listingService = listingService;
        this.userRepository = userRepository;
    }

    public ListingReport submitReport(
            Long listingId,
            String reporterEmail,
            String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ListingException("Report reason cannot be empty");
        }

        String trimmedReason = reason.trim();

        if (trimmedReason.length() > 500) {
            throw new ListingException(
                    "Report reason cannot exceed 500 characters");
        }

        Listing listing = listingService.getListingById(listingId);

        UserAccount reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Reporter not found"));

        ListingReport report =
                new ListingReport(listing, reporter, trimmedReason);

        return reportRepository.save(report);
    }

    public List<ListingReport> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    public void resolveReport(Long reportId) {
        ListingReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found"));

        report.setStatus(ReportStatus.RESOLVED);
        reportRepository.save(report);
    }
}

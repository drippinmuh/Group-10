
package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingReport;
import ca.sfu.cmpt276.marketplace.model.ReportStatus;
import ca.sfu.cmpt276.marketplace.repository.FavouriteRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingReportRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ListingReportService;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ListingReportTests {

    private static final String SELLER = "seller@sfu.ca";
    private static final String REPORTER = "reporter@sfu.ca";
    private static final String ADMIN = "admin@sfu.ca";

    @Autowired private MockMvc mockMvc;
    @Autowired private ListingReportRepository reportRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private FavouriteRepository favouriteRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private UserAccountService userService;
    @Autowired private ListingService listingService;
    @Autowired private ListingReportService reportService;

    private Listing listing;

    private void clearDatabase() {
        // Delete dependent records before listings and users.
        reportRepository.deleteAll();
        favouriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        clearDatabase();

        userService.register("Test Seller", SELLER, "password123");
        userService.register("Test Reporter", REPORTER, "password123");

        listing = listingService.createListing(
                SELLER,
                "Desk Lamp",
                "Working desk lamp",
                new BigDecimal("15.00"),
                "Furniture",
                "Good",
                "Burnaby",
                null
        );
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    private String reportUrl() {
        return "/listings/" + listing.getId() + "/report";
    }

    private ListingReport createReport() {
        return reportService.submitReport(
                listing.getId(), REPORTER, "Suspicious listing");
    }

    // REPORT-01
    @Test
    void userCanSubmitReport() throws Exception {
        mockMvc.perform(post(reportUrl())
                        .with(user(REPORTER).roles("USER"))
                        .with(csrf())
                        .param("reason", "  Suspicious listing  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/listings/" + listing.getId()))
                .andExpect(flash().attribute(
                        "message", "Report submitted successfully."));

        assertEquals(1, reportRepository.count());

        ListingReport report = reportRepository.findAllByOrderByCreatedAtDesc().get(0);

        assertEquals("Suspicious listing", report.getReason());
        assertEquals(ReportStatus.PENDING, report.getStatus());
        assertEquals(REPORTER, report.getReporter().getEmail());
        assertEquals(listing.getId(), report.getListing().getId());
        assertNotNull(report.getCreatedAt());
    }

    // REPORT-02
    @Test
    void anonymousUserCannotSubmitReport() throws Exception {
        mockMvc.perform(post(reportUrl())
                        .with(csrf())
                        .param("reason", "Suspicious listing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertEquals(0, reportRepository.count());
    }

    // REPORT-03
    @Test
    void reportWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post(reportUrl())
                        .with(user(REPORTER).roles("USER"))
                        .param("reason", "Suspicious listing"))
                .andExpect(status().isForbidden());

        assertEquals(0, reportRepository.count());
    }

    // REPORT-04
    @Test
    void emptyReasonIsRejected() throws Exception {
        mockMvc.perform(post(reportUrl())
                        .with(user(REPORTER).roles("USER"))
                        .with(csrf())
                        .param("reason", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "error", "Report reason cannot be empty"));

        assertEquals(0, reportRepository.count());
    }

    // REPORT-05
    @Test
    void reasonLongerThan500CharactersIsRejected() throws Exception {
        mockMvc.perform(post(reportUrl())
                        .with(user(REPORTER).roles("USER"))
                        .with(csrf())
                        .param("reason", "x".repeat(501)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "error",
                        "Report reason cannot exceed 500 characters"));

        assertEquals(0, reportRepository.count());
    }

    // REPORT-06
    @Test
    void unknownListingCannotBeReported() throws Exception {
        mockMvc.perform(post("/listings/999999/report")
                        .with(user(REPORTER).roles("USER"))
                        .with(csrf())
                        .param("reason", "Suspicious listing"))
                .andExpect(status().isNotFound());

        assertEquals(0, reportRepository.count());
    }

    // REPORT-07
    @Test
    void removedListingCannotBeReported() throws Exception {
        listingService.adminRemoveListing(listing.getId());

        mockMvc.perform(post(reportUrl())
                        .with(user(REPORTER).roles("USER"))
                        .with(csrf())
                        .param("reason", "Suspicious listing"))
                .andExpect(status().isNotFound());

        assertEquals(0, reportRepository.count());
    }

    // REPORT-08
    @Test
    void reportFormVisibleToLoggedInUser() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId())
                        .with(user(REPORTER).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Submit Report")));
    }

    // REPORT-09
    @Test
    void reportFormHiddenFromGuests() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        not(containsString("Submit Report"))));
    }

    // REPORT-10
    @Test
    void adminCanViewReports() throws Exception {
        createReport();

        mockMvc.perform(get("/admin/reports")
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-reports"))
                .andExpect(content().string(
                        containsString("Desk Lamp")))
                .andExpect(content().string(
                        containsString("Suspicious listing")))
                .andExpect(content().string(
                        containsString(REPORTER)));
    }

    // REPORT-11
    @Test
    void regularUserCannotViewReports() throws Exception {
        mockMvc.perform(get("/admin/reports")
                        .with(user(REPORTER).roles("USER")))
                .andExpect(status().isForbidden());
    }

    // REPORT-12
    @Test
    void anonymousUserCannotViewReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // REPORT-13
    @Test
    void adminCanResolveReport() throws Exception {
        ListingReport report = createReport();

        mockMvc.perform(post(
                        "/admin/reports/" + report.getId() + "/resolve")
                        .with(user(ADMIN).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reports"))
                .andExpect(flash().attribute(
                        "message", "Report marked as resolved."));

        ListingReport updated = reportRepository.findById(
                report.getId()).orElseThrow();

        assertEquals(ReportStatus.RESOLVED, updated.getStatus());
    }

    // REPORT-14
    @Test
    void regularUserCannotResolveReport() throws Exception {
        ListingReport report = createReport();

        mockMvc.perform(post(
                        "/admin/reports/" + report.getId() + "/resolve")
                        .with(user(REPORTER).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertEquals(
                ReportStatus.PENDING,
                reportRepository.findById(report.getId())
                        .orElseThrow().getStatus()
        );
    }

    // REPORT-15
    @Test
    void resolveWithoutCsrfIsForbidden() throws Exception {
        ListingReport report = createReport();

        mockMvc.perform(post(
                        "/admin/reports/" + report.getId() + "/resolve")
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertEquals(
                ReportStatus.PENDING,
                reportRepository.findById(report.getId())
                        .orElseThrow().getStatus()
        );
    }

    // REPORT-16
    @Test
    void resolvingUnknownReportReturnsNotFound() throws Exception {
        mockMvc.perform(post("/admin/reports/999999/resolve")
                        .with(user(ADMIN).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // REPORT-17
    @Test
    void adminReportsPageShowsEmptyState() throws Exception {
        mockMvc.perform(get("/admin/reports")
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("No reports have been submitted yet")));
    }

    // REPORT-18
    @Test
    void removedListingIsNotLinkedInReportsPage() throws Exception {
        createReport();
        listingService.adminRemoveListing(listing.getId());

        mockMvc.perform(get("/admin/reports")
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Desk Lamp (Removed)")))
                .andExpect(content().string(
                        not(containsString(
                                "href=\"/listings/" + listing.getId() + "\""))));
    }
}


package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.service.ListingException;
import ca.sfu.cmpt276.marketplace.service.ListingReportService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportController {

    private final ListingReportService reportService;

    public ReportController(ListingReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/listings/{id}/report")
    public String submitReport(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication auth,
            RedirectAttributes redirect) {

        try {
            reportService.submitReport(id, auth.getName(), reason);
        } catch (ListingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/listings/" + id;
        }

        redirect.addFlashAttribute(
                "message", "Report submitted successfully.");

        return "redirect:/listings/" + id;
    }

    @GetMapping("/admin/reports")
    public String viewReports(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        return "admin-reports";
    }

    @PostMapping("/admin/reports/{id}/resolve")
    public String resolveReport(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        reportService.resolveReport(id);

        redirect.addFlashAttribute(
                "message", "Report marked as resolved.");

        return "redirect:/admin/reports";
    }
}

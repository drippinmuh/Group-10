package ca.sfu.cmpt276.marketplace;

import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.ui.Model;
import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.service.ListingService;

import ca.sfu.cmpt276.marketplace.service.ListingException;

@Controller
public class ListingsController {

    private final ListingService service;

    public ListingsController(ListingService service) {
        this.service = service;
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("listings", service.getAllListings());
        return "listings";
    }

    @PostMapping("/listings")
    public String create(
                    @RequestParam String title,
                    @RequestParam String description,
                    @RequestParam BigDecimal price,
                    @RequestParam(required = false) String category,
                    @RequestParam(required = false) String condition,
                    @RequestParam(required = false) String campus,
                    @RequestParam(required = false) String imageUrl,
                    Authentication auth,
                    Model model
    ) 
    {
        String email = auth.getName();

        Listing saved = null;

        try {
             saved = service.createListing(email, title, description, price, category, condition, campus, imageUrl);
        } catch (ListingException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("description", description);
            model.addAttribute("price", price);
            model.addAttribute("category", category);
            model.addAttribute("condition", condition);
            model.addAttribute("campus", campus);
            model.addAttribute("imageUrl", imageUrl);
            return "listing-form";
        }
        return "redirect:/listings/" + saved.getId();
    }

    @GetMapping("/listings/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("listing", service.getListingById(id));
        return "listing-details";
    }

    @GetMapping("/listings/new")
    public String createForm(Model model) {
        return "listing-form";
    }

    @GetMapping("/listings/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        Listing listing = service.getOwnedListing(id, auth.getName());
        return editForm(model, id, null, listing.getTitle(), listing.getDescription(), listing.getPrice(),
                listing.getCategory(), listing.getCondition(), listing.getCampus(), listing.getImageUrl());
    }

    @PostMapping("/listings/{id}/edit")
    public String update(
                    @PathVariable Long id,
                    @RequestParam String title,
                    @RequestParam String description,
                    @RequestParam BigDecimal price,
                    @RequestParam(required = false) String category,
                    @RequestParam(required = false) String condition,
                    @RequestParam(required = false) String campus,
                    @RequestParam(required = false) String imageUrl,
                    Authentication auth,
                    Model model,
                    RedirectAttributes redirect
    )
    {
        try {
            service.updateListing(id, auth.getName(), title, description, price, category, condition, campus, imageUrl);
        } catch (ListingException e) {
            return editForm(model, id, e.getMessage(), title, description, price, category, condition, campus, imageUrl);
        }
        redirect.addFlashAttribute("message", "Listing updated.");
        return "redirect:/listings/" + id;
    }

    private static String editForm(Model model, Long id, String error, String title, String description,
                                   BigDecimal price, String category, String condition, String campus, String imageUrl) {
        model.addAttribute("listingId", id);
        model.addAttribute("error", error);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("price", price);
        model.addAttribute("category", category);
        model.addAttribute("condition", condition);
        model.addAttribute("campus", campus);
        model.addAttribute("imageUrl", imageUrl);
        return "listing-form";
    }

}

package ca.sfu.cmpt276.marketplace;

import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String listings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        model.addAttribute("listings", service.searchListings(keyword, category, condition, campus, minPrice, maxPrice));
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("condition", condition);
        model.addAttribute("campus", campus);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

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
    
}

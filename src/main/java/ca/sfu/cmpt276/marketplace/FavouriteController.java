package ca.sfu.cmpt276.marketplace;

import ca.sfu.cmpt276.marketplace.service.FavouriteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FavouriteController {

    private final FavouriteService favouriteService;

    public FavouriteController(FavouriteService favouriteService) {
        this.favouriteService = favouriteService;
    }

    @GetMapping("/favourites")
    public String myFavourites(Authentication auth, Model model) {
        model.addAttribute("favourites", favouriteService.getFavourites(auth.getName()));
        return "favourites";
    }

    @PostMapping("/favourites/{listingId}")
    public String add(@PathVariable Long listingId, Authentication auth) {
        favouriteService.addFavourite(auth.getName(), listingId);
        return "redirect:/listings/" + listingId;
    }

    @PostMapping("/favourites/{listingId}/remove")
    public String remove(@PathVariable Long listingId, Authentication auth,
                         @RequestParam(defaultValue = "details") String returnTo) {
        favouriteService.removeFavourite(auth.getName(), listingId);
        // Only support two fixed destinations to avoid untrusted redirect URLs.
        return "favourites".equals(returnTo)
                ? "redirect:/favourites"
                : "redirect:/listings/" + listingId;
    }
}

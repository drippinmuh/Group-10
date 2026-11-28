package ca.sfu.cmpt276.marketplace;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.marketplace.model.SfuCampus;
import ca.sfu.cmpt276.marketplace.service.MeetupLocationException;
import ca.sfu.cmpt276.marketplace.service.MeetupLocationService;

/**
 * US-20: search for meetup spots near an SFU campus using the OpenStreetMap Nominatim API.
 * Requires login (covered by anyRequest().authenticated() in SecurityConfig), which also
 * keeps anonymous traffic away from the public Nominatim server.
 */
@Controller
public class MeetupLocationController {

    private final MeetupLocationService service;

    public MeetupLocationController(MeetupLocationService service) {
        this.service = service;
    }

    @GetMapping("/meetup-locations")
    public String search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) Long listingId,
            Model model) {

        SfuCampus selected = SfuCampus.fromName(campus);
        model.addAttribute("q", q);
        model.addAttribute("campus", selected);
        model.addAttribute("campuses", SfuCampus.values());
        model.addAttribute("listingId", listingId);

        if (q != null) {
            try {
                model.addAttribute("results", service.search(q, selected));
            } catch (MeetupLocationException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "meetup-locations";
    }
}
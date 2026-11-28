package ca.sfu.cmpt276.marketplace;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.service.ConversationThread;
import ca.sfu.cmpt276.marketplace.service.MessagingException;
import ca.sfu.cmpt276.marketplace.service.MessagingService;

/**
 * Buyer-seller messaging. All routes are under /messages, which SecurityConfig's
 * anyRequest().authenticated() rule already restricts to logged-in users.
 */
@Controller
public class MessageController {

    /** Messages are stored in UTC; show them in Vancouver time. */
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("MMM d, h:mm a", Locale.CANADA)
            .withZone(ZoneId.of("America/Vancouver"));

    private final MessagingService messagingService;

    public MessageController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    /** US-21: send the seller a message from the listing page. */
    @PostMapping("/messages/contact")
    public String contactSeller(@RequestParam Long listingId,
                                @RequestParam(required = false) String body,
                                Authentication auth,
                                RedirectAttributes redirect) {
        try {
            Conversation conversation = messagingService.contactSeller(listingId, auth.getName(), body);
            redirect.addFlashAttribute("message", "Message sent.");
            return "redirect:/messages/" + conversation.getId();
        } catch (MessagingException e) {
            redirect.addFlashAttribute("contactError", e.getMessage());
            redirect.addFlashAttribute("contactBody", body);
            return "redirect:/listings/" + listingId;
        }
    }

    @GetMapping("/messages/{id}")
    public String conversation(@PathVariable Long id, Authentication auth, Model model) {
        ConversationThread thread = messagingService.getConversation(id, auth.getName());
        Conversation conversation = thread.conversation();
        UserAccount otherParty = conversation.getBuyer().getEmail().equals(auth.getName())
                ? conversation.getSeller()
                : conversation.getBuyer();

        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", thread.messages());
        model.addAttribute("otherParty", otherParty);
        model.addAttribute("timeFormat", TIME_FORMAT);
        return "conversation";
    }
}
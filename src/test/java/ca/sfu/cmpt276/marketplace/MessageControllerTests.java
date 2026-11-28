package ca.sfu.cmpt276.marketplace;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.repository.ConversationRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.MessageRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.MessagingService;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

/** US-21: Contact Seller — pages, redirects and access rules. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTests {

    private static final String SELLER = "seller@sfu.ca";
    private static final String BUYER = "buyer@sfu.ca";
    private static final String OUTSIDER = "outsider@sfu.ca";

    @Autowired private MockMvc mockMvc;
    @Autowired private MessagingService messagingService;
    @Autowired private ListingService listingService;
    @Autowired private UserAccountService userService;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private UserAccountRepository userRepository;

    private Listing listing;

    @BeforeEach
    void seed() {
        clearData();
        userService.register("Sam Seller", SELLER, "password123");
        userService.register("Bea Buyer", BUYER, "password123");
        userService.register("Olly Outsider", OUTSIDER, "password123");
        listing = listingService.createListing(SELLER, "CMPT 276 Textbook", "Lightly used",
                new BigDecimal("25.00"), "Textbooks", "Good", "Burnaby", null);
    }

    @AfterEach
    void clearData() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    // CONTACT-01: sending a message redirects to the new conversation with a confirmation
    @Test
    void contactSellerRedirectsToConversation() throws Exception {
        MvcResult result = mockMvc.perform(post("/messages/contact")
                        .param("listingId", listing.getId().toString())
                        .param("body", "Is this still available?")
                        .with(user(BUYER)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "Message sent."))
                .andReturn();

        Conversation conversation = conversationRepository.findAll().get(0);
        assertEquals("/messages/" + conversation.getId(), result.getResponse().getRedirectedUrl());
        assertEquals(1, messageRepository.count());
    }

    // CONTACT-02: the buyer sees the message, listing and seller on the conversation page
    @Test
    void buyerSeesConversation() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Is this still available?");

        mockMvc.perform(get("/messages/" + c.getId()).with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Is this still available?")))
                .andExpect(content().string(containsString("CMPT 276 Textbook")))
                .andExpect(content().string(containsString("Sam Seller")));
    }

    // CONTACT-03: the seller can open the same conversation
    @Test
    void sellerSeesConversation() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Is this still available?");

        mockMvc.perform(get("/messages/" + c.getId()).with(user(SELLER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Is this still available?")))
                .andExpect(content().string(containsString("Bea Buyer")));
    }

    // CONTACT-04: other users cannot open the conversation
    @Test
    void outsiderIsForbidden() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Private");

        mockMvc.perform(get("/messages/" + c.getId()).with(user(OUTSIDER)))
                .andExpect(status().isForbidden());
    }

    // CONTACT-05: logged-out visitors are sent to login when opening a conversation
    @Test
    void loggedOutCannotViewConversation() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Private");

        mockMvc.perform(get("/messages/" + c.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // CONTACT-06: logged-out visitors cannot send messages
    @Test
    void loggedOutCannotSend() throws Exception {
        mockMvc.perform(post("/messages/contact")
                        .param("listingId", listing.getId().toString())
                        .param("body", "Hi")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertEquals(0, messageRepository.count());
    }

    // CONTACT-07: requests without a CSRF token are rejected
    @Test
    void sendWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/messages/contact")
                        .param("listingId", listing.getId().toString())
                        .param("body", "Hi")
                        .with(user(BUYER)))
                .andExpect(status().isForbidden());
        assertEquals(0, messageRepository.count());
    }

    // CONTACT-08: a blank message returns to the listing with an error, keeping what was typed
    @Test
    void blankMessageShowsErrorOnListing() throws Exception {
        mockMvc.perform(post("/messages/contact")
                        .param("listingId", listing.getId().toString())
                        .param("body", "   ")
                        .with(user(BUYER)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/" + listing.getId()))
                .andExpect(flash().attribute("contactError", "Write a message before sending."))
                .andExpect(flash().attribute("contactBody", "   "));
        assertEquals(0, conversationRepository.count());
    }

    // CONTACT-09: the seller messaging their own listing gets an error
    @Test
    void sellerMessagingOwnListingShowsError() throws Exception {
        mockMvc.perform(post("/messages/contact")
                        .param("listingId", listing.getId().toString())
                        .param("body", "Hello me")
                        .with(user(SELLER)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/" + listing.getId()))
                .andExpect(flash().attribute("contactError", "You can't message yourself about your own listing."));
        assertEquals(0, conversationRepository.count());
    }

    // CONTACT-10: the message box shows for other logged-in users, not for the seller
    @Test
    void messageBoxShownToBuyersOnly() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId()).with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Message the seller")))
                .andExpect(content().string(containsString("/messages/contact")));

        mockMvc.perform(get("/listings/" + listing.getId()).with(user(SELLER)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Message the seller"))));
    }

    // CONTACT-11: logged-out visitors are prompted to log in instead
    @Test
    void loggedOutSeesLoginPrompt() throws Exception {
        mockMvc.perform(get("/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("to message the seller")))
                .andExpect(content().string(not(containsString("/messages/contact"))));
    }

    // CONTACT-12: a sold listing shows a note instead of the form
    @Test
    void soldListingHidesForm() throws Exception {
        listingService.updateListingStatus(listing.getId(), SELLER, "SOLD");

        mockMvc.perform(get("/listings/" + listing.getId()).with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This item has been sold.")))
                .andExpect(content().string(not(containsString("/messages/contact"))));
    }

    // CONTACT-13: HTML typed into a message is displayed as text, not run
    @Test
    void messageHtmlIsEscaped() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "<script>alert('x')</script>");

        mockMvc.perform(get("/messages/" + c.getId()).with(user(SELLER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert"))));
    }

    // CONTACT-14: a conversation that doesn't exist returns 404
    @Test
    void missingConversationReturnsNotFound() throws Exception {
        mockMvc.perform(get("/messages/999999").with(user(BUYER)))
                .andExpect(status().isNotFound());
    }
}
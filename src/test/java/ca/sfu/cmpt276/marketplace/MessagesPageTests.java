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

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.repository.ConversationRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.MessageRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.MessagingService;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

/** US-22: Messages inbox and replies — pages, redirects and access rules. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MessagesPageTests {

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

    // INBOXPAGE-01: an empty inbox shows a friendly message
    @Test
    void emptyInboxShowsMessage() throws Exception {
        mockMvc.perform(get("/messages").with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No messages yet.")));
    }

    // INBOXPAGE-02: the buyer's inbox lists the conversation with the seller and a "You:" preview
    @Test
    void buyerInboxListsConversation() throws Exception {
        messagingService.contactSeller(listing.getId(), BUYER, "Is this available?");

        mockMvc.perform(get("/messages").with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sam Seller")))
                .andExpect(content().string(containsString("CMPT 276 Textbook")))
                .andExpect(content().string(containsString("You: Is this available?")))
                .andExpect(content().string(containsString("Select a conversation to start chatting.")));
    }

    // INBOXPAGE-03: the seller's inbox lists the same conversation with the buyer, no "You:" prefix
    @Test
    void sellerInboxListsConversation() throws Exception {
        messagingService.contactSeller(listing.getId(), BUYER, "Is this available?");

        mockMvc.perform(get("/messages").with(user(SELLER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bea Buyer")))
                .andExpect(content().string(not(containsString("You: Is this available?"))));
    }

    // INBOXPAGE-04: users never see other people's conversations in their inbox
    @Test
    void inboxExcludesOtherPeoplesConversations() throws Exception {
        messagingService.contactSeller(listing.getId(), BUYER, "Private question");

        mockMvc.perform(get("/messages").with(user(OUTSIDER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No messages yet.")))
                .andExpect(content().string(not(containsString("Private question"))));
    }

    // INBOXPAGE-05: logged-out visitors are sent to login
    @Test
    void loggedOutInboxRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/messages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // INBOXPAGE-06: the navbar shows a Messages link only to logged-in users
    @Test
    void navbarMessagesLinkOnlyWhenLoggedIn() throws Exception {
        mockMvc.perform(get("/listings").with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/messages\"")));

        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/messages\""))));
    }

    // INBOXPAGE-07: opening a conversation shows the chat, the reply box and a link to the listing
    @Test
    void openConversationShowsChatAndReplyBox() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Is this available?");

        mockMvc.perform(get("/messages/" + c.getId()).with(user(SELLER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Is this available?")))
                .andExpect(content().string(containsString("/messages/" + c.getId() + "/reply")))
                .andExpect(content().string(containsString("View listing")));
    }

    // REPLYPAGE-01: replying saves the message and returns to the same conversation
    @Test
    void replyRedirectsBackToConversation() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Is this available?");

        mockMvc.perform(post("/messages/" + c.getId() + "/reply")
                        .param("body", "Yes, it is!")
                        .with(user(SELLER)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/" + c.getId()));

        assertEquals(2, messageRepository.count());
        mockMvc.perform(get("/messages/" + c.getId()).with(user(BUYER)))
                .andExpect(content().string(containsString("Yes, it is!")));
    }

    // REPLYPAGE-02: a blank reply shows an error and keeps what was typed
    @Test
    void blankReplyShowsError() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Hi");

        mockMvc.perform(post("/messages/" + c.getId() + "/reply")
                        .param("body", "   ")
                        .with(user(SELLER)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages/" + c.getId()))
                .andExpect(flash().attribute("replyError", "Write a message before sending."))
                .andExpect(flash().attribute("replyBody", "   "));
        assertEquals(1, messageRepository.count());
    }

    // REPLYPAGE-03: outsiders cannot reply
    @Test
    void outsiderReplyIsForbidden() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Hi");

        mockMvc.perform(post("/messages/" + c.getId() + "/reply")
                        .param("body", "Let me in")
                        .with(user(OUTSIDER)).with(csrf()))
                .andExpect(status().isForbidden());
        assertEquals(1, messageRepository.count());
    }

    // REPLYPAGE-04: replies without a CSRF token are rejected
    @Test
    void replyWithoutCsrfIsRejected() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Hi");

        mockMvc.perform(post("/messages/" + c.getId() + "/reply")
                        .param("body", "Hello")
                        .with(user(SELLER)))
                .andExpect(status().isForbidden());
        assertEquals(1, messageRepository.count());
    }

    // REPLYPAGE-05: logged-out visitors cannot reply
    @Test
    void loggedOutReplyRedirectsToLogin() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Hi");

        mockMvc.perform(post("/messages/" + c.getId() + "/reply")
                        .param("body", "Hello")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertEquals(1, messageRepository.count());
    }

    // REPLYPAGE-06: a conversation about a removed listing is closed
    @Test
    void removedListingClosesConversation() throws Exception {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Hi");
        listingService.adminRemoveListing(listing.getId());

        mockMvc.perform(get("/messages/" + c.getId()).with(user(BUYER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("so this conversation is closed")))
                .andExpect(content().string(not(containsString("/messages/" + c.getId() + "/reply"))));

        mockMvc.perform(post("/messages/" + c.getId() + "/reply")
                        .param("body", "Hello?")
                        .with(user(BUYER)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("replyError",
                        "This listing has been removed, so this conversation is closed."));
        assertEquals(1, messageRepository.count());
    }

    // REPLYPAGE-07: replying to a conversation that doesn't exist returns 404
    @Test
    void replyToMissingConversationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/messages/999999/reply")
                        .param("body", "Hello")
                        .with(user(BUYER)).with(csrf()))
                .andExpect(status().isNotFound());
    }
}

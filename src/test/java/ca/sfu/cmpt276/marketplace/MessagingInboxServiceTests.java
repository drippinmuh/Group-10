package ca.sfu.cmpt276.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.repository.ConversationRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.MessageRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ConversationSummary;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.MessagingException;
import ca.sfu.cmpt276.marketplace.service.MessagingService;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

/** US-22: Messages inbox and replies — service rules. */
@ActiveProfiles("test")
@SpringBootTest
class MessagingInboxServiceTests {

    private static final String SELLER = "seller@sfu.ca";
    private static final String BUYER = "buyer@sfu.ca";
    private static final String OUTSIDER = "outsider@sfu.ca";

    @Autowired private MessagingService messagingService;
    @Autowired private ListingService listingService;
    @Autowired private UserAccountService userService;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private UserAccountRepository userRepository;

    private Listing textbook;
    private Listing lamp;

    @BeforeEach
    void seed() {
        clearData();
        userService.register("Sam Seller", SELLER, "password123");
        userService.register("Bea Buyer", BUYER, "password123");
        userService.register("Olly Outsider", OUTSIDER, "password123");
        textbook = listingService.createListing(SELLER, "CMPT 276 Textbook", "Lightly used",
                new BigDecimal("25.00"), "Textbooks", "Good", "Burnaby", null);
        lamp = listingService.createListing(SELLER, "Desk Lamp", "LED lamp",
                new BigDecimal("10.00"), "Furniture", "Good", "Surrey", null);
    }

    @AfterEach
    void clearData() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    // INBOX-01: the buyer sees the conversation, who it's with, and the latest message
    @Test
    void buyerInboxShowsConversation() throws Exception {
        messagingService.contactSeller(textbook.getId(), BUYER, "Is this available?");
        Thread.sleep(20);
        messagingService.contactSeller(textbook.getId(), BUYER, "I can pay cash");

        List<ConversationSummary> inbox = messagingService.getInbox(BUYER);

        assertEquals(1, inbox.size());
        assertEquals(SELLER, inbox.get(0).otherParty().getEmail());
        assertEquals("CMPT 276 Textbook", inbox.get(0).conversation().getListing().getTitle());
        assertEquals("I can pay cash", inbox.get(0).lastMessage().getBody());
    }

    // INBOX-02: the seller sees the same conversation with the buyer as the other party
    @Test
    void sellerInboxShowsConversation() {
        messagingService.contactSeller(textbook.getId(), BUYER, "Is this available?");

        List<ConversationSummary> inbox = messagingService.getInbox(SELLER);

        assertEquals(1, inbox.size());
        assertEquals(BUYER, inbox.get(0).otherParty().getEmail());
    }

    // INBOX-03: newest activity first; replying moves a conversation to the top
    @Test
    void inboxIsNewestFirst() throws Exception {
        Conversation older = messagingService.contactSeller(textbook.getId(), BUYER, "About the textbook");
        Thread.sleep(20);
        messagingService.contactSeller(lamp.getId(), BUYER, "About the lamp");

        assertEquals("Desk Lamp", messagingService.getInbox(BUYER).get(0).conversation().getListing().getTitle());

        Thread.sleep(20);
        messagingService.reply(older.getId(), SELLER, "Still available!");

        assertEquals("CMPT 276 Textbook", messagingService.getInbox(BUYER).get(0).conversation().getListing().getTitle());
    }

    // INBOX-04: no conversations means an empty inbox
    @Test
    void emptyInbox() {
        assertTrue(messagingService.getInbox(BUYER).isEmpty());
    }

    // INBOX-05: users never see other people's conversations
    @Test
    void outsiderInboxExcludesOthers() {
        messagingService.contactSeller(textbook.getId(), BUYER, "Private");

        assertTrue(messagingService.getInbox(OUTSIDER).isEmpty());
    }

    // REPLY-01: the seller can reply, and it becomes the latest message
    @Test
    void sellerCanReply() throws Exception {
        Conversation c = messagingService.contactSeller(textbook.getId(), BUYER, "Is this available?");
        Thread.sleep(20);

        messagingService.reply(c.getId(), SELLER, "  Yes, it is!  ");

        assertEquals(2, messageRepository.count());
        ConversationSummary row = messagingService.getInbox(BUYER).get(0);
        assertEquals("Yes, it is!", row.lastMessage().getBody());
        assertEquals(SELLER, row.lastMessage().getSender().getEmail());
    }

    // REPLY-02: the buyer can reply in the conversation too
    @Test
    void buyerCanReply() {
        Conversation c = messagingService.contactSeller(textbook.getId(), BUYER, "Is this available?");

        messagingService.reply(c.getId(), BUYER, "Also, does it have notes in it?");

        assertEquals(2, messageRepository.count());
    }

    // REPLY-03: outsiders cannot reply
    @Test
    void outsiderCannotReply() {
        Conversation c = messagingService.contactSeller(textbook.getId(), BUYER, "Private");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> messagingService.reply(c.getId(), OUTSIDER, "Hi"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        assertEquals(1, messageRepository.count());
    }

    // REPLY-04: blank replies are rejected
    @Test
    void blankReplyIsRejected() {
        Conversation c = messagingService.contactSeller(textbook.getId(), BUYER, "Hi");

        assertThrows(MessagingException.class, () -> messagingService.reply(c.getId(), SELLER, "   "));
        assertEquals(1, messageRepository.count());
    }

    // REPLY-05: replying to a missing conversation returns 404
    @Test
    void replyToMissingConversation() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> messagingService.reply(999999L, BUYER, "Hi"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // REPLY-06: conversations about removed listings are closed
    @Test
    void replyClosedWhenListingRemoved() {
        Conversation c = messagingService.contactSeller(textbook.getId(), BUYER, "Hi");
        listingService.adminRemoveListing(textbook.getId());

        MessagingException e = assertThrows(MessagingException.class,
                () -> messagingService.reply(c.getId(), BUYER, "Hello?"));
        assertEquals("This listing has been removed, so this conversation is closed.", e.getMessage());
    }

    // REPLY-07: replies still work after the item is sold
    @Test
    void replyAllowedAfterSale() {
        Conversation c = messagingService.contactSeller(textbook.getId(), BUYER, "I'll take it");
        listingService.updateListingStatus(textbook.getId(), SELLER, "SOLD");

        messagingService.reply(c.getId(), SELLER, "Great, see you at the library at 3");

        assertEquals(2, messageRepository.count());
    }
}

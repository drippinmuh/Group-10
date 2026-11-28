package ca.sfu.cmpt276.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ca.sfu.cmpt276.marketplace.model.Message;
import ca.sfu.cmpt276.marketplace.repository.ConversationRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.MessageRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import ca.sfu.cmpt276.marketplace.service.ConversationThread;
import ca.sfu.cmpt276.marketplace.service.ListingService;
import ca.sfu.cmpt276.marketplace.service.MessagingException;
import ca.sfu.cmpt276.marketplace.service.MessagingService;
import ca.sfu.cmpt276.marketplace.service.UserAccountService;

/** US-21: Contact Seller — service rules. */
@ActiveProfiles("test")
@SpringBootTest
class MessagingServiceTests {

    private static final String SELLER = "seller@sfu.ca";
    private static final String BUYER = "buyer@sfu.ca";
    private static final String OTHER_BUYER = "other@sfu.ca";

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
        userService.register("Seller", SELLER, "password123");
        userService.register("Buyer", BUYER, "password123");
        userService.register("Other Buyer", OTHER_BUYER, "password123");
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

    // MSG-01: first message creates a conversation with the right people, text trimmed
    @Test
    void firstMessageStartsConversation() {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "  Is this still available?  ");

        ConversationThread thread = messagingService.getConversation(c.getId(), BUYER);
        assertEquals(BUYER, thread.conversation().getBuyer().getEmail());
        assertEquals(SELLER, thread.conversation().getSeller().getEmail());
        assertEquals(listing.getId(), thread.conversation().getListing().getId());
        assertEquals(1, thread.messages().size());
        assertEquals("Is this still available?", thread.messages().get(0).getBody());
        assertEquals(BUYER, thread.messages().get(0).getSender().getEmail());
    }

    // MSG-02: messaging again about the same listing reuses the conversation
    @Test
    void repeatMessageReusesConversation() {
        Conversation first = messagingService.contactSeller(listing.getId(), BUYER, "Hi!");
        Conversation second = messagingService.contactSeller(listing.getId(), BUYER, "Would you take $20?");

        assertEquals(first.getId(), second.getId());
        assertEquals(1, conversationRepository.count());
        assertEquals(2, messageRepository.count());
    }

    // MSG-03: different buyers get separate conversations
    @Test
    void differentBuyersGetSeparateConversations() {
        Conversation a = messagingService.contactSeller(listing.getId(), BUYER, "Hi from buyer");
        Conversation b = messagingService.contactSeller(listing.getId(), OTHER_BUYER, "Hi from other");

        assertEquals(2, conversationRepository.count());
        assertEquals(false, a.getId().equals(b.getId()));
    }

    // MSG-04: the seller cannot message their own listing
    @Test
    void sellerCannotMessageOwnListing() {
        MessagingException e = assertThrows(MessagingException.class,
                () -> messagingService.contactSeller(listing.getId(), SELLER, "Hello me"));
        assertEquals("You can't message yourself about your own listing.", e.getMessage());
        assertEquals(0, conversationRepository.count());
    }

    // MSG-05: a blank message is rejected and nothing is saved
    @Test
    void blankMessageIsRejected() {
        assertThrows(MessagingException.class,
                () -> messagingService.contactSeller(listing.getId(), BUYER, "   "));
        assertEquals(0, conversationRepository.count());
        assertEquals(0, messageRepository.count());
    }

    // MSG-06: 1000 characters is accepted, 1001 is rejected
    @Test
    void messageLengthLimit() {
        messagingService.contactSeller(listing.getId(), BUYER, "a".repeat(Message.MAX_LENGTH));
        assertThrows(MessagingException.class,
                () -> messagingService.contactSeller(listing.getId(), BUYER, "a".repeat(Message.MAX_LENGTH + 1)));
        assertEquals(1, messageRepository.count());
    }

    // MSG-07: removed listings return 404
    @Test
    void removedListingReturnsNotFound() {
        listingService.adminRemoveListing(listing.getId());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> messagingService.contactSeller(listing.getId(), BUYER, "Hi"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // MSG-08: a listing that doesn't exist returns 404
    @Test
    void missingListingReturnsNotFound() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> messagingService.contactSeller(999999L, BUYER, "Hi"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // MSG-09: no new conversations about a sold item
    @Test
    void cannotStartConversationAboutSoldItem() {
        listingService.updateListingStatus(listing.getId(), SELLER, "SOLD");

        assertThrows(MessagingException.class,
                () -> messagingService.contactSeller(listing.getId(), BUYER, "Still available?"));
        assertEquals(0, conversationRepository.count());
    }

    // MSG-10: an existing conversation can continue after the item is sold
    @Test
    void existingConversationContinuesAfterSale() {
        messagingService.contactSeller(listing.getId(), BUYER, "I'll take it");
        listingService.updateListingStatus(listing.getId(), SELLER, "SOLD");

        messagingService.contactSeller(listing.getId(), BUYER, "Thanks, see you tomorrow!");

        assertEquals(2, messageRepository.count());
    }

    // MSG-11: buyer and seller can both open the conversation; messages are oldest first
    @Test
    void participantsCanViewConversationInOrder() {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "First");
        messagingService.contactSeller(listing.getId(), BUYER, "Second");

        List<Message> forBuyer = messagingService.getConversation(c.getId(), BUYER).messages();
        List<Message> forSeller = messagingService.getConversation(c.getId(), SELLER).messages();

        assertEquals("First", forBuyer.get(0).getBody());
        assertEquals("Second", forBuyer.get(1).getBody());
        assertEquals(2, forSeller.size());
    }

    // MSG-12: anyone else gets 403
    @Test
    void outsiderCannotViewConversation() {
        Conversation c = messagingService.contactSeller(listing.getId(), BUYER, "Private message");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> messagingService.getConversation(c.getId(), OTHER_BUYER));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }

    // MSG-13: a conversation that doesn't exist returns 404
    @Test
    void missingConversationReturnsNotFound() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> messagingService.getConversation(999999L, BUYER));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
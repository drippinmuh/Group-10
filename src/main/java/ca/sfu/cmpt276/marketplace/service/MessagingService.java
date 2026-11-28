package ca.sfu.cmpt276.marketplace.service;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.Listing;
import ca.sfu.cmpt276.marketplace.model.ListingStatus;
import ca.sfu.cmpt276.marketplace.model.Message;
import ca.sfu.cmpt276.marketplace.model.UserAccount;
import ca.sfu.cmpt276.marketplace.repository.ConversationRepository;
import ca.sfu.cmpt276.marketplace.repository.ListingRepository;
import ca.sfu.cmpt276.marketplace.repository.MessageRepository;
import ca.sfu.cmpt276.marketplace.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final UserAccountRepository userRepository;

    public MessagingService(ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            ListingRepository listingRepository,
                            UserAccountRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /**
     * US-21: a buyer sends the seller a message about a listing. Reuses the buyer's existing
     * conversation for this listing, or starts a new one.
     */
    @Transactional
    public Conversation contactSeller(Long listingId, String buyerEmail, String text) {
        UserAccount buyer = currentUser(buyerEmail);
        Listing listing = listingRepository.findWithSellerById(listingId)
                .filter(l -> l.getStatus() != ListingStatus.REMOVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        String body = cleanBody(text);

        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new MessagingException("You can't message yourself about your own listing.");
        }

        Conversation conversation = conversationRepository
                .findByListing_IdAndBuyer_Id(listingId, buyer.getId())
                .orElse(null);
        if (conversation == null) {
            if (listing.getStatus() == ListingStatus.SOLD) {
                throw new MessagingException("This item has been sold, so you can't start a new conversation about it.");
            }
            conversation = conversationRepository.save(new Conversation(listing, buyer));
        }

        addMessage(conversation, buyer, body);
        return conversation;
    }

    /** Loads a conversation and its messages. Only the buyer and the seller may see it. */
    @Transactional(readOnly = true)
    public ConversationThread getConversation(Long conversationId, String email) {
        Conversation conversation = conversationRepository.findWithDetailsById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.isParticipant(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return new ConversationThread(conversation,
                messageRepository.findByConversation_IdOrderBySentAtAscIdAsc(conversationId));
    }

    private void addMessage(Conversation conversation, UserAccount sender, String body) {
        Message message = messageRepository.save(new Message(conversation, sender, body));
        conversation.recordMessageAt(message.getSentAt());
    }

    private String cleanBody(String text) {
        String body = text == null ? "" : text.trim();
        if (body.isEmpty()) {
            throw new MessagingException("Write a message before sending.");
        }
        if (body.length() > Message.MAX_LENGTH) {
            throw new MessagingException("Messages must be " + Message.MAX_LENGTH + " characters or fewer.");
        }
        return body;
    }

    private UserAccount currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));
    }
}
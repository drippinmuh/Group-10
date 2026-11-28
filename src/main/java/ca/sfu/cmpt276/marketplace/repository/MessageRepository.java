package ca.sfu.cmpt276.marketplace.repository;

import ca.sfu.cmpt276.marketplace.model.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Oldest first, with each sender loaded so the page can show who wrote what. */
    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversation_IdOrderBySentAtAscIdAsc(Long conversationId);

    /** The newest message in a conversation, for the inbox preview. */
    @EntityGraph(attributePaths = "sender")
    Optional<Message> findFirstByConversation_IdOrderBySentAtDescIdDesc(Long conversationId);
}
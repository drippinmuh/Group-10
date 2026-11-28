package ca.sfu.cmpt276.marketplace.service;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.Message;
import ca.sfu.cmpt276.marketplace.model.UserAccount;

/** One row in the Messages inbox: the conversation, who it's with, and the latest message. */
public record ConversationSummary(Conversation conversation, UserAccount otherParty, Message lastMessage) {
}

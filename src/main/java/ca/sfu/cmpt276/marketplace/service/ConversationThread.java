package ca.sfu.cmpt276.marketplace.service;

import java.util.List;

import ca.sfu.cmpt276.marketplace.model.Conversation;
import ca.sfu.cmpt276.marketplace.model.Message;

/** A conversation together with its messages, oldest first. */
public record ConversationThread(Conversation conversation, List<Message> messages) {
}
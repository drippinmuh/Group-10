package ca.sfu.cmpt276.marketplace.service;

/** A messaging rule the user can fix, such as an empty message. Shown on the page as an error. */
public class MessagingException extends RuntimeException {

    public MessagingException(String message) {
        super(message);
    }
}
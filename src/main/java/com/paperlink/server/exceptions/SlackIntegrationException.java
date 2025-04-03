package com.paperlink.server.exceptions;

/**
 * Exception thrown when there's an issue with Slack integration
 */
public class SlackIntegrationException extends RuntimeException {

    public SlackIntegrationException(String message) {
        super(message);
    }

    public SlackIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}



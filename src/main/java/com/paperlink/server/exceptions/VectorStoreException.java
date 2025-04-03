package com.paperlink.server.exceptions;

/**
 * Exception thrown when operations on the vector store fail.
 */
public class VectorStoreException extends RuntimeException {

    /**
     * Creates a new vector store exception with the specified message.
     *
     * @param message Error message
     */
    public VectorStoreException(String message) {
        super(message);
    }

    /**
     * Creates a new vector store exception with the specified message and cause.
     *
     * @param message Error message
     * @param cause Root cause of the exception
     */
    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
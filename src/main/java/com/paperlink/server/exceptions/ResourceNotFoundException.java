package com.paperlink.server.exceptions;

// Resource not found exception
public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}

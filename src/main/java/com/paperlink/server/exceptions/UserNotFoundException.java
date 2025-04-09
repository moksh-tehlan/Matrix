package com.paperlink.server.exceptions;

// User related exception
public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String message) {
    super(message);
  }
}

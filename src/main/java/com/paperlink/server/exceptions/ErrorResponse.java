package com.paperlink.server.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

// Error response class
@Setter
@Getter
@Data
@AllArgsConstructor
public class ErrorResponse {
  private int status;
  private String message;
  private long timestamp;
}

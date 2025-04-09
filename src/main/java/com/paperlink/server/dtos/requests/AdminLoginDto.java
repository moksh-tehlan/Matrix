package com.paperlink.server.dtos.requests;

import jakarta.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginDto {

  @NotNull(message = "Admin email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotNull(message = "Admin password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;
}

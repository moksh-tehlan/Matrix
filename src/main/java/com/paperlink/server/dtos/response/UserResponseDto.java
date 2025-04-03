package com.paperlink.server.dtos.response;

import com.paperlink.server.dtos.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private String id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private boolean isVerified;

    private UserRole role;
}

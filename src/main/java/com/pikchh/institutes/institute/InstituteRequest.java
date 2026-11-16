package com.pikchh.institutes.institute;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstituteRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "must contain only letters, numbers, dots, underscores, or hyphens")
        String code,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 2048) String website,
        @Size(max = 500) String address
) {
}

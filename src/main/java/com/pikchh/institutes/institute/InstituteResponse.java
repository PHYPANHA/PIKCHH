package com.pikchh.institutes.institute;

import java.time.Instant;
import java.util.UUID;

public record InstituteResponse(
        UUID id,
        String name,
        String code,
        String email,
        String phone,
        String website,
        String address,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstituteResponse from(Institute institute) {
        return new InstituteResponse(
                institute.getId(),
                institute.getName(),
                institute.getCode(),
                institute.getEmail(),
                institute.getPhone(),
                institute.getWebsite(),
                institute.getAddress(),
                institute.getCreatedAt(),
                institute.getUpdatedAt()
        );
    }
}

package com.pikchh.institutes.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class LibraryModels {

    private LibraryModels() {
    }

    public record BookRequest(
            @NotNull UUID instituteId,
            @NotBlank @Size(max = 20) String isbn,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 150) String author,
            @NotNull @PositiveOrZero Integer totalCopies
    ) {
    }

    public record LoanRequest(
            @NotNull UUID bookId,
            @NotNull UUID studentId,
            @NotNull LocalDate dueDate
    ) {
    }

    public record BookResponse(
            UUID id,
            UUID instituteId,
            String instituteName,
            String isbn,
            String title,
            String author,
            int totalCopies,
            int availableCopies
    ) {
    }

    public record LoanResponse(
            UUID id,
            UUID bookId,
            String bookTitle,
            UUID studentId,
            String studentNumber,
            String studentName,
            Instant checkedOutAt,
            LocalDate dueDate,
            Instant returnedAt
    ) {
    }
}

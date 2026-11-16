package com.pikchh.institutes.academics;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class AcademicModels {

    private AcademicModels() {
    }

    public record CourseLevelRequest(
            @NotNull UUID instituteId,
            @NotBlank @Size(max = 30)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$")
            String code,
            @NotBlank @Size(max = 100) String name
    ) {
    }

    public record CourseRequest(
            @NotBlank @Size(max = 30)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$")
            String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull UUID levelId
    ) {
    }

    public record ClassRequest(
            @NotNull UUID courseId,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 20) String academicYear
    ) {
    }

    public record StudentRequest(
            @NotNull UUID instituteId,
            @NotBlank @Size(max = 40)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$")
            String studentNumber,
            @NotBlank @Size(max = 150) String name,
            @Email @Size(max = 254) String email
    ) {
    }

    public record EnrollmentRequest(@NotNull UUID studentId) {
    }

    public record AssessmentRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull @Positive @Digits(integer = 6, fraction = 2) BigDecimal maxScore
    ) {
    }

    public record ScoreRequest(
            @NotNull UUID studentId,
            @NotNull @DecimalMin("0.0") @Digits(integer = 6, fraction = 2) BigDecimal score
    ) {
    }

    public record CourseLevelResponse(UUID id, UUID instituteId, String instituteName, String code, String name) {
    }

    public record CourseResponse(
            UUID id,
            String code,
            String name,
            UUID levelId,
            String levelName,
            UUID instituteId,
            String instituteName
    ) {
    }

    public record ClassResponse(
            UUID id,
            UUID courseId,
            String courseName,
            String instituteName,
            String name,
            String academicYear
    ) {
    }

    public record StudentResponse(
            UUID id,
            String studentNumber,
            String name,
            String email,
            UUID instituteId,
            String instituteName
    ) {
    }

    public record EnrollmentResponse(UUID studentId, String studentNumber, String name, String email) {
    }

    public record AssessmentResponse(UUID id, UUID classId, String name, BigDecimal maxScore) {
    }

    public record ScoreResponse(
            UUID studentId,
            String studentNumber,
            String studentName,
            UUID assessmentId,
            String assessmentName,
            BigDecimal maxScore,
            BigDecimal score
    ) {
    }
}

package com.pikchh.institutes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID instituteId;

    @BeforeEach
    void cleanDatabase() {
        clearManagementData();
        instituteId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO institutes (id, name, code, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, instituteId, "Test Institute", "TEST-INSTITUTE", now, now);
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        clearManagementData();
    }

    private void clearManagementData() {
        jdbc.update("DELETE FROM library_loans");
        jdbc.update("DELETE FROM library_books");
        jdbc.update("DELETE FROM student_scores");
        jdbc.update("DELETE FROM assessments");
        jdbc.update("DELETE FROM class_enrollments");
        jdbc.update("DELETE FROM course_classes");
        jdbc.update("DELETE FROM courses");
        jdbc.update("DELETE FROM course_levels");
        jdbc.update("DELETE FROM students");
        jdbc.update("DELETE FROM institutes");
    }

    @Test
    void enrollsStudentsAndManagesAssessmentScores() throws Exception {
        JsonNode level = create("/api/v1/course-levels",
                "{\"instituteId\":\"" + instituteId + "\",\"code\":\"primary\",\"name\":\"Primary\"}");
        UUID levelId = UUID.fromString(level.get("id").asText());

        JsonNode course = create("/api/v1/courses",
                "{\"code\":\"math-1\",\"name\":\"Mathematics\",\"levelId\":\"" + levelId + "\"}");
        JsonNode createdClass = create("/api/v1/classes",
                "{\"courseId\":\"" + course.get("id").asText() + "\",\"name\":\"Section A\",\"academicYear\":\"2026-2027\"}");
        UUID classId = UUID.fromString(createdClass.get("id").asText());
        JsonNode student = create("/api/v1/students", """
                {"studentNumber":"S-100","name":"Ada Student","email":"ada@example.org","instituteId":"%s"}
                """.formatted(instituteId));
        UUID studentId = UUID.fromString(student.get("id").asText());

        mockMvc.perform(post("/api/v1/classes/" + classId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("S-100"));

        UUID otherInstitute = insertInstitute("Other Institute", "OTHER-INSTITUTE");
        JsonNode otherStudent = create("/api/v1/students",
                "{\"studentNumber\":\"S-101\",\"name\":\"Other Student\",\"instituteId\":\"" + otherInstitute + "\"}");
        mockMvc.perform(post("/api/v1/classes/" + classId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + otherStudent.get("id").asText() + "\"}"))
                .andExpect(status().isBadRequest());

        UUID assessmentId = UUID.fromString(create("/api/v1/classes/" + classId + "/assessments",
                "{\"name\":\"Quiz 1\",\"maxScore\":20}").get("id").asText());
        mockMvc.perform(put("/api/v1/assessments/" + assessmentId + "/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + studentId + "\",\"score\":17.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(17.5));

        mockMvc.perform(get("/api/v1/classes/" + classId + "/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Ada Student"))
                .andExpect(jsonPath("$[0].score").value(17.5));

        mockMvc.perform(put("/api/v1/assessments/" + assessmentId + "/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + studentId + "\",\"score\":21}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void libraryCheckoutTracksAvailabilityAndReturn() throws Exception {
        JsonNode student = create("/api/v1/students", """
                {"studentNumber":"S-200","name":"Lin Reader","instituteId":"%s"}
                """.formatted(instituteId));
        UUID studentId = UUID.fromString(student.get("id").asText());
        JsonNode book = create("/api/v1/library/books", """
                {"instituteId":"%s","isbn":"978-0-00-000001-1","title":"Learning Java","author":"A. Writer","totalCopies":1}
                """.formatted(instituteId));
        UUID bookId = UUID.fromString(book.get("id").asText());
        String dueDate = LocalDate.now().plusDays(14).toString();
        UUID otherInstitute = insertInstitute("Other Institute", "OTHER-INSTITUTE");
        JsonNode otherStudent = create("/api/v1/students",
                "{\"studentNumber\":\"S-201\",\"name\":\"Other Reader\",\"instituteId\":\"" + otherInstitute + "\"}");
        mockMvc.perform(post("/api/v1/library/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + bookId + "\",\"studentId\":\""
                                + otherStudent.get("id").asText() + "\",\"dueDate\":\"" + dueDate + "\"}"))
                .andExpect(status().isBadRequest());

        JsonNode loan = create("/api/v1/library/loans",
                "{\"bookId\":\"" + bookId + "\",\"studentId\":\"" + studentId + "\",\"dueDate\":\"" + dueDate + "\"}");
        UUID loanId = UUID.fromString(loan.get("id").asText());
        mockMvc.perform(get("/api/v1/library/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableCopies").value(0));

        mockMvc.perform(post("/api/v1/library/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + bookId + "\",\"studentId\":\"" + studentId + "\",\"dueDate\":\"" + dueDate + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/library/loans/" + loanId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/library/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableCopies").value(1));
    }

    private JsonNode create(String path, String json) throws Exception {
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private UUID insertInstitute(String name, String code) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO institutes (id, name, code, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, name, code, now, now);
        return id;
    }
}

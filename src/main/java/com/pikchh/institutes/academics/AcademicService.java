package com.pikchh.institutes.academics;

import com.pikchh.institutes.institute.DomainConflictException;
import com.pikchh.institutes.institute.DomainNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.pikchh.institutes.academics.AcademicModels.*;

@Service
@Transactional
public class AcademicService {

    private final JdbcTemplate jdbc;

    public AcademicService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CourseLevelResponse> findCourseLevels() {
        return jdbc.query("""
                SELECT l.id, l.institute_id, i.name AS institute_name, l.code, l.name
                FROM course_levels l JOIN institutes i ON i.id = l.institute_id
                ORDER BY i.name, l.name
                """, (rs, row) -> new CourseLevelResponse(uuid(rs, "id"), uuid(rs, "institute_id"),
                rs.getString("institute_name"), rs.getString("code"), rs.getString("name")));
    }

    public CourseLevelResponse createCourseLevel(CourseLevelRequest request) {
        String instituteName = requiredName("institutes", request.instituteId(), "Institute");
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO course_levels (id, institute_id, code, name, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, request.instituteId(), normalizeCode(request.code()), request.name().trim(), Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("Course level code already exists for this institute: " + request.code());
        }
        return new CourseLevelResponse(id, request.instituteId(), instituteName,
                normalizeCode(request.code()), request.name().trim());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findCourses() {
        return jdbc.query("""
                SELECT c.id, c.code, c.name, l.id AS level_id, l.name AS level_name,
                       i.id AS institute_id, i.name AS institute_name
                FROM courses c
                JOIN course_levels l ON l.id = c.level_id
                JOIN institutes i ON i.id = l.institute_id
                ORDER BY i.name, c.name
                """, (rs, row) -> new CourseResponse(uuid(rs, "id"), rs.getString("code"),
                rs.getString("name"), uuid(rs, "level_id"), rs.getString("level_name"),
                uuid(rs, "institute_id"), rs.getString("institute_name")));
    }

    public CourseResponse createCourse(CourseRequest request) {
        LevelInfo level = jdbc.query("""
                SELECT l.name AS level_name, i.id AS institute_id, i.name AS institute_name
                FROM course_levels l JOIN institutes i ON i.id = l.institute_id
                WHERE l.id = ?
                """, (rs, row) -> new LevelInfo(rs.getString("level_name"),
                uuid(rs, "institute_id"), rs.getString("institute_name")), request.levelId()).stream()
                .findFirst().orElseThrow(() -> new DomainNotFoundException("Course level not found: " + request.levelId()));
        UUID id = UUID.randomUUID();
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        try {
            jdbc.update("INSERT INTO courses (id, code, name, level_id, created_at) VALUES (?, ?, ?, ?, ?)",
                    id, code, name, request.levelId(), Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("Course code already exists: " + code);
        }
        return new CourseResponse(id, code, name, request.levelId(), level.name(),
                level.instituteId(), level.instituteName());
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> findClasses() {
        return jdbc.query("""
                SELECT cl.id, c.id AS course_id, c.name AS course_name, i.name AS institute_name,
                       cl.name, cl.academic_year
                FROM course_classes cl
                JOIN courses c ON c.id = cl.course_id
                JOIN course_levels l ON l.id = c.level_id
                JOIN institutes i ON i.id = l.institute_id
                ORDER BY i.name, c.name, cl.academic_year, cl.name
                """, (rs, row) -> classResponse(rs));
    }

    public ClassResponse createClass(ClassRequest request) {
        CourseInfo course = jdbc.query("""
                SELECT c.name AS course_name, i.name AS institute_name
                FROM courses c
                JOIN course_levels l ON l.id = c.level_id
                JOIN institutes i ON i.id = l.institute_id
                WHERE c.id = ?
                """, (rs, row) -> new CourseInfo(rs.getString("course_name"),
                rs.getString("institute_name")), request.courseId()).stream()
                .findFirst().orElseThrow(() -> new DomainNotFoundException("Course not found: " + request.courseId()));
        UUID id = UUID.randomUUID();
        String name = request.name().trim();
        String year = request.academicYear().trim();
        try {
            jdbc.update("""
                    INSERT INTO course_classes (id, course_id, name, academic_year, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, request.courseId(), name, year, Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("This class already exists for the course and academic year");
        }
        return new ClassResponse(id, request.courseId(), course.name(), course.instituteName(), name, year);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findStudents() {
        return jdbc.query("""
                SELECT s.id, s.student_number, s.name, s.email, s.institute_id, i.name AS institute_name
                FROM students s JOIN institutes i ON i.id = s.institute_id ORDER BY i.name, s.name
                """, (rs, row) -> studentResponse(rs));
    }

    public StudentResponse createStudent(StudentRequest request) {
        String instituteName = requiredName("institutes", request.instituteId(), "Institute");
        UUID id = UUID.randomUUID();
        String number = request.studentNumber().trim().toUpperCase();
        String name = request.name().trim();
        String email = normalizeOptional(request.email());
        try {
            jdbc.update("""
                    INSERT INTO students (id, institute_id, student_number, name, email, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, request.instituteId(), number, name, email, Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("Student number already exists for this institute: " + number);
        }
        return new StudentResponse(id, number, name, email, request.instituteId(), instituteName);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findClassStudents(UUID classId) {
        requireExists("course_classes", classId, "Class");
        return jdbc.query("""
                SELECT s.id, s.student_number, s.name, s.email
                FROM class_enrollments e JOIN students s ON s.id = e.student_id
                WHERE e.class_id = ? ORDER BY s.name
                """, (rs, row) -> new EnrollmentResponse(uuid(rs, "id"), rs.getString("student_number"),
                rs.getString("name"), rs.getString("email")), classId);
    }

    public EnrollmentResponse enrollStudent(UUID classId, EnrollmentRequest request) {
        requireExists("course_classes", classId, "Class");
        StudentResponse student = findStudent(request.studentId());
        UUID classInstituteId = jdbc.queryForObject("""
                SELECT l.institute_id
                FROM course_classes cl
                JOIN courses c ON c.id = cl.course_id
                JOIN course_levels l ON l.id = c.level_id
                WHERE cl.id = ?
                """, UUID.class, classId);
        if (!classInstituteId.equals(student.instituteId())) {
            throw new IllegalArgumentException("Student and class must belong to the same institute");
        }
        try {
            jdbc.update("""
                    INSERT INTO class_enrollments (id, class_id, student_id, enrolled_at)
                    VALUES (?, ?, ?, ?)
                    """, UUID.randomUUID(), classId, request.studentId(), Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("Student is already enrolled in this class");
        }
        return new EnrollmentResponse(student.id(), student.studentNumber(), student.name(), student.email());
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> findAssessments(UUID classId) {
        requireExists("course_classes", classId, "Class");
        return jdbc.query("""
                SELECT id, class_id, name, max_score FROM assessments
                WHERE class_id = ? ORDER BY name
                """, (rs, row) -> new AssessmentResponse(uuid(rs, "id"), uuid(rs, "class_id"),
                rs.getString("name"), rs.getBigDecimal("max_score")), classId);
    }

    public AssessmentResponse createAssessment(UUID classId, AssessmentRequest request) {
        requireExists("course_classes", classId, "Class");
        UUID id = UUID.randomUUID();
        String name = request.name().trim();
        try {
            jdbc.update("""
                    INSERT INTO assessments (id, class_id, name, max_score, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, classId, name, request.maxScore(), Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("An assessment with this name already exists in the class");
        }
        return new AssessmentResponse(id, classId, name, request.maxScore());
    }

    @Transactional(readOnly = true)
    public List<ScoreResponse> findScores(UUID classId) {
        requireExists("course_classes", classId, "Class");
        return jdbc.query("""
                SELECT s.id AS student_id, s.student_number, s.name AS student_name,
                       a.id AS assessment_id, a.name AS assessment_name, a.max_score, sc.score
                FROM class_enrollments e
                JOIN students s ON s.id = e.student_id
                CROSS JOIN assessments a
                LEFT JOIN student_scores sc ON sc.assessment_id = a.id AND sc.student_id = s.id
                WHERE e.class_id = ? AND a.class_id = ?
                ORDER BY s.name, a.name
                """, (rs, row) -> new ScoreResponse(uuid(rs, "student_id"), rs.getString("student_number"),
                rs.getString("student_name"), uuid(rs, "assessment_id"), rs.getString("assessment_name"),
                rs.getBigDecimal("max_score"), rs.getBigDecimal("score")), classId, classId);
    }

    public ScoreResponse recordScore(UUID assessmentId, ScoreRequest request) {
        AssessmentResponse assessment = findAssessment(assessmentId);
        if (!isEnrolled(assessment.classId(), request.studentId())) {
            throw new IllegalArgumentException("Student must be enrolled in the class before receiving a score");
        }
        if (request.score().compareTo(assessment.maxScore()) > 0) {
            throw new IllegalArgumentException("Score cannot be greater than the assessment maximum");
        }

        int updated = jdbc.update("""
                UPDATE student_scores SET score = ?, updated_at = ?
                WHERE assessment_id = ? AND student_id = ?
                """, request.score(), Instant.now(), assessmentId, request.studentId());
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO student_scores (id, assessment_id, student_id, score, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), assessmentId, request.studentId(), request.score(), Instant.now());
        }
        StudentResponse student = findStudent(request.studentId());
        return new ScoreResponse(student.id(), student.studentNumber(), student.name(), assessment.id(),
                assessment.name(), assessment.maxScore(), request.score());
    }

    private StudentResponse findStudent(UUID id) {
        return jdbc.query("""
                SELECT s.id, s.student_number, s.name, s.email, s.institute_id, i.name AS institute_name
                FROM students s JOIN institutes i ON i.id = s.institute_id WHERE s.id = ?
                """, (rs, row) -> studentResponse(rs), id).stream().findFirst()
                .orElseThrow(() -> new DomainNotFoundException("Student not found: " + id));
    }

    private AssessmentResponse findAssessment(UUID id) {
        return jdbc.query("""
                SELECT id, class_id, name, max_score FROM assessments WHERE id = ?
                """, (rs, row) -> new AssessmentResponse(uuid(rs, "id"), uuid(rs, "class_id"),
                rs.getString("name"), rs.getBigDecimal("max_score")), id).stream().findFirst()
                .orElseThrow(() -> new DomainNotFoundException("Assessment not found: " + id));
    }

    private boolean isEnrolled(UUID classId, UUID studentId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM class_enrollments WHERE class_id = ? AND student_id = ?
                """, Integer.class, classId, studentId) > 0;
    }

    private String requiredName(String table, UUID id, String label) {
        requireExists(table, id, label);
        return jdbc.queryForObject("SELECT name FROM " + table + " WHERE id = ?", String.class, id);
    }

    private void requireExists(String table, UUID id, String label) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id) == 0) {
            throw new DomainNotFoundException(label + " not found: " + id);
        }
    }

    private static ClassResponse classResponse(ResultSet rs) throws SQLException {
        return new ClassResponse(uuid(rs, "id"), uuid(rs, "course_id"), rs.getString("course_name"),
                rs.getString("institute_name"), rs.getString("name"), rs.getString("academic_year"));
    }

    private static StudentResponse studentResponse(ResultSet rs) throws SQLException {
        return new StudentResponse(uuid(rs, "id"), rs.getString("student_number"),
                rs.getString("name"), rs.getString("email"), uuid(rs, "institute_id"),
                rs.getString("institute_name"));
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record LevelInfo(String name, UUID instituteId, String instituteName) {
    }

    private record CourseInfo(String name, String instituteName) {
    }
}

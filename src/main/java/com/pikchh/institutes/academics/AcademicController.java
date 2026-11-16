package com.pikchh.institutes.academics;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.pikchh.institutes.academics.AcademicModels.*;

@RestController
@RequestMapping("/api/v1")
public class AcademicController {

    private final AcademicService service;

    public AcademicController(AcademicService service) {
        this.service = service;
    }

    @GetMapping("/course-levels")
    public List<CourseLevelResponse> findCourseLevels() {
        return service.findCourseLevels();
    }

    @PostMapping("/course-levels")
    public CourseLevelResponse createCourseLevel(@Valid @RequestBody CourseLevelRequest request) {
        return service.createCourseLevel(request);
    }

    @GetMapping("/courses")
    public List<CourseResponse> findCourses() {
        return service.findCourses();
    }

    @PostMapping("/courses")
    public CourseResponse createCourse(@Valid @RequestBody CourseRequest request) {
        return service.createCourse(request);
    }

    @GetMapping("/classes")
    public List<ClassResponse> findClasses() {
        return service.findClasses();
    }

    @PostMapping("/classes")
    public ClassResponse createClass(@Valid @RequestBody ClassRequest request) {
        return service.createClass(request);
    }

    @GetMapping("/students")
    public List<StudentResponse> findStudents() {
        return service.findStudents();
    }

    @PostMapping("/students")
    public StudentResponse createStudent(@Valid @RequestBody StudentRequest request) {
        return service.createStudent(request);
    }

    @GetMapping("/classes/{classId}/students")
    public List<EnrollmentResponse> findClassStudents(@PathVariable UUID classId) {
        return service.findClassStudents(classId);
    }

    @PostMapping("/classes/{classId}/students")
    public EnrollmentResponse enrollStudent(
            @PathVariable UUID classId,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        return service.enrollStudent(classId, request);
    }

    @GetMapping("/classes/{classId}/assessments")
    public List<AssessmentResponse> findAssessments(@PathVariable UUID classId) {
        return service.findAssessments(classId);
    }

    @PostMapping("/classes/{classId}/assessments")
    public AssessmentResponse createAssessment(
            @PathVariable UUID classId,
            @Valid @RequestBody AssessmentRequest request
    ) {
        return service.createAssessment(classId, request);
    }

    @GetMapping("/classes/{classId}/scores")
    public List<ScoreResponse> findScores(@PathVariable UUID classId) {
        return service.findScores(classId);
    }

    @PutMapping("/assessments/{assessmentId}/scores")
    public ScoreResponse recordScore(
            @PathVariable UUID assessmentId,
            @Valid @RequestBody ScoreRequest request
    ) {
        return service.recordScore(assessmentId, request);
    }
}

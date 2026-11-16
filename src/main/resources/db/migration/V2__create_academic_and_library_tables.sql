CREATE TABLE course_levels (
    id UUID PRIMARY KEY,
    institute_id UUID NOT NULL REFERENCES institutes (id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_course_levels_institute_code UNIQUE (institute_id, code)
);

CREATE TABLE courses (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    level_id UUID NOT NULL REFERENCES course_levels (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_courses_level_code UNIQUE (level_id, code)
);

CREATE TABLE course_classes (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses (id),
    name VARCHAR(100) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_course_classes_course_name_year UNIQUE (course_id, name, academic_year)
);

CREATE TABLE students (
    id UUID PRIMARY KEY,
    institute_id UUID NOT NULL REFERENCES institutes (id),
    student_number VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(254),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_students_institute_number UNIQUE (institute_id, student_number)
);

CREATE TABLE class_enrollments (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES course_classes (id),
    student_id UUID NOT NULL REFERENCES students (id),
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_class_enrollments_class_student UNIQUE (class_id, student_id)
);

CREATE TABLE assessments (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES course_classes (id),
    name VARCHAR(150) NOT NULL,
    max_score DECIMAL(8, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_assessments_class_name UNIQUE (class_id, name),
    CONSTRAINT ck_assessments_max_score_positive CHECK (max_score > 0)
);

CREATE TABLE student_scores (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessments (id),
    student_id UUID NOT NULL REFERENCES students (id),
    score DECIMAL(8, 2) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_student_scores_assessment_student UNIQUE (assessment_id, student_id),
    CONSTRAINT ck_student_scores_nonnegative CHECK (score >= 0)
);

CREATE TABLE library_books (
    id UUID PRIMARY KEY,
    institute_id UUID NOT NULL REFERENCES institutes (id),
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    total_copies INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_library_books_institute_isbn UNIQUE (institute_id, isbn),
    CONSTRAINT ck_library_books_copies_nonnegative CHECK (total_copies >= 0)
);

CREATE TABLE library_loans (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL REFERENCES library_books (id),
    student_id UUID NOT NULL REFERENCES students (id),
    checked_out_at TIMESTAMP WITH TIME ZONE NOT NULL,
    due_date DATE NOT NULL,
    returned_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_class_enrollments_student ON class_enrollments (student_id);
CREATE INDEX ix_student_scores_student ON student_scores (student_id);
CREATE INDEX ix_library_loans_active ON library_loans (book_id, returned_at);

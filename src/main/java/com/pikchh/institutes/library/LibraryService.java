package com.pikchh.institutes.library;

import com.pikchh.institutes.institute.DomainConflictException;
import com.pikchh.institutes.institute.DomainNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.pikchh.institutes.library.LibraryModels.*;

@Service
@Transactional
public class LibraryService {

    private final JdbcTemplate jdbc;

    public LibraryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> findBooks() {
        return jdbc.query("""
                SELECT b.id, b.institute_id, i.name AS institute_name, b.isbn, b.title, b.author, b.total_copies,
                       b.total_copies - COUNT(l.id) AS available_copies
                FROM library_books b
                JOIN institutes i ON i.id = b.institute_id
                LEFT JOIN library_loans l ON l.book_id = b.id AND l.returned_at IS NULL
                GROUP BY b.id, b.institute_id, i.name, b.isbn, b.title, b.author, b.total_copies
                ORDER BY b.title
                """, (rs, row) -> bookResponse(rs));
    }

    public BookResponse createBook(BookRequest request) {
        String instituteName = jdbc.query("""
                SELECT name FROM institutes WHERE id = ?
                """, (rs, row) -> rs.getString("name"), request.instituteId()).stream().findFirst()
                .orElseThrow(() -> new DomainNotFoundException("Institute not found: " + request.instituteId()));
        UUID id = UUID.randomUUID();
        String isbn = request.isbn().trim();
        String title = request.title().trim();
        String author = request.author().trim();
        try {
            jdbc.update("""
                    INSERT INTO library_books (id, institute_id, isbn, title, author, total_copies, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, request.instituteId(), isbn, title, author, request.totalCopies(), Instant.now());
        } catch (DuplicateKeyException exception) {
            throw new DomainConflictException("A book with this ISBN already exists for this institute: " + isbn);
        }
        return new BookResponse(id, request.instituteId(), instituteName, isbn, title, author,
                request.totalCopies(), request.totalCopies());
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> findLoans() {
        return jdbc.query("""
                SELECT l.id, b.id AS book_id, b.title AS book_title, s.id AS student_id,
                       s.student_number, s.name AS student_name, l.checked_out_at, l.due_date, l.returned_at
                FROM library_loans l
                JOIN library_books b ON b.id = l.book_id
                JOIN students s ON s.id = l.student_id
                ORDER BY l.returned_at NULLS FIRST, l.due_date, b.title
                """, (rs, row) -> loanResponse(rs));
    }

    public LoanResponse checkout(LoanRequest request) {
        if (!request.dueDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }
        BookForLoan book = jdbc.query("""
                SELECT institute_id, total_copies FROM library_books WHERE id = ? FOR UPDATE
                """, (rs, row) -> new BookForLoan(rs.getObject("institute_id", UUID.class),
                rs.getInt("total_copies")), request.bookId()).stream().findFirst()
                .orElseThrow(() -> new DomainNotFoundException("Book not found: " + request.bookId()));
        var student = jdbc.query("""
                SELECT id, institute_id, student_number, name FROM students WHERE id = ?
                """, (rs, row) -> new Student(rs.getObject("id", UUID.class),
                rs.getObject("institute_id", UUID.class), rs.getString("student_number"),
                rs.getString("name")), request.studentId()).stream().findFirst()
                .orElseThrow(() -> new DomainNotFoundException("Student not found: " + request.studentId()));
        if (!book.instituteId().equals(student.instituteId())) {
            throw new IllegalArgumentException("Student and book must belong to the same institute");
        }

        int activeLoans = jdbc.queryForObject("""
                SELECT COUNT(*) FROM library_loans WHERE book_id = ? AND returned_at IS NULL
                """, Integer.class, request.bookId());
        if (activeLoans >= book.totalCopies()) {
            throw new DomainConflictException("No copies of this book are currently available");
        }

        UUID id = UUID.randomUUID();
        Instant checkedOutAt = Instant.now();
        jdbc.update("""
                INSERT INTO library_loans (id, book_id, student_id, checked_out_at, due_date)
                VALUES (?, ?, ?, ?, ?)
                """, id, request.bookId(), request.studentId(), checkedOutAt, request.dueDate());
        String title = jdbc.queryForObject("SELECT title FROM library_books WHERE id = ?",
                String.class, request.bookId());
        return new LoanResponse(id, request.bookId(), title, student.id(), student.number(), student.name(),
                checkedOutAt, request.dueDate(), null);
    }

    public LoanResponse returnBook(UUID loanId) {
        LoanResponse loan = jdbc.query("""
                SELECT l.id, b.id AS book_id, b.title AS book_title, s.id AS student_id,
                       s.student_number, s.name AS student_name, l.checked_out_at, l.due_date, l.returned_at
                FROM library_loans l
                JOIN library_books b ON b.id = l.book_id
                JOIN students s ON s.id = l.student_id
                WHERE l.id = ? FOR UPDATE
                """, rs -> rs.next() ? loanResponse(rs) : null, loanId);
        if (loan == null) {
            throw new DomainNotFoundException("Library loan not found: " + loanId);
        }
        if (loan.returnedAt() != null) {
            throw new DomainConflictException("This book has already been returned");
        }
        Instant returnedAt = Instant.now();
        jdbc.update("UPDATE library_loans SET returned_at = ? WHERE id = ?", returnedAt, loanId);
        return new LoanResponse(loan.id(), loan.bookId(), loan.bookTitle(), loan.studentId(),
                loan.studentNumber(), loan.studentName(), loan.checkedOutAt(), loan.dueDate(), returnedAt);
    }

    private static BookResponse bookResponse(ResultSet rs) throws SQLException {
        return new BookResponse(rs.getObject("id", UUID.class), rs.getObject("institute_id", UUID.class),
                rs.getString("institute_name"), rs.getString("isbn"), rs.getString("title"),
                rs.getString("author"), rs.getInt("total_copies"), rs.getInt("available_copies"));
    }

    private static LoanResponse loanResponse(ResultSet rs) throws SQLException {
        var checkedOut = rs.getTimestamp("checked_out_at");
        var returned = rs.getTimestamp("returned_at");
        return new LoanResponse(rs.getObject("id", UUID.class), rs.getObject("book_id", UUID.class),
                rs.getString("book_title"), rs.getObject("student_id", UUID.class), rs.getString("student_number"),
                rs.getString("student_name"), checkedOut.toInstant(), rs.getObject("due_date", LocalDate.class),
                returned == null ? null : returned.toInstant());
    }

    private record Student(UUID id, UUID instituteId, String number, String name) {
    }

    private record BookForLoan(UUID instituteId, int totalCopies) {
    }
}

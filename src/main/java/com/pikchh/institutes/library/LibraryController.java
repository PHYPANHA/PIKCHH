package com.pikchh.institutes.library;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.pikchh.institutes.library.LibraryModels.*;

@RestController
@RequestMapping("/api/v1/library")
public class LibraryController {

    private final LibraryService service;

    public LibraryController(LibraryService service) {
        this.service = service;
    }

    @GetMapping("/books")
    public List<BookResponse> findBooks() {
        return service.findBooks();
    }

    @PostMapping("/books")
    public BookResponse createBook(@Valid @RequestBody BookRequest request) {
        return service.createBook(request);
    }

    @GetMapping("/loans")
    public List<LoanResponse> findLoans() {
        return service.findLoans();
    }

    @PostMapping("/loans")
    public LoanResponse checkout(@Valid @RequestBody LoanRequest request) {
        return service.checkout(request);
    }

    @PostMapping("/loans/{loanId}/return")
    public LoanResponse returnBook(@PathVariable UUID loanId) {
        return service.returnBook(loanId);
    }
}

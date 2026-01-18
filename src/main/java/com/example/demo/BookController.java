package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
public class BookController {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;

    @Autowired
    public BookController(BookRepository bookRepository, GoogleBookService googleBookService) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("/google")
    public GoogleBook searchGoogleBooks(@RequestParam("q") String query,
                                        @RequestParam(value = "maxResults", required = false) Integer maxResults,
                                        @RequestParam(value = "startIndex", required = false) Integer startIndex) {
        return googleBookService.searchBooks(query, maxResults, startIndex);
    }

    @PostMapping("/books/{googleId}")
    public ResponseEntity<Book> addBookFromGoogle(@PathVariable String googleId) {
        System.out.println("Received request to save Google book: " + googleId);

        JsonNode root = googleBookService.getBookAsJson(googleId);
        if (root == null) {
            return ResponseEntity.badRequest().build();
        }
        // Extract required fields from the response
        String id = root.path("id").asText(null);
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        JsonNode volumeInfo = root.path("volumeInfo");

        String title = volumeInfo.path("title").asText("UNKNOWN");

        String author = "UNKNOWN";

        JsonNode authorsNode = volumeInfo.path("authors");

        if (authorsNode.isArray() && authorsNode.size() > 0) {
            author = authorsNode.get(0).asText();
        }
        if (title == null || author == null) {
            return ResponseEntity.badRequest().build();
        }

        int pageCount = volumeInfo.path("pageCount").asInt(0);

        Book book = new Book(id,title,author,pageCount);

        Book savedBook = bookRepository.save(book);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
    }

}

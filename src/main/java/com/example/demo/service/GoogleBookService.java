package com.example.demo.service;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.dto.GoogleebookResponse;
import com.example.demo.dto.VolumeInfo;
import com.example.demo.google.GoogleBook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleBookService {
    private final RestClient restClient;
    private final BookRepository bookRepository;


    public GoogleBookService(@Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl, BookRepository bookRepository) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.bookRepository = bookRepository;
    }

    public GoogleBook searchBooks(String query, Integer maxResults, Integer startIndex) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/volumes")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults != null ? maxResults : 10)
                        .queryParam("startIndex", startIndex != null ? startIndex : 0)
                        .build())
                .retrieve()
                .body(GoogleBook.class);
    }

//    public JsonNode getBookAsJson(String googleId) {
//        return restClient.get()
//                .uri("/volumes/{id}", googleId)
//                .retrieve()
//                .body(JsonNode.class);
//    }

    public Book saveGoogleBook(String googleId) {

        GoogleebookResponse response = restClient.get()
                .uri("/volumes/{id}", googleId)
                .retrieve()
                .body(GoogleebookResponse.class);

        if (response == null || response.getId() == null) {
            throw new IllegalArgumentException("Invalid Google Book ID");
        }

        VolumeInfo info = response.getVolumeInfo();

        if (info == null || info.getTitle() == null) {
            throw new IllegalArgumentException("Missing book details");
        }

        String author = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                ? info.getAuthors().get(0)
                : "Unknown";

        Book book = new Book(
                response.getId(),
                info.getTitle(),
                author,
                info.getPageCount()
        );

        return bookRepository.save(book);
    }
}


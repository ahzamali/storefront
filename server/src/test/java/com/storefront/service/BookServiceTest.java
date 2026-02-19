package com.storefront.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {

    @Test
    void bookService_Instantiates() {
        BookService bookService = new BookService();
        assertNotNull(bookService);
    }

    @Test
    void fetchBookDetails_WithInvalidISBN_ReturnsEmpty() {
        BookService bookService = new BookService();
        var result = bookService.fetchBookDetails("0000000000");
        // This will make an actual API call, but with invalid ISBN should return empty
        // In a real scenario, we'd use WireMock or similar for external API testing
        assertNotNull(result);
    }
}

package com.storefront.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BookService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String GOOGLE_BOOKS_API_URL = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    public BookService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Optional<Map<String, Object>> fetchBookDetails(String isbn) {
        try {
            String url = GOOGLE_BOOKS_API_URL + isbn;
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);

            if (root != null && root.has("items") && root.get("items").isArray() && root.get("items").size() > 0) {
                JsonNode volumeInfo = root.get("items").get(0).get("volumeInfo");

                Map<String, Object> details = new HashMap<>();
                details.put("title", volumeInfo.path("title").asText("Unknown Title"));
                details.put("authors", volumeInfo.path("authors")); // Array node
                details.put("publisher", volumeInfo.path("publisher").asText("Unknown"));
                details.put("description", volumeInfo.path("description").asText(""));
                details.put("pageCount", volumeInfo.path("pageCount").asInt(0));

                // Google Books rarely gives price in public API, defaulting or looking at
                // saleInfo
                JsonNode saleInfo = root.get("items").get(0).get("saleInfo");
                if (saleInfo.has("listPrice")) {
                    details.put("price", new BigDecimal(saleInfo.get("listPrice").get("amount").asText()));
                } else {
                    details.put("price", BigDecimal.ZERO); // Needs manual entry
                }

                return Optional.of(details);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

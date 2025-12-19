package com.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.model.Product;
import com.storefront.model.attributes.ApparelAttributes;
import com.storefront.model.attributes.BookAttributes;
import com.storefront.model.attributes.PencilAttributes;
import com.storefront.model.attributes.ProductAttributes;
import com.storefront.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ProductAttributesTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProductAttributesPersistence() {
        // 1. Create Book
        BookAttributes bookAttrs = new BookAttributes();
        bookAttrs.setAuthor("John Doe");
        bookAttrs.setDescription("A good book");
        bookAttrs.setPublisher("Tech Press");
        Product book = new Product("ISBN-TEST", "BOOK", "Test Book", new BigDecimal("19.99"), bookAttrs);

        book = productRepository.save(book);

        // Retrieve and Verify
        Product retrievedBook = productRepository.findById(book.getId()).orElseThrow();
        assertTrue(retrievedBook.getAttributes() instanceof BookAttributes);
        BookAttributes retrievedBookAttrs = (BookAttributes) retrievedBook.getAttributes();
        assertEquals("John Doe", retrievedBookAttrs.getAuthor());
        assertEquals("Tech Press", retrievedBookAttrs.getPublisher());

        // 2. Create Pencil
        PencilAttributes pencilAttrs = new PencilAttributes();
        pencilAttrs.setBrand("Faber");
        pencilAttrs.setHardness("HB");
        pencilAttrs.setEraserIncluded(true);
        Product pencil = new Product("SKU-PEN", "PENCIL", "Graphite Pencil", new BigDecimal("1.50"), pencilAttrs);

        pencil = productRepository.save(pencil);

        // Retrieve and Verify
        Product retrievedPencil = productRepository.findById(pencil.getId()).orElseThrow();
        assertTrue(retrievedPencil.getAttributes() instanceof PencilAttributes);
        PencilAttributes retrievedPencilAttrs = (PencilAttributes) retrievedPencil.getAttributes();
        assertEquals("Faber", retrievedPencilAttrs.getBrand());
        assertTrue(retrievedPencilAttrs.isEraserIncluded());

        // 3. Create Apparel
        ApparelAttributes apparelAttrs = new ApparelAttributes();
        apparelAttrs.setSize("L");
        apparelAttrs.setMaterial("Cotton");
        apparelAttrs.setColor("Blue");
        Product shirt = new Product("SKU-SHIRT", "APPAREL", "Blue Shirt", new BigDecimal("25.00"), apparelAttrs);

        shirt = productRepository.save(shirt);

        // Retrieve and Verify
        Product retrievedShirt = productRepository.findById(shirt.getId()).orElseThrow();
        assertTrue(retrievedShirt.getAttributes() instanceof ApparelAttributes);
        ApparelAttributes retrievedShirtAttrs = (ApparelAttributes) retrievedShirt.getAttributes();
        assertEquals("L", retrievedShirtAttrs.getSize());
        assertEquals("Cotton", retrievedShirtAttrs.getMaterial());
    }
}

package com.storefront.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storefront.model.attributes.ProductAttributes;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class JsonAttributeConverter implements AttributeConverter<ProductAttributes, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ProductAttributes attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting ProductAttributes to JSON", e);
        }
    }

    @Override
    public ProductAttributes convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ProductAttributes.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to ProductAttributes", e);
        }
    }
}

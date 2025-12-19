package com.storefront.model.attributes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BookAttributes.class, name = "BOOK"),
        @JsonSubTypes.Type(value = PencilAttributes.class, name = "PENCIL"),
        @JsonSubTypes.Type(value = ApparelAttributes.class, name = "APPAREL")
})
public interface ProductAttributes extends Serializable {
}

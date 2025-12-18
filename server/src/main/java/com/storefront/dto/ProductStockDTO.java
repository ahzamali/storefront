package com.storefront.dto;

import java.math.BigDecimal;

public class ProductStockDTO {
    private Long id;
    private String sku;
    private String name;
    private String type;
    private BigDecimal price;
    private int quantity;

    public ProductStockDTO(Long id, String sku, String name, String type, BigDecimal price, int quantity) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}

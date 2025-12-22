package com.storefront.dto;

import java.math.BigDecimal;
import java.util.List;

public class BundleViewDTO {
    private Long id;
    private String sku;
    private String name;
    private BigDecimal price;
    private List<BundleItemDTO> items;

    public BundleViewDTO(Long id, String sku, String name, BigDecimal price, List<BundleItemDTO> items) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.items = items;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public List<BundleItemDTO> getItems() {
        return items;
    }

    public static class BundleItemDTO {
        private String productSku;
        private int quantity;

        public BundleItemDTO(String productSku, int quantity) {
            this.productSku = productSku;
            this.quantity = quantity;
        }

        public String getProductSku() {
            return productSku;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}

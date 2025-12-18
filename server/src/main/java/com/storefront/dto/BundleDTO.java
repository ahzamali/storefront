package com.storefront.dto;

import java.math.BigDecimal;
import java.util.List;

public class BundleDTO {
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private List<BundleItemDTO> items;

    // Getters and Setters
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<BundleItemDTO> getItems() {
        return items;
    }

    public void setItems(List<BundleItemDTO> items) {
        this.items = items;
    }

    public static class BundleItemDTO {
        private String productSku;
        private int quantity;

        public String getProductSku() {
            return productSku;
        }

        public void setProductSku(String productSku) {
            this.productSku = productSku;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}

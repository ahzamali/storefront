package com.storefront.dto;

import java.util.List;

public class OrderItemRequestDTO {
    private String sku;
    private int quantity;
    private List<String> excludedProductSkus;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<String> getExcludedProductSkus() {
        return excludedProductSkus;
    }

    public void setExcludedProductSkus(List<String> excludedProductSkus) {
        this.excludedProductSkus = excludedProductSkus;
    }
}

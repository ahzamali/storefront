package com.storefront.dto;

public class StockAllocationDTO {
    @jakarta.validation.constraints.NotBlank(message = "SKU is required")
    private String sku;
    @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

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
}

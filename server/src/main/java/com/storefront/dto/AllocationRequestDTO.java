package com.storefront.dto;

import java.util.List;

public class AllocationRequestDTO {
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotEmpty(message = "Items list cannot be empty")
    private List<StockAllocationDTO> items;

    public List<StockAllocationDTO> getItems() {
        return items;
    }

    public void setItems(List<StockAllocationDTO> items) {
        this.items = items;
    }
}

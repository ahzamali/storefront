package com.storefront.dto;

import java.util.List;

public class AllocationRequestDTO {
    private List<StockAllocationDTO> items;

    public List<StockAllocationDTO> getItems() {
        return items;
    }

    public void setItems(List<StockAllocationDTO> items) {
        this.items = items;
    }
}

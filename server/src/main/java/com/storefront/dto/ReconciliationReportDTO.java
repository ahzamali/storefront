package com.storefront.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReconciliationReportDTO {
    private Long storeId;
    private String storeName;
    private BigDecimal totalRevenue;
    private int totalItemsSold;
    private List<ItemSales> soldItems;
    private List<ReturnedItem> returnedItems;
    private List<String> assignedAdmins;

    public ReconciliationReportDTO(Long storeId, String storeName, BigDecimal totalRevenue, int totalItemsSold,
            List<ItemSales> soldItems, List<ReturnedItem> returnedItems, List<String> assignedAdmins) {
        this.storeId = storeId;
        this.storeName = storeName;
        this.totalRevenue = totalRevenue;
        this.totalItemsSold = totalItemsSold;
        this.soldItems = soldItems;
        this.returnedItems = returnedItems;
        this.assignedAdmins = assignedAdmins;
    }

    // Getters and Setters
    public Long getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public int getTotalItemsSold() {
        return totalItemsSold;
    }

    public List<ItemSales> getSoldItems() {
        return soldItems;
    }

    public List<ReturnedItem> getReturnedItems() {
        return returnedItems;
    }

    public List<String> getAssignedAdmins() {
        return assignedAdmins;
    }

    public static class ItemSales {
        private String sku;
        private String name;
        private int quantity;
        private BigDecimal total;

        public ItemSales(String sku, String name, int quantity, BigDecimal total) {
            this.sku = sku;
            this.name = name;
            this.quantity = quantity;
            this.total = total;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }

    public static class ReturnedItem {
        private String sku;
        private String name;
        private int quantity;

        public ReturnedItem(String sku, String name, int quantity) {
            this.sku = sku;
            this.name = name;
            this.quantity = quantity;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}

package com.storefront.dto;

import java.math.BigDecimal;

public class OrderReconciliationSummaryDTO {
    private long totalOrders;
    private long reconciledOrders;
    private long unreconciledOrders;
    private BigDecimal totalAmount;
    private BigDecimal reconciledAmount;
    private BigDecimal unreconciledAmount;

    public OrderReconciliationSummaryDTO() {
    }

    public OrderReconciliationSummaryDTO(long totalOrders, long reconciledOrders, long unreconciledOrders,
            BigDecimal totalAmount, BigDecimal reconciledAmount, BigDecimal unreconciledAmount) {
        this.totalOrders = totalOrders;
        this.reconciledOrders = reconciledOrders;
        this.unreconciledOrders = unreconciledOrders;
        this.totalAmount = totalAmount;
        this.reconciledAmount = reconciledAmount;
        this.unreconciledAmount = unreconciledAmount;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getReconciledOrders() {
        return reconciledOrders;
    }

    public void setReconciledOrders(long reconciledOrders) {
        this.reconciledOrders = reconciledOrders;
    }

    public long getUnreconciledOrders() {
        return unreconciledOrders;
    }

    public void setUnreconciledOrders(long unreconciledOrders) {
        this.unreconciledOrders = unreconciledOrders;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReconciledAmount() {
        return reconciledAmount;
    }

    public void setReconciledAmount(BigDecimal reconciledAmount) {
        this.reconciledAmount = reconciledAmount;
    }

    public BigDecimal getUnreconciledAmount() {
        return unreconciledAmount;
    }

    public void setUnreconciledAmount(BigDecimal unreconciledAmount) {
        this.unreconciledAmount = unreconciledAmount;
    }
}

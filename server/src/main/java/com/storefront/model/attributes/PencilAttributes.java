package com.storefront.model.attributes;

public class PencilAttributes implements ProductAttributes {
    private String hardness; // e.g. HB, 2B, 2H
    private String brand;
    private boolean eraserIncluded;
    private String material; // e.g. Wood, Mechanical

    public PencilAttributes() {
    }

    public PencilAttributes(String hardness, String brand, boolean eraserIncluded, String material) {
        this.hardness = hardness;
        this.brand = brand;
        this.eraserIncluded = eraserIncluded;
        this.material = material;
    }

    public String getHardness() {
        return hardness;
    }

    public void setHardness(String hardness) {
        this.hardness = hardness;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public boolean isEraserIncluded() {
        return eraserIncluded;
    }

    public void setEraserIncluded(boolean eraserIncluded) {
        this.eraserIncluded = eraserIncluded;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }
}

package com.storefront.model.attributes;

public class ApparelAttributes implements ProductAttributes {
    private String size; // e.g. S, M, L, XL
    private String color;
    private String material; // e.g. Cotton, Polyester
    private String brand;
    private String gender; // e.g. Men, Women, Unisex

    public ApparelAttributes() {
    }

    public ApparelAttributes(String size, String color, String material, String brand, String gender) {
        this.size = size;
        this.color = color;
        this.material = material;
        this.brand = brand;
        this.gender = gender;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}

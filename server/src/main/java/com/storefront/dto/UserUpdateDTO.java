package com.storefront.dto;

import java.util.List;

public class UserUpdateDTO {
    private String password;
    private List<Long> storeIds;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Long> getStoreIds() {
        return storeIds;
    }

    public void setStoreIds(List<Long> storeIds) {
        this.storeIds = storeIds;
    }
}

package com.storefront.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_stores", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "store_id"))
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("currentOwner")
    private java.util.Set<Store> stores = new java.util.HashSet<>();

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public AppUser() {
    }

    public AppUser(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.stores = new java.util.HashSet<>();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public java.util.Set<Store> getStores() {
        return stores;
    }

    public void setStores(java.util.Set<Store> stores) {
        this.stores = stores;
    }

    public void addStore(Store store) {
        this.stores.add(store);
    }

    public void removeStore(Store store) {
        this.stores.remove(store);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

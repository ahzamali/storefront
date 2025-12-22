package com.storefront.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "store")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreType type;

    @ManyToOne
    @JoinColumn(name = "current_owner_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("store")
    private AppUser currentOwner;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Store() {
    }

    public Store(String name, StoreType type, AppUser currentOwner) {
        this.name = name;
        this.type = type;
        this.currentOwner = currentOwner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StoreType getType() {
        return type;
    }

    public void setType(StoreType type) {
        this.type = type;
    }

    public AppUser getCurrentOwner() {
        return currentOwner;
    }

    public void setCurrentOwner(AppUser currentOwner) {
        this.currentOwner = currentOwner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public enum StoreType {
        MASTER, VIRTUAL
    }
}

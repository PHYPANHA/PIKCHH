package com.pikchh.institutes.institute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "institutes", uniqueConstraints = @UniqueConstraint(name = "uk_institutes_code", columnNames = "code"))
public class Institute {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 2048)
    private String website;

    @Column(length = 500)
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Institute() {
    }

    public Institute(String name, String code, String email, String phone, String website, String address) {
        this.name = name;
        this.code = code;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.address = address;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getWebsite() {
        return website;
    }

    public String getAddress() {
        return address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String code, String email, String phone, String website, String address) {
        this.name = name;
        this.code = code;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.address = address;
    }
}

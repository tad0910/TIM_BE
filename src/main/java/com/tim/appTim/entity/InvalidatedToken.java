package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "invalidated_tokens")
public class InvalidatedToken {

    @Id
    private String jti; // Dùng chính JTI làm ID để tra cứu nhanh

    @Column(nullable = false)
    private Instant expiryDate;

    // Constructors, Getters, Setters
    public InvalidatedToken() {}

    public InvalidatedToken(String jti, Instant expiryDate) {
        this.jti = jti;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters...
    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }
}
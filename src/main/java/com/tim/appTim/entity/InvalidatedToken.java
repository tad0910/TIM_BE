package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "invalidated_tokens")
public class InvalidatedToken {

    @Id
    private String jti;

    @Column(nullable = false)
    private Instant expiryDate;

    public InvalidatedToken() {}

    public InvalidatedToken(String jti, Instant expiryDate) {
        this.jti = jti;
        this.expiryDate = expiryDate;
    }

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
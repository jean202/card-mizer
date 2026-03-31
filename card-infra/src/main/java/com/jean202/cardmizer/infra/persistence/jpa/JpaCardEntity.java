package com.jean202.cardmizer.infra.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
public class JpaCardEntity {
    @Id
    @Column(nullable = false, length = 100)
    private String id;

    @Column(nullable = false, length = 100)
    private String issuerName;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 20)
    private String cardType;

    @Column(nullable = false)
    private int priority;

    protected JpaCardEntity() {
    }

    public JpaCardEntity(String id, String issuerName, String productName, String cardType, int priority) {
        this.id = id;
        this.issuerName = issuerName;
        this.productName = productName;
        this.cardType = cardType;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public String getProductName() {
        return productName;
    }

    public String getCardType() {
        return cardType;
    }

    public int getPriority() {
        return priority;
    }
}

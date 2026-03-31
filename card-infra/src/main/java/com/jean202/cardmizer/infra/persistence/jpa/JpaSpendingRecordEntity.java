package com.jean202.cardmizer.infra.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "spending_records")
public class JpaSpendingRecordEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String cardId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private LocalDate spentOn;

    @Column(nullable = false, length = 200)
    private String merchantName;

    @Column(nullable = false, length = 100)
    private String merchantCategory;

    @Column(nullable = false, columnDefinition = "text")
    private String paymentTags;

    protected JpaSpendingRecordEntity() {
    }

    public JpaSpendingRecordEntity(
            UUID id,
            String cardId,
            long amount,
            LocalDate spentOn,
            String merchantName,
            String merchantCategory,
            String paymentTags
    ) {
        this.id = id;
        this.cardId = cardId;
        this.amount = amount;
        this.spentOn = spentOn;
        this.merchantName = merchantName;
        this.merchantCategory = merchantCategory;
        this.paymentTags = paymentTags;
    }

    public UUID getId() {
        return id;
    }

    public String getCardId() {
        return cardId;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDate getSpentOn() {
        return spentOn;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public String getPaymentTags() {
        return paymentTags;
    }
}

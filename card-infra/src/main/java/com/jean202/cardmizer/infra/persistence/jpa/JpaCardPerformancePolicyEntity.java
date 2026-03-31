package com.jean202.cardmizer.infra.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_performance_policies")
public class JpaCardPerformancePolicyEntity {
    @Id
    @Column(nullable = false, length = 100)
    private String cardId;

    @Column(nullable = false, columnDefinition = "text")
    private String tiersJson;

    @Column(nullable = false, columnDefinition = "text")
    private String benefitRulesJson;

    protected JpaCardPerformancePolicyEntity() {
    }

    public JpaCardPerformancePolicyEntity(String cardId, String tiersJson, String benefitRulesJson) {
        this.cardId = cardId;
        this.tiersJson = tiersJson;
        this.benefitRulesJson = benefitRulesJson;
    }

    public String getCardId() {
        return cardId;
    }

    public String getTiersJson() {
        return tiersJson;
    }

    public String getBenefitRulesJson() {
        return benefitRulesJson;
    }
}

package com.jean202.cardmizer.infra.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "card_performance_policies")
class JpaCardPerformancePolicyEntity(
    @Id @Column(nullable = false, length = 100)
    var cardId: String,

    @Column(nullable = false, columnDefinition = "text")
    var tiersJson: String,

    @Column(nullable = false, columnDefinition = "text")
    var benefitRulesJson: String,
)

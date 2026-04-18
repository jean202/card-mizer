package com.jean202.cardmizer.infra.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "spending_records")
class JpaSpendingRecordEntity(
    @Id @Column(nullable = false)
    var id: UUID,

    @Column(nullable = false, length = 100)
    var cardId: String,

    @Column(nullable = false)
    var amount: Long,

    @Column(nullable = false)
    var spentOn: LocalDate,

    @Column(nullable = false, length = 200)
    var merchantName: String,

    @Column(nullable = false, length = 100)
    var merchantCategory: String,

    @Column(nullable = false, columnDefinition = "text")
    var paymentTags: String,

    @Column(nullable = false)
    var deleted: Boolean,
)

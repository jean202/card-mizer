package com.jean202.cardmizer.infra.persistence.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "cards")
class JpaCardEntity(
    @Id @Column(nullable = false, length = 100)
    var id: String,

    @Column(nullable = false, length = 100)
    var issuerName: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    @Column(nullable = false, length = 20)
    var cardType: String,

    @Column(nullable = false)
    var priority: Int,
)

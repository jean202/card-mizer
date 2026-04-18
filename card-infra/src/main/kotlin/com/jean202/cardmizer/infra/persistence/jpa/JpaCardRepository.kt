package com.jean202.cardmizer.infra.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface JpaCardRepository : JpaRepository<JpaCardEntity, String> {
    fun findAllByOrderByPriorityAsc(): List<JpaCardEntity>
}

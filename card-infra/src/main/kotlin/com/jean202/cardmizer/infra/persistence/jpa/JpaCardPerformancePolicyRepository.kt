package com.jean202.cardmizer.infra.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface JpaCardPerformancePolicyRepository : JpaRepository<JpaCardPerformancePolicyEntity, String>

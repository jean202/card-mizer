package com.jean202.cardmizer.infra.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSpendingRecordRepository extends JpaRepository<JpaSpendingRecordEntity, UUID> {
    List<JpaSpendingRecordEntity> findByDeletedFalseAndSpentOnBetweenOrderBySpentOnAscIdAsc(
            LocalDate startDate, LocalDate endDate);

    List<JpaSpendingRecordEntity> findByDeletedFalseAndSpentOnBetweenOrderBySpentOnDescIdDesc(
            LocalDate startDate, LocalDate endDate);

    List<JpaSpendingRecordEntity> findByDeletedFalseAndCardIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(
            String cardId, LocalDate startDate, LocalDate endDate);
}

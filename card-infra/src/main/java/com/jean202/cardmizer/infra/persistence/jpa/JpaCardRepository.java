package com.jean202.cardmizer.infra.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCardRepository extends JpaRepository<JpaCardEntity, String> {
    List<JpaCardEntity> findAllByOrderByPriorityAsc();
}

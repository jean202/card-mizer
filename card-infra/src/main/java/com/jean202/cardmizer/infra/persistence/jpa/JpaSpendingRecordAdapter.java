package com.jean202.cardmizer.infra.persistence.jpa;

import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.LoadSpendingRecordsPort;
import com.jean202.cardmizer.core.port.out.SaveSpendingRecordPort;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@Transactional
public class JpaSpendingRecordAdapter implements LoadSpendingRecordsPort, SaveSpendingRecordPort {
    private final JpaSpendingRecordRepository repository;

    public JpaSpendingRecordAdapter(JpaSpendingRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpendingRecord> loadByPeriod(SpendingPeriod period) {
        LocalDate startDate = period.yearMonth().atDay(1);
        LocalDate endDate = period.yearMonth().atEndOfMonth();
        return repository.findBySpentOnBetweenOrderBySpentOnAscIdAsc(startDate, endDate).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(SpendingRecord spendingRecord) {
        repository.save(toEntity(spendingRecord));
    }

    private SpendingRecord toDomain(JpaSpendingRecordEntity entity) {
        return new SpendingRecord(
                entity.getId(),
                new CardId(entity.getCardId()),
                Money.won(entity.getAmount()),
                entity.getSpentOn(),
                entity.getMerchantName(),
                entity.getMerchantCategory(),
                parseTags(entity.getPaymentTags())
        );
    }

    private JpaSpendingRecordEntity toEntity(SpendingRecord spendingRecord) {
        return new JpaSpendingRecordEntity(
                spendingRecord.id(),
                spendingRecord.cardId().value(),
                spendingRecord.amount().amount(),
                spendingRecord.spentOn(),
                spendingRecord.merchantName(),
                spendingRecord.merchantCategory(),
                formatTags(spendingRecord.paymentTags())
        );
    }

    private String formatTags(Set<String> paymentTags) {
        if (paymentTags == null || paymentTags.isEmpty()) {
            return "";
        }
        return paymentTags.stream().sorted().collect(Collectors.joining(","));
    }

    private Set<String> parseTags(String serializedPaymentTags) {
        if (serializedPaymentTags == null || serializedPaymentTags.isBlank()) {
            return Set.of();
        }

        Set<String> tags = new LinkedHashSet<>();
        for (String rawTag : serializedPaymentTags.split(",")) {
            if (!rawTag.isBlank()) {
                tags.add(rawTag);
            }
        }
        return Set.copyOf(tags);
    }
}

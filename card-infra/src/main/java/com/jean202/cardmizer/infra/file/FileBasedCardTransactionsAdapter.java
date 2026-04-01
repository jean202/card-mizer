package com.jean202.cardmizer.infra.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter that reads card transactions from local JSON files.
 * Activated via {@code --spring.profiles.active=file-sync} profile.
 * <p>
 * Demonstrates hexagonal architecture's adapter interchangeability:
 * same {@link FetchCardTransactionsPort}, different data source (file vs HTTP),
 * zero changes to the domain core.
 * <p>
 * File naming convention: {@code {directory}/{cardId}-{yearMonth}.json}
 * <p>
 * The file format intentionally uses different field names (date, store, won, ...)
 * from both the domain model and the HTTP adapter's DTO to show that each adapter
 * performs its own translation.
 */
@Component
@Profile("file-sync")
public class FileBasedCardTransactionsAdapter implements FetchCardTransactionsPort {
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String directory;

    public FileBasedCardTransactionsAdapter(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${cardcompany.file.directory:classpath:transactions}") String directory
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.directory = directory;
    }

    @Override
    public List<SpendingRecord> fetchByCardAndPeriod(CardId cardId, SpendingPeriod period) {
        String fileName = cardId.value() + "-" + period.yearMonth() + ".json";
        Resource resource = resourceLoader.getResource(directory + "/" + fileName);

        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            List<FileTransactionDto> dtos = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<FileTransactionDto>>() {}
            );
            return dtos.stream().map(this::toDomain).toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read transaction file: " + fileName, e);
        }
    }

    private SpendingRecord toDomain(FileTransactionDto dto) {
        return new SpendingRecord(
                UUID.randomUUID(),
                new CardId(dto.card()),
                Money.won(dto.won()),
                LocalDate.parse(dto.date()),
                dto.store(),
                dto.category() != null ? dto.category() : "UNCATEGORIZED",
                dto.tags() != null ? new HashSet<>(dto.tags()) : Set.of()
        );
    }

    /**
     * File-based transaction DTO.
     * Field names (date, store, won, card, tags) intentionally differ from both
     * the domain model and the HTTP adapter's DTO (transactionDate, merchantName,
     * approvalAmount, cardNumber, paymentMethods).
     */
    private record FileTransactionDto(
            String date,
            String store,
            String category,
            long won,
            String card,
            List<String> tags
    ) {}
}

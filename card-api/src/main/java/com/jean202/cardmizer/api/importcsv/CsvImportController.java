package com.jean202.cardmizer.api.importcsv;

import com.jean202.cardmizer.api.normalization.NormalizedTransaction;
import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Inbound adapter that imports spending records from a CSV file.
 * <p>
 * Demonstrates hexagonal architecture's inbound adapter interchangeability:
 * same {@link RecordSpendingUseCase}, different input source (CSV file upload vs JSON REST body).
 * <p>
 * Expected CSV format (UTF-8, header required):
 * <pre>
 * date,cardId,amount,merchantName,merchantCategory,paymentTags
 * 2026-03-05,KB_NORI2_KBPAY,5500,스타벅스 강남점,카페,KB_PAY
 * 2026-03-09,KB_NORI2_KBPAY,14000,CGV 왕십리,영화,"KB_PAY|ONLINE"
 * </pre>
 * {@code paymentTags} uses {@code |} as delimiter within the field.
 */
@RestController
@RequestMapping("/api/import")
public class CsvImportController {
    private static final int EXPECTED_COLUMNS = 6;

    private final RecordSpendingUseCase recordSpendingUseCase;
    private final TransactionNormalizer transactionNormalizer;

    public CsvImportController(
            RecordSpendingUseCase recordSpendingUseCase,
            TransactionNormalizer transactionNormalizer
    ) {
        this.recordSpendingUseCase = recordSpendingUseCase;
        this.transactionNormalizer = transactionNormalizer;
    }

    @PostMapping(value = "/spending-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResponse importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<String> errors = new ArrayList<>();
        int imported = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            lineNumber++;
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file is empty or missing header row");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                try {
                    SpendingRecord record = parseLine(line, lineNumber);
                    recordSpendingUseCase.record(record);
                    imported++;
                } catch (Exception e) {
                    errors.add("line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }

        return new CsvImportResponse(imported, errors.size(), errors);
    }

    private SpendingRecord parseLine(String line, int lineNumber) {
        String[] columns = line.split(",", -1);
        if (columns.length < EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "expected " + EXPECTED_COLUMNS + " columns but found " + columns.length
            );
        }

        String dateStr = columns[0].trim();
        String cardId = columns[1].trim();
        String amountStr = columns[2].trim();
        String merchantName = columns[3].trim();
        String merchantCategory = columns[4].trim();
        String tagsStr = columns[5].trim();

        if (cardId.isBlank()) {
            throw new IllegalArgumentException("cardId is blank");
        }
        if (merchantName.isBlank()) {
            throw new IllegalArgumentException("merchantName is blank");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid date: " + dateStr);
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid amount: " + amountStr);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }

        Set<String> rawTags = tagsStr.isBlank()
                ? Set.of()
                : new LinkedHashSet<>(Arrays.asList(tagsStr.split("\\|")));

        NormalizedTransaction normalized = transactionNormalizer.normalize(
                merchantName,
                merchantCategory.isBlank() ? null : merchantCategory,
                rawTags
        );

        return new SpendingRecord(
                UUID.randomUUID(),
                new CardId(cardId),
                Money.won(amount),
                date,
                merchantName,
                normalized.merchantCategory(),
                normalized.paymentTags()
        );
    }

    public record CsvImportResponse(int importedCount, int errorCount, List<String> errors) {}
}

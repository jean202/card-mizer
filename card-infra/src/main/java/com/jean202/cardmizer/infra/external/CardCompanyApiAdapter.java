package com.jean202.cardmizer.infra.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.cardmizer.common.Money;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.SpendingPeriod;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter that fetches card transactions from an external card company API.
 * <p>
 * Handles API key authentication, HTTP error codes, and timeouts.
 * Translates the external DTO format (cardNumber, approvalAmount, businessType, ...)
 * into the domain SpendingRecord model.
 */
@Component
@Profile("!file-sync")
public class CardCompanyApiAdapter implements FetchCardTransactionsPort {
    private static final int MAX_RETRIES = 2;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public CardCompanyApiAdapter(
            ObjectMapper objectMapper,
            @Value("${cardcompany.api.base-url:http://localhost:8080/simulator/api/v1}") String baseUrl,
            @Value("${cardcompany.api.key:sim-api-key-2026}") String apiKey
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public List<SpendingRecord> fetchByCardAndPeriod(CardId cardId, SpendingPeriod period) {
        String url = baseUrl + "/cards/" + cardId.value() + "/transactions?yearMonth=" + period.yearMonth();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("X-Api-Key", apiKey)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = sendWithRetry(request, cardId);

        try {
            List<CardCompanyTransactionDto> dtos = objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<CardCompanyTransactionDto>>() {}
            );
            return dtos.stream().map(this::toDomain).toList();
        } catch (IOException e) {
            throw new CardCompanyApiException(
                    "Failed to parse response for card " + cardId.value(), e
            );
        }
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request, CardId cardId) {
        IOException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status == 200) {
                    return response;
                }
                if (status == 401 || status == 403) {
                    throw new CardCompanyApiException(
                            "Authentication failed for card company API (HTTP " + status + ")"
                    );
                }
                if (status == 429 || status >= 500) {
                    lastException = new IOException("HTTP " + status);
                    if (attempt < MAX_RETRIES) {
                        sleepBeforeRetry(attempt);
                        continue;
                    }
                }
                throw new CardCompanyApiException(
                        "Card company API returned HTTP " + status + " for card " + cardId.value()
                );
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry(attempt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CardCompanyApiException("Transaction fetch interrupted for card " + cardId.value(), e);
            }
        }

        throw new CardCompanyApiException(
                "Failed to fetch transactions for card " + cardId.value() + " after " + (MAX_RETRIES + 1) + " attempts",
                lastException
        );
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(500L * (attempt + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SpendingRecord toDomain(CardCompanyTransactionDto dto) {
        return new SpendingRecord(
                UUID.randomUUID(),
                new CardId(dto.cardNumber()),
                Money.won(dto.approvalAmount()),
                LocalDate.parse(dto.transactionDate()),
                dto.merchantName(),
                dto.businessType() != null ? dto.businessType() : "UNCATEGORIZED",
                dto.paymentMethods() != null ? new HashSet<>(dto.paymentMethods()) : Set.of()
        );
    }

    /**
     * External card company API response DTO.
     * Field names intentionally differ from domain model to demonstrate
     * the adapter's translation responsibility in hexagonal architecture.
     */
    private record CardCompanyTransactionDto(
            String txnId,
            String cardNumber,
            long approvalAmount,
            String transactionDate,
            String merchantName,
            String businessType,
            List<String> paymentMethods
    ) {}

    public static class CardCompanyApiException extends RuntimeException {
        public CardCompanyApiException(String message) {
            super(message);
        }

        public CardCompanyApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.jean202.cardmizer.infra.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.jean202.cardmizer.common.Money
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.SpendingPeriod
import com.jean202.cardmizer.core.domain.SpendingRecord
import com.jean202.cardmizer.core.port.out.FetchCardTransactionsPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Component
@Profile("!file-sync")
class CardCompanyApiAdapter(
    private val objectMapper: ObjectMapper,
    @Value("\${cardcompany.api.base-url:http://localhost:8080/simulator/api/v1}") private val baseUrl: String,
    @Value("\${cardcompany.api.key:sim-api-key-2026}") private val apiKey: String,
) : FetchCardTransactionsPort {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build()

    override fun fetchByCardAndPeriod(cardId: CardId, period: SpendingPeriod): List<SpendingRecord> {
        val url = "$baseUrl/cards/${cardId.value}/transactions?yearMonth=${period.yearMonth}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("X-Api-Key", apiKey)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build()

        val response = sendWithRetry(request, cardId)

        return try {
            val dtos = objectMapper.readValue(
                response.body(),
                object : TypeReference<List<CardCompanyTransactionDto>>() {},
            )
            dtos.map { toDomain(it) }
        } catch (e: Exception) {
            throw CardCompanyApiException("Failed to parse response for card ${cardId.value}", e)
        }
    }

    private fun sendWithRetry(request: HttpRequest, cardId: CardId): HttpResponse<String> {
        var lastException: Exception? = null

        for (attempt in 0..MAX_RETRIES) {
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                when (val status = response.statusCode()) {
                    200 -> return response
                    401, 403 -> throw CardCompanyApiException("Authentication failed for card company API (HTTP $status)")
                    429 -> {
                        lastException = Exception("HTTP $status")
                        if (attempt < MAX_RETRIES) { sleepBeforeRetry(attempt); continue }
                    }
                    in 500..599 -> {
                        lastException = Exception("HTTP $status")
                        if (attempt < MAX_RETRIES) { sleepBeforeRetry(attempt); continue }
                    }
                    else -> throw CardCompanyApiException("Card company API returned HTTP $status for card ${cardId.value}")
                }
            } catch (e: CardCompanyApiException) {
                throw e
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw CardCompanyApiException("Transaction fetch interrupted for card ${cardId.value}", e)
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES) sleepBeforeRetry(attempt)
            }
        }

        throw CardCompanyApiException(
            "Failed to fetch transactions for card ${cardId.value} after ${MAX_RETRIES + 1} attempts",
            lastException,
        )
    }

    private fun sleepBeforeRetry(attempt: Int) {
        try { Thread.sleep(500L * (attempt + 1)) }
        catch (e: InterruptedException) { Thread.currentThread().interrupt() }
    }

    private fun toDomain(dto: CardCompanyTransactionDto) = SpendingRecord(
        id = UUID.randomUUID(),
        cardId = CardId(dto.cardNumber),
        amount = Money.won(dto.approvalAmount),
        spentOn = LocalDate.parse(dto.transactionDate),
        merchantName = dto.merchantName,
        merchantCategory = dto.businessType ?: "UNCATEGORIZED",
        paymentTags = dto.paymentMethods?.toHashSet() ?: emptySet(),
    )

    private data class CardCompanyTransactionDto(
        val txnId: String,
        val cardNumber: String,
        val approvalAmount: Long,
        val transactionDate: String,
        val merchantName: String,
        val businessType: String?,
        val paymentMethods: List<String>?,
    )

    class CardCompanyApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    companion object {
        private const val MAX_RETRIES = 2
        private val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}

package com.jean202.cardmizer.api.simulator

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException

class CardCompanySimulatorControllerTest {
    private val controller = CardCompanySimulatorController()

    @Test
    fun generatesTransactionsForKnownCard() {
        val result = controller.getTransactions("KB_NORI2_KBPAY", "2026-03", "sim-api-key-2026", null)

        assertAll(
            { assertTrue(result.size >= 15, "should generate at least 15 transactions") },
            { assertTrue(result.size <= 25, "should generate at most 25 transactions") },
            { assertTrue(result.all { it.cardNumber == "KB_NORI2_KBPAY" }) },
            { assertTrue(result.all { it.transactionDate.startsWith("2026-03") }) },
            { assertTrue(result.all { it.txnId.startsWith("TXN-KB_NORI2_KBPAY-") }) },
        )
    }

    @Test
    fun generatesDifferentTransactionsForDifferentMonths() {
        val march = controller.getTransactions("KB_NORI2_KBPAY", "2026-03", "sim-api-key-2026", null)
        val april = controller.getTransactions("KB_NORI2_KBPAY", "2026-04", "sim-api-key-2026", null)

        val marchMerchants = march.map { "${it.merchantName}:${it.approvalAmount}" }.toSet()
        val aprilMerchants = april.map { "${it.merchantName}:${it.approvalAmount}" }.toSet()

        assertFalse(marchMerchants == aprilMerchants, "different months should produce different transaction sets")
    }

    @Test
    fun returnsEmptyListForUnknownCard() {
        val result = controller.getTransactions("UNKNOWN_CARD", "2026-03", "sim-api-key-2026", null)

        assertEquals(0, result.size)
    }

    @Test
    fun rejectsMissingApiKey() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            controller.getTransactions("KB_NORI2_KBPAY", "2026-03", null, null)
        }

        assertEquals(401, exception.statusCode.value())
    }

    @Test
    fun rejectsInvalidApiKey() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            controller.getTransactions("KB_NORI2_KBPAY", "2026-03", "wrong-key", null)
        }

        assertEquals(403, exception.statusCode.value())
    }

    @Test
    fun simulatesServerError() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            controller.getTransactions("KB_NORI2_KBPAY", "2026-03", "sim-api-key-2026", "500")
        }

        assertEquals(500, exception.statusCode.value())
    }

    @Test
    fun generatesTransactionsForHyundaiZeroPoint() {
        val result = controller.getTransactions("HYUNDAI_ZERO_POINT", "2026-03", "sim-api-key-2026", null)

        assertAll(
            { assertTrue(result.size >= 10, "should generate transactions for HYUNDAI_ZERO_POINT") },
            { assertTrue(result.all { it.cardNumber == "HYUNDAI_ZERO_POINT" }) },
        )
    }

    @Test
    fun generatesDeterministicResultsForSameInput() {
        val first = controller.getTransactions("SAMSUNG_KPASS", "2026-03", "sim-api-key-2026", null)
        val second = controller.getTransactions("SAMSUNG_KPASS", "2026-03", "sim-api-key-2026", null)

        assertEquals(first.size, second.size)
        for (i in first.indices) {
            assertEquals(first[i].merchantName, second[i].merchantName)
            assertEquals(first[i].approvalAmount, second[i].approvalAmount)
        }
    }
}

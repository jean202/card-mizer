package com.jean202.cardmizer.api.simulator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.api.simulator.CardCompanySimulatorController.TransactionResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CardCompanySimulatorControllerTest {
    private final CardCompanySimulatorController controller = new CardCompanySimulatorController();

    @Test
    void generatesTransactionsForKnownCard() {
        List<TransactionResponse> result = controller.getTransactions(
                "KB_NORI2_KBPAY", "2026-03", "sim-api-key-2026", null
        );

        assertAll(
                () -> assertTrue(result.size() >= 15, "should generate at least 15 transactions"),
                () -> assertTrue(result.size() <= 25, "should generate at most 25 transactions"),
                () -> assertTrue(result.stream().allMatch(t -> t.cardNumber().equals("KB_NORI2_KBPAY"))),
                () -> assertTrue(result.stream().allMatch(t -> t.transactionDate().startsWith("2026-03"))),
                () -> assertTrue(result.stream().allMatch(t -> t.txnId().startsWith("TXN-KB_NORI2_KBPAY-")))
        );
    }

    @Test
    void generatesDifferentTransactionsForDifferentMonths() {
        List<TransactionResponse> march = controller.getTransactions(
                "KB_NORI2_KBPAY", "2026-03", "sim-api-key-2026", null
        );
        List<TransactionResponse> april = controller.getTransactions(
                "KB_NORI2_KBPAY", "2026-04", "sim-api-key-2026", null
        );

        Set<String> marchMerchants = new HashSet<>();
        march.forEach(t -> marchMerchants.add(t.merchantName() + ":" + t.approvalAmount()));
        Set<String> aprilMerchants = new HashSet<>();
        april.forEach(t -> aprilMerchants.add(t.merchantName() + ":" + t.approvalAmount()));

        assertFalse(marchMerchants.equals(aprilMerchants), "different months should produce different transaction sets");
    }

    @Test
    void returnsEmptyListForUnknownCard() {
        List<TransactionResponse> result = controller.getTransactions(
                "UNKNOWN_CARD", "2026-03", "sim-api-key-2026", null
        );

        assertEquals(0, result.size());
    }

    @Test
    void rejectsMissingApiKey() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getTransactions("KB_NORI2_KBPAY", "2026-03", null, null)
        );

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void rejectsInvalidApiKey() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getTransactions("KB_NORI2_KBPAY", "2026-03", "wrong-key", null)
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void simulatesServerError() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getTransactions("KB_NORI2_KBPAY", "2026-03", "sim-api-key-2026", "500")
        );

        assertEquals(500, exception.getStatusCode().value());
    }

    @Test
    void generatesTransactionsForHyundaiZeroPoint() {
        List<TransactionResponse> result = controller.getTransactions(
                "HYUNDAI_ZERO_POINT", "2026-03", "sim-api-key-2026", null
        );

        assertAll(
                () -> assertTrue(result.size() >= 10, "should generate transactions for HYUNDAI_ZERO_POINT"),
                () -> assertTrue(result.stream().allMatch(t -> t.cardNumber().equals("HYUNDAI_ZERO_POINT")))
        );
    }

    @Test
    void generatesDeterministicResultsForSameInput() {
        List<TransactionResponse> first = controller.getTransactions(
                "SAMSUNG_KPASS", "2026-03", "sim-api-key-2026", null
        );
        List<TransactionResponse> second = controller.getTransactions(
                "SAMSUNG_KPASS", "2026-03", "sim-api-key-2026", null
        );

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).merchantName(), second.get(i).merchantName());
            assertEquals(first.get(i).approvalAmount(), second.get(i).approvalAmount());
        }
    }
}

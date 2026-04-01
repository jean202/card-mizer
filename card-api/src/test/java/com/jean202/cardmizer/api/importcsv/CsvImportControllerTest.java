package com.jean202.cardmizer.api.importcsv;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jean202.cardmizer.api.normalization.TransactionNormalizer;
import com.jean202.cardmizer.core.domain.SpendingRecord;
import com.jean202.cardmizer.core.port.in.RecordSpendingUseCase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CsvImportControllerTest {
    @Test
    void importsCsvRowsUsingRecordSpendingUseCase() {
        CapturingRecordSpendingUseCase useCase = new CapturingRecordSpendingUseCase();
        CsvImportController controller = new CsvImportController(useCase, new TransactionNormalizer());

        String csv = """
                date,cardId,amount,merchantName,merchantCategory,paymentTags
                2026-03-05,KB_NORI2_KBPAY,5500,스타벅스 강남점,카페,KB_PAY
                2026-03-09,KB_NORI2_KBPAY,14000,CGV 왕십리,영화,KB_PAY|ONLINE
                """;
        MockMultipartFile file = csvFile(csv);

        CsvImportController.CsvImportResponse response = controller.importCsv(file);

        assertAll(
                () -> assertEquals(2, response.importedCount()),
                () -> assertEquals(0, response.errorCount()),
                () -> assertEquals(2, useCase.saved.size()),
                () -> assertEquals("스타벅스 강남점", useCase.saved.get(0).merchantName()),
                () -> assertEquals("CGV 왕십리", useCase.saved.get(1).merchantName()),
                () -> assertTrue(useCase.saved.get(1).paymentTags().contains("KB_PAY")),
                () -> assertTrue(useCase.saved.get(1).paymentTags().contains("ONLINE"))
        );
    }

    @Test
    void normalizesTransactionsDuringImport() {
        CapturingRecordSpendingUseCase useCase = new CapturingRecordSpendingUseCase();
        CsvImportController controller = new CsvImportController(useCase, new TransactionNormalizer());

        String csv = """
                date,cardId,amount,merchantName,merchantCategory,paymentTags
                2026-03-01,KB_MY_WESH,17000,넷플릭스,,
                """;
        MockMultipartFile file = csvFile(csv);

        controller.importCsv(file);

        SpendingRecord record = useCase.saved.get(0);
        assertAll(
                () -> assertEquals("OTT", record.merchantCategory()),
                () -> assertTrue(record.paymentTags().contains("SUBSCRIPTION")),
                () -> assertTrue(record.paymentTags().contains("ONLINE"))
        );
    }

    @Test
    void collectsErrorsWithoutStoppingImport() {
        CapturingRecordSpendingUseCase useCase = new CapturingRecordSpendingUseCase();
        CsvImportController controller = new CsvImportController(useCase, new TransactionNormalizer());

        String csv = """
                date,cardId,amount,merchantName,merchantCategory,paymentTags
                2026-03-05,KB_NORI2_KBPAY,5500,스타벅스 강남점,카페,KB_PAY
                bad-date,KB_NORI2_KBPAY,5500,이디야,,
                2026-03-10,KB_NORI2_KBPAY,3000,GS25 역삼점,편의점,
                """;
        MockMultipartFile file = csvFile(csv);

        CsvImportController.CsvImportResponse response = controller.importCsv(file);

        assertAll(
                () -> assertEquals(2, response.importedCount()),
                () -> assertEquals(1, response.errorCount()),
                () -> assertTrue(response.errors().get(0).contains("line 3"))
        );
    }

    @Test
    void rejectsEmptyFile() {
        CsvImportController controller = new CsvImportController(record -> {}, new TransactionNormalizer());

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> controller.importCsv(emptyFile));
    }

    @Test
    void skipsBlankLines() {
        CapturingRecordSpendingUseCase useCase = new CapturingRecordSpendingUseCase();
        CsvImportController controller = new CsvImportController(useCase, new TransactionNormalizer());

        String csv = """
                date,cardId,amount,merchantName,merchantCategory,paymentTags
                2026-03-05,SAMSUNG_KPASS,1250,서울교통공사,대중교통,

                2026-03-07,SAMSUNG_KPASS,1200,경기도버스,대중교통,
                """;
        MockMultipartFile file = csvFile(csv);

        CsvImportController.CsvImportResponse response = controller.importCsv(file);

        assertEquals(2, response.importedCount());
    }

    private static MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file", "transactions.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static final class CapturingRecordSpendingUseCase implements RecordSpendingUseCase {
        private final List<SpendingRecord> saved = new ArrayList<>();

        @Override
        public void record(SpendingRecord spendingRecord) {
            saved.add(spendingRecord);
        }
    }
}

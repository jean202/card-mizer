package com.jean202.cardmizer.api.simulator;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulates an external card company API.
 * In production this would be replaced by a real card company endpoint.
 *
 * <p>Features:
 * <ul>
 *   <li>API key authentication via X-Api-Key header</li>
 *   <li>Month-varying transaction data (seeded by yearMonth hash)</li>
 *   <li>Realistic Korean merchant names, categories, and amounts</li>
 *   <li>Configurable error simulation via X-Simulate-Error header</li>
 * </ul>
 */
@RestController
@RequestMapping("/simulator/api/v1")
public class CardCompanySimulatorController {
    private static final String VALID_API_KEY = "sim-api-key-2026";

    @GetMapping("/cards/{cardId}/transactions")
    public List<TransactionResponse> getTransactions(
            @PathVariable String cardId,
            @RequestParam String yearMonth,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateError
    ) {
        verifyApiKey(apiKey);
        simulateErrorIfRequested(simulateError);

        YearMonth month = YearMonth.parse(yearMonth);
        List<MerchantPool> pool = getMerchantPoolForCard(cardId);
        if (pool.isEmpty()) {
            return List.of();
        }

        return generateTransactions(cardId, month, pool);
    }

    private void verifyApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Api-Key header");
        }
        if (!VALID_API_KEY.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid API key");
        }
    }

    private void simulateErrorIfRequested(String simulateError) {
        if (simulateError == null) {
            return;
        }
        switch (simulateError.toUpperCase()) {
            case "TIMEOUT" -> {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            case "500" -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated server error");
            case "503" -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated service unavailable");
            case "429" -> throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Simulated rate limit exceeded");
            default -> { }
        }
    }

    private List<TransactionResponse> generateTransactions(String cardId, YearMonth month, List<MerchantPool> pool) {
        int monthSeed = month.getYear() * 100 + month.getMonthValue();
        int txnCount = 15 + (monthSeed * 7 + cardId.hashCode()) % 11;

        List<TransactionResponse> result = new ArrayList<>();
        for (int i = 0; i < txnCount; i++) {
            int hash = scramble(monthSeed, cardId.hashCode(), i);
            MerchantPool merchant = pool.get(Math.abs(hash) % pool.size());

            int day = 1 + Math.abs(scramble(hash, i, 3)) % month.lengthOfMonth();
            long amountJitter = (Math.abs(scramble(hash, i, 7)) % 5 - 2) * merchant.amountUnit();
            long amount = Math.max(merchant.amountUnit(), merchant.baseAmount() + amountJitter);

            String txnId = String.format("TXN-%s-%d-%03d", cardId, monthSeed, i + 1);
            result.add(new TransactionResponse(
                    txnId,
                    cardId,
                    amount,
                    month.atDay(day).toString(),
                    merchant.merchantName(),
                    merchant.businessType(),
                    merchant.paymentMethods()
            ));
        }
        return result;
    }

    private static int scramble(int a, int b, int c) {
        int h = a * 31 + b;
        h = h * 37 + c;
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        return h;
    }

    private List<MerchantPool> getMerchantPoolForCard(String cardId) {
        return CARD_MERCHANT_POOLS.getOrDefault(cardId, List.of());
    }

    private static final Map<String, List<MerchantPool>> CARD_MERCHANT_POOLS = Map.of(
            "SAMSUNG_KPASS", List.of(
                    merchant("서울교통공사", "대중교통", 1250, 50, List.of()),
                    merchant("경기도버스", "대중교통", 1200, 50, List.of()),
                    merchant("인천교통공사", "대중교통", 1350, 50, List.of()),
                    merchant("카카오T 택시", "교통", 8500, 500, List.of()),
                    merchant("CU 강남역점", "편의점", 4500, 500, List.of()),
                    merchant("GS25 역삼점", "편의점", 3200, 300, List.of()),
                    merchant("이마트24 선릉점", "편의점", 2800, 200, List.of()),
                    merchant("세븐일레븐 삼성점", "편의점", 3600, 400, List.of()),
                    merchant("다이소 강남점", "생활용품", 5500, 500, List.of()),
                    merchant("교보문고 광화문점", "서적", 18000, 2000, List.of())
            ),
            "KB_NORI2_KBPAY", List.of(
                    merchant("스타벅스 강남점", "카페", 5500, 500, List.of("KB_PAY")),
                    merchant("스타벅스 역삼점", "카페", 6200, 500, List.of("KB_PAY")),
                    merchant("이디야 서초점", "카페", 4300, 300, List.of("KB_PAY")),
                    merchant("투썸플레이스 선릉점", "카페", 6500, 500, List.of("KB_PAY")),
                    merchant("CGV 왕십리", "영화", 14000, 1000, List.of("KB_PAY")),
                    merchant("롯데시네마 건대", "영화", 13000, 1000, List.of("KB_PAY")),
                    merchant("메가박스 코엑스", "영화", 15000, 1000, List.of("KB_PAY")),
                    merchant("쿠팡", "온라인쇼핑", 35000, 5000, List.of("KB_PAY", "ONLINE")),
                    merchant("올리브영 강남점", "드럭스토어", 22000, 3000, List.of("KB_PAY")),
                    merchant("GS25 역삼점", "편의점", 3500, 500, List.of("KB_PAY")),
                    merchant("배스킨라빈스 강남점", "디저트", 7800, 500, List.of("KB_PAY"))
            ),
            "KB_MY_WESH", List.of(
                    merchant("넷플릭스", "OTT", 17000, 0, List.of("ONLINE", "SUBSCRIPTION")),
                    merchant("웨이브", "OTT", 7900, 0, List.of("ONLINE", "SUBSCRIPTION")),
                    merchant("멜론", "음악", 10900, 0, List.of("ONLINE", "SUBSCRIPTION")),
                    merchant("디즈니플러스", "OTT", 9900, 0, List.of("ONLINE", "SUBSCRIPTION")),
                    merchant("롯데월드", "놀이공원", 52000, 5000, List.of()),
                    merchant("에버랜드", "놀이공원", 58000, 5000, List.of()),
                    merchant("CGV 용산", "영화", 28000, 3000, List.of()),
                    merchant("배달의민족", "배달", 23500, 3000, List.of("ONLINE")),
                    merchant("요기요", "배달", 19800, 2000, List.of("ONLINE")),
                    merchant("스타필드 코엑스", "쇼핑", 45000, 5000, List.of()),
                    merchant("방탈출카페 홍대", "여가", 22000, 2000, List.of())
            ),
            "HYUNDAI_ZERO_POINT", List.of(
                    merchant("쿠팡", "온라인쇼핑", 42000, 5000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("11번가", "온라인쇼핑", 28500, 3000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("네이버쇼핑", "온라인쇼핑", 15700, 2000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("무신사", "온라인쇼핑", 67000, 5000, List.of("ONLINE")),
                    merchant("마켓컬리", "온라인쇼핑", 38200, 3000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("카카오페이 택시", "교통", 8400, 1000, List.of("SIMPLE_PAY_ONLINE")),
                    merchant("SSG닷컴", "온라인쇼핑", 52000, 5000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("올리브영 온라인", "온라인쇼핑", 31000, 3000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("배달의민족", "배달", 21000, 2000, List.of("ONLINE", "SIMPLE_PAY_ONLINE")),
                    merchant("토스페이 결제", "간편결제", 12500, 1500, List.of("SIMPLE_PAY_ONLINE"))
            )
    );

    private static MerchantPool merchant(
            String merchantName, String businessType, long baseAmount, long amountUnit, List<String> paymentMethods
    ) {
        return new MerchantPool(merchantName, businessType, baseAmount, amountUnit, paymentMethods);
    }

    public record TransactionResponse(
            String txnId,
            String cardNumber,
            long approvalAmount,
            String transactionDate,
            String merchantName,
            String businessType,
            List<String> paymentMethods
    ) {}

    private record MerchantPool(
            String merchantName,
            String businessType,
            long baseAmount,
            long amountUnit,
            List<String> paymentMethods
    ) {}
}

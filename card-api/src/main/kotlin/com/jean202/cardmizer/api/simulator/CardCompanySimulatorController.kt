package com.jean202.cardmizer.api.simulator

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.YearMonth

@RestController
@RequestMapping("/simulator/api/v1")
class CardCompanySimulatorController {

    @GetMapping("/cards/{cardId}/transactions")
    fun getTransactions(
        @PathVariable cardId: String,
        @RequestParam yearMonth: String,
        @RequestHeader(value = "X-Api-Key", required = false) apiKey: String?,
        @RequestHeader(value = "X-Simulate-Error", required = false) simulateError: String?,
    ): List<TransactionResponse> {
        verifyApiKey(apiKey)
        simulateErrorIfRequested(simulateError)

        val month = YearMonth.parse(yearMonth)
        val pool = getMerchantPoolForCard(cardId)
        if (pool.isEmpty()) return emptyList()

        return generateTransactions(cardId, month, pool)
    }

    private fun verifyApiKey(apiKey: String?) {
        if (apiKey.isNullOrBlank()) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Api-Key header")
        if (apiKey != VALID_API_KEY) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid API key")
    }

    private fun simulateErrorIfRequested(simulateError: String?) {
        when (simulateError?.uppercase()) {
            "TIMEOUT" -> try { Thread.sleep(10_000) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
            "500" -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated server error")
            "503" -> throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated service unavailable")
            "429" -> throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Simulated rate limit exceeded")
            else -> {}
        }
    }

    private fun generateTransactions(cardId: String, month: YearMonth, pool: List<MerchantPool>): List<TransactionResponse> {
        val monthSeed = month.year * 100 + month.monthValue
        val txnCount = 15 + (monthSeed * 7 + cardId.hashCode()) % 11

        return (0 until txnCount).map { i ->
            val hash = scramble(monthSeed, cardId.hashCode(), i)
            val merchant = pool[Math.abs(hash) % pool.size]
            val day = 1 + Math.abs(scramble(hash, i, 3)) % month.lengthOfMonth()
            val amountJitter = (Math.abs(scramble(hash, i, 7)) % 5 - 2) * merchant.amountUnit
            val amount = maxOf(merchant.amountUnit, merchant.baseAmount + amountJitter)

            TransactionResponse(
                txnId = "TXN-$cardId-$monthSeed-${"%03d".format(i + 1)}",
                cardNumber = cardId,
                approvalAmount = amount,
                transactionDate = month.atDay(day).toString(),
                merchantName = merchant.merchantName,
                businessType = merchant.businessType,
                paymentMethods = merchant.paymentMethods,
            )
        }
    }

    private fun scramble(a: Int, b: Int, c: Int): Int {
        var h = a * 31 + b
        h = h * 37 + c
        h = h xor (h ushr 16)
        h *= 0x45d9f3b
        h = h xor (h ushr 16)
        return h
    }

    private fun getMerchantPoolForCard(cardId: String): List<MerchantPool> =
        CARD_MERCHANT_POOLS.getOrDefault(cardId, emptyList())

    data class TransactionResponse(
        val txnId: String,
        val cardNumber: String,
        val approvalAmount: Long,
        val transactionDate: String,
        val merchantName: String,
        val businessType: String,
        val paymentMethods: List<String>,
    )

    private data class MerchantPool(
        val merchantName: String,
        val businessType: String,
        val baseAmount: Long,
        val amountUnit: Long,
        val paymentMethods: List<String>,
    )

    companion object {
        private const val VALID_API_KEY = "sim-api-key-2026"

        private fun merchant(
            merchantName: String, businessType: String, baseAmount: Long, amountUnit: Long, paymentMethods: List<String>,
        ) = MerchantPool(merchantName, businessType, baseAmount, amountUnit, paymentMethods)

        private val CARD_MERCHANT_POOLS = mapOf(
            "SAMSUNG_KPASS" to listOf(
                merchant("서울교통공사", "대중교통", 1250, 50, emptyList()),
                merchant("경기도버스", "대중교통", 1200, 50, emptyList()),
                merchant("인천교통공사", "대중교통", 1350, 50, emptyList()),
                merchant("카카오T 택시", "교통", 8500, 500, emptyList()),
                merchant("CU 강남역점", "편의점", 4500, 500, emptyList()),
                merchant("GS25 역삼점", "편의점", 3200, 300, emptyList()),
                merchant("이마트24 선릉점", "편의점", 2800, 200, emptyList()),
                merchant("세븐일레븐 삼성점", "편의점", 3600, 400, emptyList()),
                merchant("다이소 강남점", "생활용품", 5500, 500, emptyList()),
                merchant("교보문고 광화문점", "서적", 18000, 2000, emptyList()),
            ),
            "KB_NORI2_KBPAY" to listOf(
                merchant("스타벅스 강남점", "카페", 5500, 500, listOf("KB_PAY")),
                merchant("스타벅스 역삼점", "카페", 6200, 500, listOf("KB_PAY")),
                merchant("이디야 서초점", "카페", 4300, 300, listOf("KB_PAY")),
                merchant("투썸플레이스 선릉점", "카페", 6500, 500, listOf("KB_PAY")),
                merchant("CGV 왕십리", "영화", 14000, 1000, listOf("KB_PAY")),
                merchant("롯데시네마 건대", "영화", 13000, 1000, listOf("KB_PAY")),
                merchant("메가박스 코엑스", "영화", 15000, 1000, listOf("KB_PAY")),
                merchant("쿠팡", "온라인쇼핑", 35000, 5000, listOf("KB_PAY", "ONLINE")),
                merchant("올리브영 강남점", "드럭스토어", 22000, 3000, listOf("KB_PAY")),
                merchant("GS25 역삼점", "편의점", 3500, 500, listOf("KB_PAY")),
                merchant("배스킨라빈스 강남점", "디저트", 7800, 500, listOf("KB_PAY")),
            ),
            "KB_MY_WESH" to listOf(
                merchant("넷플릭스", "OTT", 17000, 0, listOf("ONLINE", "SUBSCRIPTION")),
                merchant("웨이브", "OTT", 7900, 0, listOf("ONLINE", "SUBSCRIPTION")),
                merchant("멜론", "음악", 10900, 0, listOf("ONLINE", "SUBSCRIPTION")),
                merchant("디즈니플러스", "OTT", 9900, 0, listOf("ONLINE", "SUBSCRIPTION")),
                merchant("롯데월드", "놀이공원", 52000, 5000, emptyList()),
                merchant("에버랜드", "놀이공원", 58000, 5000, emptyList()),
                merchant("CGV 용산", "영화", 28000, 3000, emptyList()),
                merchant("배달의민족", "배달", 23500, 3000, listOf("ONLINE")),
                merchant("요기요", "배달", 19800, 2000, listOf("ONLINE")),
                merchant("스타필드 코엑스", "쇼핑", 45000, 5000, emptyList()),
                merchant("방탈출카페 홍대", "여가", 22000, 2000, emptyList()),
            ),
            "HYUNDAI_ZERO_POINT" to listOf(
                merchant("쿠팡", "온라인쇼핑", 42000, 5000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("11번가", "온라인쇼핑", 28500, 3000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("네이버쇼핑", "온라인쇼핑", 15700, 2000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("무신사", "온라인쇼핑", 67000, 5000, listOf("ONLINE")),
                merchant("마켓컬리", "온라인쇼핑", 38200, 3000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("카카오페이 택시", "교통", 8400, 1000, listOf("SIMPLE_PAY_ONLINE")),
                merchant("SSG닷컴", "온라인쇼핑", 52000, 5000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("올리브영 온라인", "온라인쇼핑", 31000, 3000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("배달의민족", "배달", 21000, 2000, listOf("ONLINE", "SIMPLE_PAY_ONLINE")),
                merchant("토스페이 결제", "간편결제", 12500, 1500, listOf("SIMPLE_PAY_ONLINE")),
            ),
        )
    }
}

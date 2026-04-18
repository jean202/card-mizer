package com.jean202.cardmizer.api.cards

import com.jean202.cardmizer.core.domain.Card
import com.jean202.cardmizer.core.domain.CardId
import com.jean202.cardmizer.core.domain.CardType
import com.jean202.cardmizer.core.domain.PriorityStrategy
import com.jean202.cardmizer.core.port.`in`.GetCardsUseCase
import com.jean202.cardmizer.core.port.`in`.RegisterCardUseCase
import com.jean202.cardmizer.core.port.`in`.UpdatePriorityUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/api/cards")
class CardManagementController(
    private val getCardsUseCase: GetCardsUseCase,
    private val registerCardUseCase: RegisterCardUseCase,
    private val updatePriorityUseCase: UpdatePriorityUseCase,
) {
    @GetMapping
    fun getAll(): List<CardResponse> = getCardsUseCase.getAll().map { CardResponse.from(it) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterCardRequest): RegisterCardResponse {
        val card = request.toDomain()
        registerCardUseCase.register(card)
        return RegisterCardResponse.from(card)
    }

    @PatchMapping("/priorities")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updatePriorities(@Valid @RequestBody request: UpdatePrioritiesRequest) {
        updatePriorityUseCase.update(request.toDomain())
    }

    data class RegisterCardRequest(
        @field:NotBlank(message = "cardId must not be blank")
        val cardId: String,
        @field:NotBlank(message = "issuerName must not be blank")
        val issuerName: String,
        @field:NotBlank(message = "productName must not be blank")
        val productName: String,
        @field:NotBlank(message = "cardType must not be blank")
        @field:Pattern(regexp = "(?i)^(CREDIT|CHECK)$", message = "cardType must be CREDIT or CHECK")
        val cardType: String,
        @field:Positive(message = "priority must be greater than 0")
        val priority: Int,
    ) {
        fun toDomain() = Card(
            id = CardId(cardId),
            issuerName = issuerName,
            productName = productName,
            cardType = CardType.valueOf(cardType.trim().uppercase(Locale.ROOT)),
            priority = priority,
        )
    }

    data class RegisterCardResponse(
        val cardId: String,
        val cardName: String,
        val cardType: String,
        val priority: Int,
    ) {
        companion object {
            fun from(card: Card) = RegisterCardResponse(
                cardId = card.id.value,
                cardName = card.displayName(),
                cardType = card.cardType.name,
                priority = card.priority,
            )
        }
    }

    data class CardResponse(
        val cardId: String,
        val issuerName: String,
        val productName: String,
        val cardType: String,
        val priority: Int,
    ) {
        companion object {
            fun from(card: Card) = CardResponse(
                cardId = card.id.value,
                issuerName = card.issuerName,
                productName = card.productName,
                cardType = card.cardType.name,
                priority = card.priority,
            )
        }
    }

    data class UpdatePrioritiesRequest(
        @field:NotEmpty(message = "orderedCardIds must not be empty")
        val orderedCardIds: List<@NotBlank(message = "orderedCardIds must not contain blank values") String>,
    ) {
        fun toDomain() = PriorityStrategy(orderedCardIds.map { CardId(it) })
    }
}

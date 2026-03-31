package com.jean202.cardmizer.api.cards;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.domain.CardId;
import com.jean202.cardmizer.core.domain.CardType;
import com.jean202.cardmizer.core.domain.PriorityStrategy;
import com.jean202.cardmizer.core.port.in.RegisterCardUseCase;
import com.jean202.cardmizer.core.port.in.UpdatePriorityUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class CardManagementController {
    private final RegisterCardUseCase registerCardUseCase;
    private final UpdatePriorityUseCase updatePriorityUseCase;

    public CardManagementController(
            RegisterCardUseCase registerCardUseCase,
            UpdatePriorityUseCase updatePriorityUseCase
    ) {
        this.registerCardUseCase = registerCardUseCase;
        this.updatePriorityUseCase = updatePriorityUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterCardResponse register(@Valid @RequestBody RegisterCardRequest request) {
        Card card = request.toDomain();
        registerCardUseCase.register(card);
        return RegisterCardResponse.from(card);
    }

    @PatchMapping("/priorities")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePriorities(@Valid @RequestBody UpdatePrioritiesRequest request) {
        updatePriorityUseCase.update(request.toDomain());
    }

    public record RegisterCardRequest(
            @NotBlank(message = "cardId must not be blank")
            String cardId,
            @NotBlank(message = "issuerName must not be blank")
            String issuerName,
            @NotBlank(message = "productName must not be blank")
            String productName,
            @NotBlank(message = "cardType must not be blank")
            @Pattern(regexp = "(?i)^(CREDIT|CHECK)$", message = "cardType must be CREDIT or CHECK")
            String cardType,
            @Positive(message = "priority must be greater than 0")
            int priority
    ) {
        Card toDomain() {
            return new Card(
                    new CardId(cardId),
                    issuerName,
                    productName,
                    parseCardType(cardType),
                    priority
            );
        }

        private CardType parseCardType(String rawCardType) {
            if (rawCardType == null || rawCardType.isBlank()) {
                throw new IllegalArgumentException("cardType must not be blank");
            }

            try {
                return CardType.valueOf(rawCardType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unsupported cardType: " + rawCardType, exception);
            }
        }
    }

    public record RegisterCardResponse(
            String cardId,
            String cardName,
            String cardType,
            int priority
    ) {
        static RegisterCardResponse from(Card card) {
            return new RegisterCardResponse(
                    card.id().value(),
                    card.displayName(),
                    card.cardType().name(),
                    card.priority()
            );
        }
    }

    public record UpdatePrioritiesRequest(
            @NotEmpty(message = "orderedCardIds must not be empty")
            List<@NotBlank(message = "orderedCardIds must not contain blank values") String> orderedCardIds
    ) {
        PriorityStrategy toDomain() {
            if (orderedCardIds == null) {
                throw new IllegalArgumentException("orderedCardIds must not be null");
            }

            return new PriorityStrategy(orderedCardIds.stream()
                    .map(CardId::new)
                    .toList());
        }
    }
}

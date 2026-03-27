package com.jean202.cardmizer.infra.persistence;

import com.jean202.cardmizer.core.domain.Card;
import com.jean202.cardmizer.core.port.out.CardPolicyRepository;
import java.util.List;

public class InMemoryCardPolicyRepository implements CardPolicyRepository {
    @Override
    public List<Card> findAll() {
        return List.of(
                new Card("SAMSUNG", "삼성카드", 1),
                new Card("KB_CREDIT", "국민카드 신용", 2),
                new Card("KB_CHECK", "국민카드 체크", 3),
                new Card("HYUNDAI", "현대카드", 4)
        );
    }
}

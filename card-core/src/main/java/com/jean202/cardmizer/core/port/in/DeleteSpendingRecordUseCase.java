package com.jean202.cardmizer.core.port.in;

import java.util.UUID;

public interface DeleteSpendingRecordUseCase {
    void delete(UUID id);
}

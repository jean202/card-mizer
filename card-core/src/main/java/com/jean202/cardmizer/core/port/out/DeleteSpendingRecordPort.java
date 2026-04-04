package com.jean202.cardmizer.core.port.out;

import java.util.UUID;

public interface DeleteSpendingRecordPort {
    void delete(UUID id);
}

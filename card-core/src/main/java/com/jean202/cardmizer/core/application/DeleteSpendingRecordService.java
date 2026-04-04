package com.jean202.cardmizer.core.application;

import com.jean202.cardmizer.core.port.in.DeleteSpendingRecordUseCase;
import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort;
import java.util.Objects;
import java.util.UUID;

public class DeleteSpendingRecordService implements DeleteSpendingRecordUseCase {
    private final DeleteSpendingRecordPort deleteSpendingRecordPort;

    public DeleteSpendingRecordService(DeleteSpendingRecordPort deleteSpendingRecordPort) {
        this.deleteSpendingRecordPort = Objects.requireNonNull(
                deleteSpendingRecordPort,
                "deleteSpendingRecordPort must not be null"
        );
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        deleteSpendingRecordPort.delete(id);
    }
}

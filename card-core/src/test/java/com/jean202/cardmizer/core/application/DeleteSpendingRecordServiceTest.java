package com.jean202.cardmizer.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeleteSpendingRecordServiceTest {

    @Test
    void deleteExistingRecordDelegatesToPort() {
        CapturingDeletePort port = new CapturingDeletePort(Set.of(UUID.randomUUID()));
        DeleteSpendingRecordService service = new DeleteSpendingRecordService(port);
        UUID targetId = port.knownIds.iterator().next();

        service.delete(targetId);

        assertEquals(targetId, port.deletedId);
    }

    @Test
    void deleteNonExistentRecordThrowsResourceNotFoundException() {
        DeleteSpendingRecordPort port = id -> {
            throw new ResourceNotFoundException("Spending record not found: " + id);
        };
        DeleteSpendingRecordService service = new DeleteSpendingRecordService(port);

        UUID unknownId = UUID.randomUUID();
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(unknownId)
        );
        assertEquals("Spending record not found: " + unknownId, exception.getMessage());
    }

    @Test
    void deleteAlreadyDeletedRecordThrowsResourceNotFoundException() {
        UUID recordId = UUID.randomUUID();
        Set<UUID> deletedAlready = new HashSet<>();
        deletedAlready.add(recordId);

        DeleteSpendingRecordPort port = id -> {
            if (deletedAlready.contains(id)) {
                throw new ResourceNotFoundException("Spending record not found: " + id);
            }
        };
        DeleteSpendingRecordService service = new DeleteSpendingRecordService(port);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(recordId));
    }

    private static final class CapturingDeletePort implements DeleteSpendingRecordPort {
        private final Set<UUID> knownIds;
        private UUID deletedId;

        CapturingDeletePort(Set<UUID> knownIds) {
            this.knownIds = new HashSet<>(knownIds);
        }

        @Override
        public void delete(UUID id) {
            if (!knownIds.contains(id)) {
                throw new ResourceNotFoundException("Spending record not found: " + id);
            }
            this.deletedId = id;
        }
    }
}

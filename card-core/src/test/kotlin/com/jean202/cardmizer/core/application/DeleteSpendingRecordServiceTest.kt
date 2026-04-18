package com.jean202.cardmizer.core.application

import com.jean202.cardmizer.core.port.out.DeleteSpendingRecordPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteSpendingRecordServiceTest {

    @Test
    fun deleteExistingRecordDelegatesToPort() {
        val port = CapturingDeletePort(setOf(UUID.randomUUID()))
        val service = DeleteSpendingRecordService(port)
        val targetId = port.knownIds.iterator().next()

        service.delete(targetId)

        assertEquals(targetId, port.deletedId)
    }

    @Test
    fun deleteNonExistentRecordThrowsResourceNotFoundException() {
        val port = DeleteSpendingRecordPort { id ->
            throw ResourceNotFoundException("Spending record not found: $id")
        }
        val service = DeleteSpendingRecordService(port)

        val unknownId = UUID.randomUUID()
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            service.delete(unknownId)
        }
        assertEquals("Spending record not found: $unknownId", exception.message)
    }

    @Test
    fun deleteAlreadyDeletedRecordThrowsResourceNotFoundException() {
        val recordId = UUID.randomUUID()
        val deletedAlready = mutableSetOf(recordId)

        val port = DeleteSpendingRecordPort { id ->
            if (id in deletedAlready) {
                throw ResourceNotFoundException("Spending record not found: $id")
            }
        }
        val service = DeleteSpendingRecordService(port)

        assertThrows(ResourceNotFoundException::class.java) { service.delete(recordId) }
    }

    private class CapturingDeletePort(knownIds: Set<UUID>) : DeleteSpendingRecordPort {
        val knownIds: MutableSet<UUID> = HashSet(knownIds)
        var deletedId: UUID? = null

        override fun delete(id: UUID) {
            if (id !in knownIds) throw ResourceNotFoundException("Spending record not found: $id")
            deletedId = id
        }
    }
}

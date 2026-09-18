package sajili.agent.erp

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import sajili.agent.inventory.entity.InventoryTransactionEntity
import sajili.agent.inventory.entity.SyncStatus
import sajili.agent.inventory.repository.InventoryTransactionRepository
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.pow

@Component
class ErpSyncWorker(
    private val repository: InventoryTransactionRepository,
    private val syncService: ErpTransactionSyncService
) {
    @Scheduled(fixedDelay = 180000, initialDelay = 30000) // Polls every 3 minutes
    fun synchronizePendingTransactions() {
        val batch = repository.findBatchForSync(OffsetDateTime.now(), 100)
        batch.forEach { tx ->
            try {
                syncService.syncSingleTransaction(tx.id!!)
            } catch (ex: Exception) {
                // Catches isolated loop exceptions to protect the batch loop execution
            }
        }
    }
}

@Service
class ErpTransactionSyncService(
    private val repository: InventoryTransactionRepository,
    private val erpClient: MicrosoftErpClient
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun syncSingleTransaction(id: UUID) {
        val tx = repository.findById(id).orElse(null) ?: return

        try {
            erpClient.postItemJournalEntry(tx)
            tx.syncStatus = SyncStatus.SYNCED
            tx.erpSyncError = null
            tx.nextRetryAt = null
        } catch (ex: MicrosoftErpClient.ErpApiException) {
            handleError(tx, ex.statusCode, ex.message)
        } catch (ex: Exception) {
            handleError(tx, 500, ex.message ?: "Network failure")
        }
        repository.save(tx)
    }

    private fun handleError(tx: InventoryTransactionEntity, statusCode: Int, errorMsg: String?) {
        tx.syncRetryCount += 1
        tx.erpSyncError = errorMsg?.take(2000)

        // Short-circuit permanent client/payload errors (4xx) directly to dead-letter state
        if (statusCode in 400..499 || tx.syncRetryCount >= 5) {
            tx.syncStatus = SyncStatus.FAILED_PERMANENT
            tx.nextRetryAt = null
        } else {
            tx.syncStatus = SyncStatus.FAILED_RETRYABLE
            // Exponential backoff strategy: 60s * 2^retryCount
            val backoff = (60.0 * 2.0.pow(tx.syncRetryCount.toDouble())).toLong()
            tx.nextRetryAt = OffsetDateTime.now().plusSeconds(backoff)
        }
    }
}
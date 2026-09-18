package sajili.agent.inventory.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import sajili.agent.inventory.entity.InventoryTransactionEntity
import java.time.OffsetDateTime
import java.util.UUID

//records immutable transaction logs and supports idempotency
//checks for offline-first Android retries
interface InventoryTransactionRepository: JpaRepository<InventoryTransactionEntity, UUID>{
    fun findByIdempotencyKey(idempotencyKey: String):InventoryTransactionEntity?
    fun findByTenantIdAndWarehouseId(tenantId:UUID, warehouseId: UUID): List<InventoryTransactionEntity>

//the function below claims an un-synced batch of transactions for the microsoft ERp synchronization
//    FOR UPDATE SKIP LOCKED ->ALLOW MULTIPLE SERVICE INSTANCES

    @Query(
        value = """
            SELECT * FROM inventory_transactions 
            WHERE sync_status IN ('PENDING', 'FAILED_RETRYABLE')
              AND (next_retry_at IS NULL OR next_retry_at <= :now)
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findBatchForSync(
        @Param("now") now: OffsetDateTime,
        @Param("batchSize") batchSize: Int
    ): List<InventoryTransactionEntity>





}
//the idempotency key is used to prevent duplicate posts from offline-first Android retries
//the second function fetches transaction logs isolated by tenant and warehouse boundary
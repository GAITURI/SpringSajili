package sajili.agent.inventory.repository

import org.springframework.data.jpa.repository.JpaRepository
import sajili.agent.inventory.entity.InventoryTransactionEntity
import java.util.UUID

//records immutable transaction logs and supports idempotency
//checks for offline-first Android retries
interface InventoryTransactionRepository: JpaRepository<InventoryTransactionEntity, UUID>{
    fun findByIdempotencyKey(idempotencyKey: String):InventoryTransactionEntity?
    fun findByTenantIdAndWarehouseId(tenantId:UUID, warehouseId: UUID): List<InventoryTransactionEntity>
}
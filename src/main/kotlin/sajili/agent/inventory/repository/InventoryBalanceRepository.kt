package sajili.agent.inventory.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import sajili.agent.inventory.entity.InventoryBalanceEntity
import java.util.UUID

interface InventoryBalanceRepository : JpaRepository<InventoryBalanceEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBalanceEntity b WHERE b.warehouseId = :warehouseId AND b.productId = :productId")
    fun findByTenantIdAndWarehouseIDAndProductIdLocked(
        @Param("warehouseId") warehouseId: UUID,
        @Param("productId") productId: UUID
    ): InventoryBalanceEntity?
}


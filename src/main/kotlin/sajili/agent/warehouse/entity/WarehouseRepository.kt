package sajili.agent.warehouse.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface WarehouseRepository: JpaRepository<WarehouseEntity, UUID> {
    fun findByTenantId(tenantId: UUID):List<WarehouseEntity>
}
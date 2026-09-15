package sajili.agent.inventory.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "inventory_balances",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "warehouse_id", "product_id"])]
)
class InventoryBalanceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,



    @Column(name = "warehouse_id", nullable = false)
    val warehouseId: UUID,


    @Column(name = "product_id", nullable = false)
    val productId: UUID,

    @Column(nullable = false)
    var quantity: BigDecimal,

    @Version
    var version: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
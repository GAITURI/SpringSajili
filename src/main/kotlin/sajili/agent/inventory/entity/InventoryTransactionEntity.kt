package sajili.agent.inventory.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID




enum class SyncStatus{
    PENDING,
    SYNCED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}



@Entity
@Table(name = "inventory_transactions")
class InventoryTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "warehouse_id", nullable = false)
    val warehouseId: UUID,

    @Column(name = "product_id", nullable = false)
    val productId: UUID,

    @Column(name = "transaction_type", nullable = false)
    val transactionType: String,

    @Column(nullable = false)
    val quantity: BigDecimal,

    @Column(name = "previous_balance", nullable = false)
    val previousBalance: BigDecimal,

    @Column(name = "resulting_balance", nullable = false)
    val resultingBalance: BigDecimal,

    @Column(name = "reference_id")
    val referenceId: String?,

    @Column(name = "performed_by", nullable = false)
    val performedBy: String,

    @Column(name = "idempotency_key", unique = true)
    val idempotencyKey: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
     var syncStatus: SyncStatus= SyncStatus.PENDING,

    @Column(nullable = false)
    var syncRetryCount:Int =0,

    @Column(columnDefinition = "TEXT")
    var erpSyncError:String? =null,

    var nextRetryAt: OffsetDateTime?=null,
)
package sajili.agent.inventory.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

//this file defines the structured responses sent back to the Android App containing
//stable transaction IDS, resulting balances after server-side calculation

data class InventoryTransactionResponse(
    val transactionID: UUID,
    val tenantId: UUID,
    val warehouseId:UUID,
    val productId:UUID,
    val transactionType: String,
    val quantity: BigDecimal,
    val previousBalance: BigDecimal,
    val referenceId: String?,
    val performedBy:String,
    val createdAt: OffsetDateTime
)

data class InventoryBalanceResponse(
    val id: UUID,
    val tenantId: UUID,
    val warehouseId: UUID,
    val productId: UUID,
    val quantity: BigDecimal,
    val version: Long,
    val updatedAt: OffsetDateTime
)
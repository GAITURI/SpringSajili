package sajili.agent.inventory.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class ReceiveStockRequest(
    @field:NotNull(message="Product ID is required")
    val productId: UUID,

    @field:NotNull(message="Warehouse ID is required")
    val warehouseId: UUID,

    @field:NotNull(message="Quantity is required")
    @field:DecimalMin(value="0.0001", message="Quantity must be greater than zero")
    val quantity: BigDecimal,

    val referenceId:String?,
    val idempotencyKey: String?,
    val notes: String?
)


data class TransferStockRequest(
    @field:NotNull(message="Product ID is required")
    val productId: UUID,

    @field:NotNull(message="Destination Warehouse ID is required")
    val destinationWarehouseId: UUID,
     val sourceWarehouseId:UUID,
    @field:NotNull(message="Quantity is required")
    @field:DecimalMin(value="0.0001", message="Quantity must be greater than zero")
    val quantity: BigDecimal,
    val idempotencyKey: String?,
    val referenceId:String?

)
data class IssueStockRequest(
    @field:NotNull(message="Product ID is required")
    val productId: UUID,

    @field:NotNull(message="Warehouse ID is required")
    val warehouseId: UUID,

    @field:NotNull(message="Quantity is required")
    @field:DecimalMin(value="0.0001", message="Quantity must be greater than zero")
    val quantity: BigDecimal,
    val idempotencyKey: String?,
    val referenceId:String?
)
data class AdjustStockRequest(
    @field:NotNull(message="Product ID is required")
    val productId: UUID,
    val countedQuantity: BigDecimal,
    @field:NotNull(message="Warehouse ID is required")
    val warehouseId: UUID,

    @field:NotNull(message="Quantity is required")
    @field:DecimalMin(value="0.0001", message="Quantity must be greater than zero")
    val quantity: BigDecimal,
    val idempotencyKey: String?,
    val referenceId:String?
)

//this file contains the incoming payloads from my Android retrofit client for the core inventory operations
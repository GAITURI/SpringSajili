package sajili.agent.inventory.service

import com.google.firebase.database.core.view.View
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import sajili.agent.inventory.dto.AdjustStockRequest
import sajili.agent.inventory.dto.IssueStockRequest
import sajili.agent.inventory.dto.ReceiveStockRequest
import sajili.agent.inventory.dto.TransferStockRequest
import sajili.agent.inventory.entity.InventoryBalanceEntity
import sajili.agent.inventory.entity.InventoryTransactionEntity
import sajili.agent.inventory.entity.OperationResults
import sajili.agent.inventory.repository.InventoryBalanceRepository
import sajili.agent.inventory.repository.InventoryTransactionRepository
import sajili.agent.product.entity.ProductRepository
import sajili.agent.security.CurrentUserProvider
import sajili.agent.warehouse.entity.WarehouseRepository
import java.math.BigDecimal


@Service
class InventoryService(
    private val currentUserProvider: CurrentUserProvider,
    private val securityValidator: InventorySecurityValidator,
    private val balanceRepository: InventoryBalanceRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository

)

{
    @Transactional
    fun receiveStock(request: ReceiveStockRequest): OperationResults<InventoryTransactionEntity> {
        transactionRepository.findByIdempotencyKey(request.idempotencyKey.toString())?.let{
            return OperationResults(it, isNew = false)
        }
        val currentUser= currentUserProvider.getCurrentUser()

//        enforce tenant and warehouse boundaries
        securityValidator.validateWarehouseAccess(currentUser, request.warehouseId)

        val product =productRepository.findById(request.productId)
            .orElseThrow{ IllegalArgumentException("Product not found: ${request.productId}") }
        val warehouse= warehouseRepository.findById(request.warehouseId)
            .orElseThrow{ IllegalArgumentException("Warehouse not found:${request.warehouseId}") }


//        acquire pessimistic write lock on the Inventory Balance Row
        var balance= balanceRepository.findByTenantIdAndWarehouseIDAndProductIdLocked(request.warehouseId,request.productId)
        if (balance== null){
//            if balance record doesn't exist yet for this product in this warehouse, create it
            balance= InventoryBalanceEntity(
                tenantId = currentUser.tenantId,
                warehouseId= warehouse.id!!,
                productId =product.id!!,
                quantity= BigDecimal.ZERO
            )
        }
//Mutate state
        val previousQuantity= balance.quantity
        balance.quantity += request.quantity
        balanceRepository.save(balance)


//        write Immutable Ledger Entry (Audit trail)
        val transaction = InventoryTransactionEntity(
            tenantId = currentUser.tenantId,
            warehouseId = warehouse.id!!,
            productId = product.id!!,
            transactionType = "RECEIPT",
            quantity= BigDecimal.valueOf(request.quantity.toLong()),
            previousBalance = previousQuantity,
            resultingBalance = balance.quantity,
            referenceId = request.referenceId,
            performedBy= currentUser.subject,
            idempotencyKey = request.idempotencyKey


        )
        val savedTransaction=transactionRepository.save(transaction)
        return OperationResults(savedTransaction, isNew = true)
    }


    @Transactional
    fun issueStock(request: IssueStockRequest): OperationResults<InventoryTransactionEntity>{
        // Check if this is a network retry (idempotency hit)
        transactionRepository.findByIdempotencyKey(request.idempotencyKey.toString())?.let {
            return OperationResults(it, isNew = false)
        }
        val currentUser= currentUserProvider.getCurrentUser()

        securityValidator.validateWarehouseAccess(currentUser, request.warehouseId)

        val product= productRepository.findById(request.productId)
            .orElseThrow{ IllegalArgumentException("Product not found: ${request.productId}") }

        val warehouse= warehouseRepository.findById(request.warehouseId)
            .orElseThrow{ IllegalArgumentException("Warehouse not found: ${request.warehouseId}") }

//   Lock balance row to prevent race conditions during concurrent issues
        val balance = balanceRepository.findByTenantIdAndWarehouseIDAndProductIdLocked(
            request.warehouseId, request.productId,
        )
            ?:throw IllegalStateException("Inventory balance record does not exist for this product in this warehouse")
        if(balance.quantity < request.quantity){
            throw IllegalStateException("Insufficient stock. Available{${balance.quantity},Requested: ${request.quantity}}")
        }
        val previousQuantity =balance.quantity
        balance.quantity -= request.quantity
        balanceRepository.save(balance)

//this block captures who, what, when, where, and why an inventory change occurred
        val transaction= InventoryTransactionEntity(
            tenantId = currentUser.tenantId,
            warehouseId = warehouse.id!!,
            productId = product.id!!,
            transactionType = "Receipt",
            quantity = request.quantity,
            previousBalance = previousQuantity,
            resultingBalance = balance.quantity,
            referenceId = request.referenceId,
            performedBy = currentUser.subject,
            idempotencyKey = request.idempotencyKey,

        )
        val savedTransaction = transactionRepository.save(transaction)
        return OperationResults(savedTransaction, isNew = true)
    }
    @Transactional
    fun adjustStock(request: AdjustStockRequest): InventoryTransactionEntity {
        val currentUser = currentUserProvider.getCurrentUser()
        securityValidator.validateWarehouseAccess(currentUser, request.warehouseId)

        val product = productRepository.findById(request.productId)
            .orElseThrow { IllegalArgumentException("Product not found: ${request.productId}") }
        val warehouse = warehouseRepository.findById(request.warehouseId)
            .orElseThrow { IllegalArgumentException("Warehouse not found: ${request.warehouseId}") }

        // 1. Lock the balance row
        val balance = balanceRepository.findByTenantIdAndWarehouseIDAndProductIdLocked(request.warehouseId, request.productId)
            ?: throw IllegalStateException("Inventory balance record does not exist for this product in this warehouse.")

        val previousQuantity = balance.quantity

        // 2. Set the quantity directly to the physical count provided in the request
        val quantityDifference = request.countedQuantity.subtract(previousQuantity)
        balance.quantity = request.countedQuantity
        balanceRepository.save(balance)

        // 3. Write immutable audit log classifying it as an adjustment
        val transaction = InventoryTransactionEntity(
            tenantId = currentUser.tenantId,
            warehouseId = warehouse.id!!,
            productId = product.id!!,
            transactionType = "ADJUSTMENT",
            quantity = quantityDifference.abs(), // Records the absolute magnitude of the correction
            previousBalance = previousQuantity,
            resultingBalance = balance.quantity,
            referenceId = request.referenceId,
            performedBy = currentUser.subject,
            idempotencyKey = request.idempotencyKey
        )

        return transactionRepository.save(transaction)
    }

    @Transactional
    fun transferStock(request: TransferStockRequest): List<InventoryTransactionEntity> {
        val currentUser = currentUserProvider.getCurrentUser()

        // Validate user has access to BOTH the source and destination warehouses
        securityValidator.validateWarehouseAccess(currentUser, request.sourceWarehouseId)
        securityValidator.validateWarehouseAccess(currentUser, request.destinationWarehouseId)

        if (request.sourceWarehouseId == request.destinationWarehouseId) {
            throw IllegalArgumentException("Source and destination warehouses cannot be the same.")
        }

        val product = productRepository.findById(request.productId)
            .orElseThrow { IllegalArgumentException("Product not found: ${request.productId}") }
        val sourceWarehouse = warehouseRepository.findById(request.sourceWarehouseId)
            .orElseThrow { IllegalArgumentException("Source warehouse not found") }
        val destWarehouse = warehouseRepository.findById(request.destinationWarehouseId)
            .orElseThrow { IllegalArgumentException("Destination warehouse not found") }

        // 1. Deduct from Source Warehouse
        val sourceBalance = balanceRepository.findByTenantIdAndWarehouseIDAndProductIdLocked(request.sourceWarehouseId, request.productId,)
            ?: throw IllegalStateException("No inventory balance found at source warehouse.")

        if (sourceBalance.quantity < request.quantity) {
            throw IllegalStateException("Insufficient stock for transfer. Available: ${sourceBalance.quantity}, Requested: ${request.quantity}")
        }

        val sourcePrev = sourceBalance.quantity
        sourceBalance.quantity = sourceBalance.quantity.subtract(request.quantity)
        balanceRepository.save(sourceBalance)

        // 2. Add to Destination Warehouse (create record if it doesn't exist yet)
        var destBalance = balanceRepository.findByTenantIdAndWarehouseIDAndProductIdLocked(request.destinationWarehouseId, request.productId)
        if (destBalance == null) {
            destBalance = InventoryBalanceEntity(
                tenantId = currentUser.tenantId,
                warehouseId = destWarehouse.id!!,
                productId = product.id!!,
                quantity = BigDecimal.ZERO
            )
        }

        val destPrev = destBalance.quantity
        destBalance.quantity = destBalance.quantity.add(request.quantity)
        balanceRepository.save(destBalance)

        // 3. Create paired audit trail logs for both warehouses
        val outTransaction = InventoryTransactionEntity(
            tenantId = currentUser.tenantId,
            warehouseId = sourceWarehouse.id!!,
            productId = product.id!!,
            transactionType = "TRANSFER_OUT",
            quantity = request.quantity,
            previousBalance = sourcePrev,
            resultingBalance = sourceBalance.quantity,
            referenceId = request.referenceId,
            performedBy = currentUser.subject,
            idempotencyKey = "${request.idempotencyKey}-out"
        )

        val inTransaction = InventoryTransactionEntity(
            tenantId = currentUser.tenantId,
            warehouseId = destWarehouse.id!!,
            productId = product.id!!,
            transactionType = "TRANSFER_IN",
            quantity = request.quantity,
            previousBalance = destPrev,
            resultingBalance = destBalance.quantity,
            referenceId = request.referenceId,
            performedBy = currentUser.subject,
            idempotencyKey = "${request.idempotencyKey}-in"
        )

        return listOf(
            transactionRepository.save(outTransaction),
            transactionRepository.save(inTransaction)
        )
    }
}
//the idempotencyKey is a unique token generated by the Android App's local datbase
//the key ensure that if a network glitch causes a retry the backend won't process the same stock movement twice

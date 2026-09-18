package sajili.agent.inventory.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sajili.agent.inventory.dto.AdjustStockRequest
import sajili.agent.inventory.dto.IssueStockRequest
import sajili.agent.inventory.dto.ReceiveStockRequest
import sajili.agent.inventory.dto.TransferStockRequest
import sajili.agent.inventory.entity.InventoryTransactionEntity
import sajili.agent.inventory.service.InventoryService
//When a field agent submits stock movements from the Android app,
// the request lands in InventoryController.
//The controller delegates the work directly to InventoryService.

@RestController
@RequestMapping("/api/inventory")
class InventoryController(private val inventoryService: InventoryService) {

    @PostMapping("/receive")
    fun receiveStock(@RequestBody request: ReceiveStockRequest): ResponseEntity<InventoryTransactionEntity>{
        val transaction= inventoryService.receiveStock(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction)
    }

    @PostMapping("/issue")
    fun issueStock(@RequestBody request: IssueStockRequest): ResponseEntity<InventoryTransactionEntity>{
        val transaction= inventoryService.issueStock(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction)
    }
    @PostMapping("/adjust")
    fun adjustStock(@RequestBody request: AdjustStockRequest): ResponseEntity<InventoryTransactionEntity>{
        val transaction= inventoryService.adjustStock(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction)
    }
    @PostMapping("/transfer")
    fun transferStock(@RequestBody request: TransferStockRequest): ResponseEntity<List<InventoryTransactionEntity>>{
        val transactions= inventoryService.transferStock(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(transactions)
    }
}
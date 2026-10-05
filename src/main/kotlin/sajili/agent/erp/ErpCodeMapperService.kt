package sajili.agent.erp

import org.springframework.stereotype.Service
import sajili.agent.product.entity.ProductRepository
import sajili.agent.warehouse.entity.WarehouseRepository
import java.util.UUID


@Service
class ErpCodeMapperService(
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository


) {

    fun resolveItemCode(productId: UUID): String{
        val product= productRepository.findById(productId).orElse(null)
            ?:throw IllegalArgumentException("No ERP Item Code mapping found for product $productId")
        return product.erpItemCode
    }


    fun resolveWarehouseCode(warehouseId: UUID): String {
        val warehouse= warehouseRepository.findById(warehouseId).orElse(null)
            ?:throw IllegalArgumentException("No ERP Warehouse Location code mapping found for Warehouse $warehouseId")
        return warehouse.erpLocationCode
    }
}
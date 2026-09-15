package sajili.agent.inventory.service

import org.springframework.stereotype.Component
import sajili.agent.security.AuthenticatedUsers
import sajili.agent.warehouse.entity.WarehouseRepository
import java.util.UUID


@Component
class InventorySecurityValidator(private val warehouseRepository: WarehouseRepository) {
    fun validateWarehouseAccess(currentUser: AuthenticatedUsers, targetWarehouseId: UUID){
            //enforce tenant isolation (double-check tenant boundary
//        ensure the warehouse actually belongs to the user's tenant in the database
        val warehouse = warehouseRepository.findById(targetWarehouseId)
            .orElseThrow{ IllegalArgumentException("Warehouse not found with ID: $targetWarehouseId") }

        if (warehouse.tenantId !=currentUser.tenantId){
//            log security warning here in production
            throw SecurityException("Access denied:Warehouse belongs to a different tenant domain.")


        }

//        enforce warehouse-level accesscontorl
        //check if the warehouse UUID is explicitly in the user's authorized set
        if(!currentUser.authorizedWarehouses.contains(targetWarehouseId)){
            throw SecurityException("Access denied:User is not authorized to operate on warehouse: $targetWarehouseId")
        }



    }
}
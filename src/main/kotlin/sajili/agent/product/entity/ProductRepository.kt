package sajili.agent.product.entity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductRepository: JpaRepository<ProductEntity, UUID> {

    fun findByProductId(productId:UUID): List<ProductEntity>
}
package sajili.agent.product.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ProductRepository: JpaRepository<ProductEntity, UUID> {
// No custom query needed; JpaRepository provides findById(UUID) out of the box returning Optional<ProductEntity>
}
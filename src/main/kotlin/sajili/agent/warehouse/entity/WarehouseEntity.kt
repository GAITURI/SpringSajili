package sajili.agent.warehouse.entity


import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "warehouses", uniqueConstraints = [
    UniqueConstraint(columnNames = ["tenant_id", "code"])
])
class WarehouseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val code: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "erp_location_code", nullable = false)
    val erpLocationCode: String
)
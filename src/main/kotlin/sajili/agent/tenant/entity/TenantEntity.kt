package sajili.agent.tenant.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name="tenants")
class TenantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID?= null,

    @Column(nullable= false)
    val name:String,

    @Column(name="created_at", nullable= false)
    val createdAt: OffsetDateTime= OffsetDateTime.now()
) {
}
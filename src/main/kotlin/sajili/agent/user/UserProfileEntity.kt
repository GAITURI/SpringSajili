package sajili.agent.user.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_profiles")
class UserProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "auth_subject", nullable = false, unique = true)
    val authSubject: String,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    val email: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_warehouses", joinColumns = [JoinColumn(name = "user_profile_id")])
    @Column(name = "warehouse_id")
    val authorizedWarehouses: Set<UUID> = emptySet()
)
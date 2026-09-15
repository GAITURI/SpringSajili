package sajili.agent.security

import java.util.UUID

data class AuthenticatedUsers(

    val subject:String,
    val email:String,
    val displayName: String,
    val tenantId: UUID,
    val roles: Set<String>,
    val permissions:Set<String>,
    val authorizedWarehouses: Set<UUID>
)

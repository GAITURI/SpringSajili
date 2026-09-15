package sajili.agent.security

import io.jsonwebtoken.Jwt
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import sajili.agent.user.repository.UserProfileRepository


@Component
class SpringSecurityCurrentUserProvider(private val userProfileRepository: UserProfileRepository): CurrentUserProvider {
    override fun getCurrentUser(): AuthenticatedUsers {
        val authentication= SecurityContextHolder.getContext().authentication
            ?:throw IllegalStateException("No authentication context found in SecurityContext")

        val principal=authentication.principal
        val subject = when (principal){
            is org.springframework.security.oauth2.jwt.Jwt->principal.subject ?: principal.getClaimAsString("sub")
            else->throw IllegalStateException("Unsupported principal type: ${principal::class.java.name}")
        } ?: throw IllegalStateException("Subject cannot be null in JWT token")
//        //fetch user profile and warehouse mappings from the ERP databsse
        val profile= userProfileRepository.findByAuthSubject(subject)
            ?: throw IllegalStateException("User profile not found for identity subject: $subject")
        if(!profile.isActive){
            throw IllegalStateException("User account is disabled")
        }
        //extract permissions from spring security authorities
//        authorities-returns the priviliges granted to the principal
        val permissions= authentication.authorities.map {it.authority}.toSet()
        val roles= permissions.filter {it.startsWith("ROLE_")}.toSet()

        return AuthenticatedUsers(
            subject= profile.authSubject,
            email= profile.email,
            displayName = profile.displayName,
            tenantId = profile.tenantId,
            roles=roles,
            permissions=permissions,
            authorizedWarehouses = profile.authorizedWarehouses
        )






    }
}

//it bridges the gap between the incoming HTTP request's security token and the ERP database
//the UserProfileRepository works is to execute SQL queries against PostgreSQL tables
//the UserProfileRepository maps the user_profiles, and user_warehouses and map those rows into kotlin entities


//summary of how they interact
//->an android device sends a request with a JWT
//spring security validates the token
//spring securitycurrentuserprovider intercepts the request,
//reads the tokens subjectID and uses  UserProfileRepository to query PostgreSQL
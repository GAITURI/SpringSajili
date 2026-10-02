package sajili

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithMockJwtSecurityContextFactory : WithSecurityContextFactory<WithMockJwt> {
    override fun createSecurityContext(annotation: WithMockJwt): SecurityContext {
        val context = SecurityContextHolder.createEmptyContext()

        // Build a mock Spring Security OAuth2 Jwt object
        val jwt = Jwt.withTokenValue("mock-token-value")
            .header("alg", "none")
            .claim("sub", annotation.subject)
            .build()

        // Create an Authentication token wrapping the Jwt (similar to what Spring Security OAuth2 resource server does)
        val auth = object : AbstractAuthenticationToken(emptyList()) {
            override fun getPrincipal() = jwt
            override fun getCredentials() = "mock-token-value"
            init { isAuthenticated = true }
        }

        context.authentication = auth
        return context
    }
}
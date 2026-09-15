package sajili.agent

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CrossOrigin {

    @Bean
    fun corsFilter(): org.springframework.web.filter.CorsFilter {
        val source= UrlBasedCorsConfigurationSource()
        val config= CorsConfiguration()
        config.allowCredentials =true


        config.allowedOrigins= listOf(
            "http://localhost:8080",
            "http://localhost:3000"
        )
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
        config.allowedHeaders= listOf("Authorization","Content-Type", "X-Requested-With","Accept")

        config.maxAge= 3600L
        source.registerCorsConfiguration("/**",config)
        return CorsFilter(source)

    }
}
package sajili.agent

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter


@Configuration
@EnableWebSecurity
class SecurityConfig (){
    @Bean
    fun firebaseAuth(): FirebaseAuth{
        val apps= FirebaseApp.getApps()
        val firebaseApp= if (apps.isEmpty()){
            val serviceAccount= ClassPathResource("firebase-login-agent.json").inputStream
            val options= FirebaseOptions.builder()
                .setCredentials(GoogleCredentials
                    .fromStream(serviceAccount))
                    .build()
                    FirebaseApp.initializeApp(options)

        }else{
            apps[0]
        }
        return FirebaseAuth.getInstance(firebaseApp)
    }
    @Bean
    fun securityFilterChain(http:HttpSecurity):SecurityFilterChain{
      http
          .csrf{it.disable()}
          .cors{it.disable()}
          .sessionManagement{it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)}
          .authorizeHttpRequests{ auth->
              auth.requestMatchers("/api/auth/**").permitAll()
              auth.requestMatchers("/error").permitAll()
              auth.anyRequest().authenticated()


          }
          .httpBasic {it.disable()}
          .formLogin { it.disable() }
          .addFilterBefore(firebaseTokenFilter(firebaseAuth()),UsernamePasswordAuthenticationFilter::class.java)
    return http.build()
    }
//    new custom filter to verify Firebase ID tokens

    fun firebaseTokenFilter(firebaseAuth: FirebaseAuth):OncePerRequestFilter{
        return  object : OncePerRequestFilter(){
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain
            ) {
                val path= request.requestURI

                if (path.startsWith("/api/auth")){
                    filterChain.doFilter(request,response)
                return
                }
            val authHeader:String?= request.getHeader("Authorization")
            //checks for a valid Authorization header
                if (authHeader == null || !authHeader.startsWith("Bearer")){
                 filterChain.doFilter(request, response)
                 return
                }
//                extract the Firebase id token
                val idToken = authHeader.substring(7)
          try {
//              verify the token using Firebase's SDK
              val decodedToken = firebaseAuth.verifyIdToken(idToken)
               val uid = decodedToken.uid

//              set the user in the security context using their Firebase UID
              val authentication = UsernamePasswordAuthenticationToken(
                  uid,
                  null,
                  emptyList()

              )
              SecurityContextHolder.getContext().authentication= authentication
          }catch (e:FirebaseAuthException){
//              handle invalid or expired tokens
              response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid or expired Firebase Id token")
            return

         }
            filterChain.doFilter(request,response)
        }

        }
    }
}
package sajili.agent


import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sajili.agent.DTOProfile.AuthResponse
import sajili.agent.DTOProfile.TokenVerificationResponse

//rest controller for authentication related endpoints
@RestController
@RequestMapping("/api/auth") //to match our Android AuthServices
class AuthController(private val authService: AuthService) {


    //first handle user registration requests

//    new endpoint to verify the Firebase ID token

    @PostMapping("/verify_token")
    fun verifyToken(@RequestBody request:sajili.agent.DTOProfile.TokenRequest): ResponseEntity<Any>{
        return try{
            val uid = authService.verifyFirebaseIdToken(request.idToken)
            ResponseEntity.ok(TokenVerificationResponse(message="Token verified successfully!", uid= uid,jwtToken="dummy_token"))
        }catch (e:Exception){
            println("Token verification failed: ${e.message}")
//            return an appropriate HTTP status code based on the type of error
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse("", e.message ?:"Authentication Failed"))
        }
    }

    }



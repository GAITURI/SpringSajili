package sajili.agent.DTOProfile

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class TokenRequest(
    @get:NotBlank(message = "ID token cannot be blank")
    @JsonProperty("idToken")
    val idToken:String
)


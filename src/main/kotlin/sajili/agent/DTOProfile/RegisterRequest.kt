package sajili.agent.DTOProfile

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
//data transfer object for user registration requests
//used to receive data from Sajili app's registration screen
data class RegisterRequest(
    @field:NotBlank(message = "Phone number cannot be blank")
    //basic kenyan phone number pattern
    @field:Pattern(regexp = "^(0|\\+254)[17]\\d{8}$", message ="Invalid Phone Number" )
    val phoneNumber: String,

    @field:NotBlank(message = "Pin cannot be blank")
    @field:Size(min = 4, max= 6, message = "Pin must be between 4 and 6 digits")
    @field:Pattern(regexp = "^\\d+$", message = "Pin must contain only digits")
    val pinConfirm:String,

    @field:NotBlank(message = "Pin cannot be blank")
    @field:Size(min = 4, max= 6, message = "Pin must be between 4 and 6 digits")
    @field:Pattern(regexp = "^\\d+$", message = "Pin must contain only digits")
    val pin:String,



    )
//this is an inbound DTO for handling user registration
//it includes robust validation using annotations
//it ensures the input data meets specific requirements before its saved to the database
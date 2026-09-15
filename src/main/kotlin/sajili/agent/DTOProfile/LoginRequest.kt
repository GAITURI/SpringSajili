package sajili.agent.DTOProfile

import jakarta.validation.constraints.NotBlank

//data transfer object for user login requests
data class LoginRequest (

    @field:NotBlank(message = "Phone number cannot be blank")
    val phoneNumber:String,

    @field:NotBlank(message="Pin Cannot be blank")
    val pin:String
)
//this is an inbound DTO
//Its used to receive data from the client when a user attempts to log in
//the @NotBlank annotations are validation constraints that ensure the phoneNumber & and pin fields are not empty before the data is processed

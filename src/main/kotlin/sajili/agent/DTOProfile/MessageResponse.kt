package sajili.agent.DTOProfile
//generic response for messages(success or error messages)
data class MessageResponse(
    val message:String
)
//a generic outbond DTO for sending simple messaged back to the client
//example of outbond messages are success or error messages
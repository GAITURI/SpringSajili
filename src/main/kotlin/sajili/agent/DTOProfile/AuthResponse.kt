package sajili.agent.DTOProfile
//data transfer object for successful authentication responses
//sentback to the android app after successful login
data class AuthResponse (
    val jwt: String, //the JSON web token for subsequent authenticated requests
    val phoneNumber: String
)
//this is an outbond data access object
//its used to send data from the server to the client after a successful login
//it contains the JSON Web Token(JWT) which the android app will use for subsequent authenticated requests
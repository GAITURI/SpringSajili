package sajili.agent.DTOProfile

import com.google.gson.annotations.SerializedName

data class TokenVerificationResponse (
    @SerializedName("message")
    val message:String,
    @SerializedName("uid")
    val uid:String,
 @SerializedName("token")
    val jwtToken:String?

)

package sajili.agent.DTOProfile

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sajili.agent.UserRepository


@RestController
@RequestMapping("api/profile")
class UserProfileController(private val userRepository: UserRepository) {

    @GetMapping("/profile")
    fun getUserProfile(@AuthenticationPrincipal userDetails:UserDetails):ProfileResponse{
        val phoneNumber = userDetails.username

        val user = userRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow{IllegalStateException("User not found after authentication")}

        return ProfileResponse(
            phoneNumber= user.phoneNumber
        )
    }
}

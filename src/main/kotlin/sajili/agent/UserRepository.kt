package sajili.agent

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional


//spring data JPA repository for user entities
//provides standard CRUD operations and custom query methods.
interface UserRepository: JpaRepository<Profile,Long> {
    //finds a user by their phone number
    //used for login and checking if a phone number is already registered

    fun findByPhoneNumber(phoneNumber:String): Optional<Profile>


}
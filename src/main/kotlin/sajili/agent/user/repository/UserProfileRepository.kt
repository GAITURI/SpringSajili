package sajili.agent.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sajili.agent.user.entity.UserProfileEntity
import java.util.UUID

@Repository
interface UserProfileRepository : JpaRepository<UserProfileEntity, UUID>{
    fun findByAuthSubject(authSubject: String): UserProfileEntity
}
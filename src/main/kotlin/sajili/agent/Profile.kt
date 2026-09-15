package sajili.agent


import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Registered")
data class Profile (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val phoneNumber: String,

    @Column(unique = true, nullable = false)
    val firebaseUid: String, // Store the Firebase User ID (UID)

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
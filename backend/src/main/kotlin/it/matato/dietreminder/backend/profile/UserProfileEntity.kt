package it.matato.dietreminder.backend.profile

import jakarta.persistence.*

@Entity
@Table(name = "user_profiles")
data class UserProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true)
    val userId: String,
    val username: String,
    val dailyCalorieGoal: Int = 2000,
    val updatedAt: Long = System.currentTimeMillis()
)

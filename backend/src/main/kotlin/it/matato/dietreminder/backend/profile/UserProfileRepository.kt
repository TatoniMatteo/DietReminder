package it.matato.dietreminder.backend.profile

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserProfileRepository : JpaRepository<UserProfileEntity, Long> {
    fun findByUserId(userId: String): UserProfileEntity?
}

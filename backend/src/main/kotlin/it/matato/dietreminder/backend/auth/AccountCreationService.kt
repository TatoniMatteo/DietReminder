package it.matato.dietreminder.backend.auth

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** All registration methods use the role assigned atomically by PostgreSQL. */
@Service
class AccountCreationService(private val accounts: AccountRepository, private val entities: EntityManager) {
    @Transactional
    fun create(account: AccountEntity): AccountEntity {
        val saved = accounts.saveAndFlush(account)
        // The first insert is promoted by the database trigger. Return the actual
        // role in the initial session, without waiting for the next sign-in.
        entities.refresh(saved)
        return saved
    }
}

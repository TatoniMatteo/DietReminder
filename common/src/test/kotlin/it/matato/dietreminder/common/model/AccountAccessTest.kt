package it.matato.dietreminder.common.model

import org.junit.Assert.*
import org.junit.Test

class AccountAccessTest {
    private val account = AccountDto("id", "test@example.com", "Test")
    @Test fun verificationAndApprovalAreRequiredEvenForAnAdministrator() {
        for (status in listOf(EnrollmentStatus.PENDING_EMAIL, EnrollmentStatus.PENDING_APPROVAL, EnrollmentStatus.REJECTED)) {
            assertFalse(account.copy(role = "ADMIN", enrollmentStatus = status).permits(AccountPermission.DIETS_WRITE))
        }
        assertFalse(account.copy(role = "ADMIN", emailVerified = false).ready)
    }
    @Test fun assignedPermissionsLimitUsersAndActiveAdministratorsRetainTheirPowers() {
        val readonly = account.copy(permissions = setOf(AccountPermission.WORKSPACE_READ))
        assertTrue(readonly.permits(AccountPermission.WORKSPACE_READ))
        assertFalse(readonly.permits(AccountPermission.SHOPPING_WRITE))
        assertTrue(readonly.copy(role = "ADMIN").permits(AccountPermission.SHOPPING_WRITE))
    }
}

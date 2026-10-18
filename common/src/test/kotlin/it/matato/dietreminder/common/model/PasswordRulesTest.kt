package it.matato.dietreminder.common.model

import org.junit.Assert.*
import org.junit.Test
class PasswordRulesTest {
    @Test fun acceptsNoDigitsAccentedLettersAndLengthBoundaries() {
        for (value in listOf("Una password lunga!","Àccento sicuro!","Aa!"+"a".repeat(9),"Aa!"+"a".repeat(125))) assertTrue(value,PasswordRules.isValid(value))
    }
    @Test fun rejectsMissingCriteriaWhitespaceAsSpecialAndLengthOutsideBounds() {
        for (value in listOf("","breve!A","una password lunga!","UNA PASSWORD LUNGA!","Una password lunga","Aa "+"a".repeat(15),"Aa!"+"a".repeat(126))) assertFalse(value,PasswordRules.isValid(value))
    }
}

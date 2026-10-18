package it.matato.dietreminder.common.model

/** Rules for new passwords. Existing credentials remain valid for sign-in. */
object PasswordRules {
    fun hasUppercase(value: String) = value.any { it.isUpperCase() }
    fun hasLowercase(value: String) = value.any { it.isLowerCase() }
    fun hasSpecial(value: String) = value.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    fun isValid(value: String) = value.length in 12..128 && hasUppercase(value) && hasLowercase(value) && hasSpecial(value)
}

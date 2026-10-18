package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.error.ApiException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.transaction.event.TransactionalEventListener

data class PasswordResetRequested(val email: String, val token: String)

@Component
@EnableAsync
class PasswordResetMail(
    private val sender: ObjectProvider<JavaMailSender>,
    @Value("\${auth.mail-from:no-reply@dietreminder.local}") private val from: String
) {
    fun ensureAvailable() {
        if (sender.ifAvailable == null) throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Password recovery is not configured")
    }
    @Async
    @TransactionalEventListener
    fun send(event: PasswordResetRequested) {
        val email = event.email
        val token = event.token
        val mail = sender.ifAvailable ?: error("Mail sender unavailable")
        try {
            NotificationTemplates.send(mail, from, email, "Diet Reminder - Password reset",
                "Inserisci questo codice in Diet Reminder per scegliere una nuova password. Scade tra 30 minuti.\nEnter this code in Diet Reminder to choose a new password. It expires in 30 minutes.\n\n$token\n\nSe non hai richiesto questa operazione, ignora il messaggio. / If you did not request this, ignore this email.",
                "PASSWORD_RESET")
        } catch (_: org.springframework.mail.MailException) {
            org.slf4j.LoggerFactory.getLogger(javaClass).warn("Password reset delivery failed")
        }
    }
}

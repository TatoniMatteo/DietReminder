package it.matato.dietreminder.backend.auth

import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties

class NotificationTemplatesTest {
    @Test
    fun `all notification variants escape user content and include next steps without remote assets`() {
        val body = "Richiesta rifiutata / Application rejected: <img src=x onerror=alert(1)> & 'test'"
        for (kind in listOf("VERIFY_EMAIL", "PASSWORD_RESET", "STATUS", "REVIEW")) {
            val html = NotificationTemplates.render(kind, body)
            assertFalse(html.contains("<img src=x"))
            assertTrue(html.contains("&lt;img src=x"))
            assertTrue(html.contains("&amp;"))
            assertTrue(html.contains("cid:dietreminder-logo"))
            assertFalse(html.contains("https://"))
            assertFalse(html.contains("<script"))
        }
        assertTrue(NotificationTemplates.render("REVIEW", body).contains("contatta l’amministratore"))
        assertTrue(NotificationTemplates.render("REVIEW", "Richiesta approvata").contains("Il tuo spazio è pronto"))
        assertTrue(NotificationTemplates.render("STATUS", "Sei l’amministratore iniziale").contains("Il tuo spazio inizia da te"))
        assertTrue(NotificationTemplates.render("STATUS", "Email verificata").contains("administrator approval"))
    }

    @Test
    fun `verification and recovery emails include copyable code expiry plain text and embedded logo`() {
        for (kind in listOf("VERIFY_EMAIL", "PASSWORD_RESET")) {
            val sender = mock(JavaMailSender::class.java)
            val message = MimeMessage(Session.getInstance(Properties()))
            `when`(sender.createMimeMessage()).thenReturn(message)
            val code = "A".repeat(43)
            val text = "Usa questo codice.\n\n$code"
            NotificationTemplates.send(sender, "no-reply@example.com", "user@example.com", "Diet Reminder", text, kind)
            message.saveChanges()
            verify(sender).send(message)
            val parts = flatten(message)
            assertTrue(parts.any { it.isMimeType("text/plain") && it.content == text })
            val html = parts.single { it.isMimeType("text/html") }.content as String
            assertTrue(html.contains(code))
            assertTrue(html.contains(if (kind == "PASSWORD_RESET") "30 minuti" else "24 ore"))
            val logo = parts.single { it.isMimeType("image/png") }
            assertEquals("<dietreminder-logo>", logo.getHeader("Content-ID").single())
            assertTrue(logo.inputStream.readBytes().size > 100)
        }
    }

    private fun flatten(part: Part): List<Part> {
        val content = part.content
        return if (content is Multipart) (0 until content.count).flatMap { flatten(content.getBodyPart(it)) } else listOf(part)
    }
}

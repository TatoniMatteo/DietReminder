package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.error.ApiException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/** Transactional outbox: enrollment changes and their notifications commit together. */
@Component
@EnableScheduling
class AccountNotificationMail(
    private val sender: ObjectProvider<JavaMailSender>,
    private val jdbc: JdbcTemplate,
    private val transactions: TransactionTemplate,
    @Value("\${auth.mail-from:no-reply@dietreminder.local}") private val from: String
) {
    fun ensureAvailable() {
        if (sender.ifAvailable == null) throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Email delivery must be configured before registration")
    }
    fun enqueue(account: AccountEntity, kind: String, subject: String, body: String) {
        jdbc.update("INSERT INTO account_mail_outbox(account_id, kind, recipient, subject, body, next_attempt_at) VALUES (?, ?, ?, ?, ?, ?)",
            account.id, kind, account.email, subject, body, System.currentTimeMillis())
    }
    @Scheduled(fixedDelayString = "\${auth.notifications.poll-ms:1000}")
    fun deliver() {
        val mail = sender.ifAvailable ?: return
        jdbc.update("DELETE FROM account_mail_outbox WHERE kind = 'VERIFY_EMAIL' AND NOT EXISTS (SELECT 1 FROM account_tokens t WHERE t.account_id = account_mail_outbox.account_id AND t.purpose = 'VERIFY_EMAIL' AND t.expires_at > ?)", System.currentTimeMillis())
        jdbc.update("DELETE FROM account_mail_outbox WHERE sent_at < ?", System.currentTimeMillis() - 30L * 24 * 60 * 60_000)
        repeat(10) {
            val processed = transactions.execute {
                val rows = jdbc.queryForList("SELECT id, kind, recipient, subject, body, attempts FROM account_mail_outbox WHERE sent_at IS NULL AND next_attempt_at <= ? ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED", System.currentTimeMillis())
                val row = rows.firstOrNull() ?: return@execute false
                val id = (row["id"] as Number).toLong()
                try {
                    NotificationTemplates.send(mail, from, row["recipient"] as String,
                        row["subject"] as String, row["body"] as String, row["kind"] as String)
                    jdbc.update("UPDATE account_mail_outbox SET sent_at = ? WHERE id = ?", System.currentTimeMillis(), id)
                } catch (_: org.springframework.mail.MailException) {
                    val attempts = (row["attempts"] as Number).toInt() + 1
                    val delay = (60_000L * (1L shl attempts.coerceAtMost(10))).coerceAtMost(3_600_000L)
                    jdbc.update("UPDATE account_mail_outbox SET attempts = ?, next_attempt_at = ? WHERE id = ?", attempts, System.currentTimeMillis() + delay, id)
                    org.slf4j.LoggerFactory.getLogger(javaClass).warn("Account notification delivery failed; retry scheduled")
                }
                true
            } ?: false
            if (!processed) return
        }
    }
}

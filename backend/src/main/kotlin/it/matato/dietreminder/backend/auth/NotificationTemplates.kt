package it.matato.dietreminder.backend.auth

import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper

/** Inline styles and table layout work in mail clients without scripts or remote assets. */
object NotificationTemplates {
    fun send(sender: JavaMailSender, from: String, to: String, subject: String, body: String, kind: String) {
        val message = sender.createMimeMessage()
        MimeMessageHelper(message, true, "UTF-8").apply {
            setFrom(from, "Diet Reminder")
            setTo(to)
            setSubject(subject)
            setText(body, render(kind, body))
            addInline("dietreminder-logo", ClassPathResource("mail/logo.png"), "image/png")
        }
        sender.send(message)
    }

    fun render(kind: String, body: String): String {
        val rejected = kind == "REVIEW" && body.startsWith("Richiesta rifiutata")
        val code = if (kind in setOf("VERIFY_EMAIL", "PASSWORD_RESET"))
            Regex("(?m)^[A-Za-z0-9_-]{43}$").find(body)?.value else null
        val (badge, title, subtitle, action) = when {
            kind == "VERIFY_EMAIL" -> listOf("VERIFICA EMAIL · EMAIL VERIFICATION", "Un passo verso il tuo spazio.", "Verify your email to get started.", "Inserisci il codice nella schermata di verifica dell’app. / Enter the code on the app’s email verification screen.")
            kind == "PASSWORD_RESET" -> listOf("SICUREZZA · SECURITY", "Riprendi il controllo del tuo account.", "Reset your password securely.", "Inserisci il codice nella schermata Recupera password. / Enter the code on the password recovery screen.")
            rejected -> listOf("ESITO RICHIESTA · APPLICATION UPDATE", "Un aggiornamento sulla tua richiesta.", "Your application has been reviewed.", "Per chiarimenti contatta l’amministratore. / Contact your administrator if you need help.")
            kind == "REVIEW" -> listOf("ACCESSO APPROVATO · ACCESS APPROVED", "Il tuo spazio è pronto.", "Your account is ready to use.", "Torna nell’app e premi Controlla stato per iniziare. / Return to the app and select Check status to get started.")
            body.contains("amministratore iniziale") -> listOf("BENVENUTO · WELCOME", "Il tuo spazio inizia da te.", "You are this installation’s administrator.", "Apri l’app per conoscere i tuoi poteri e le tue responsabilità. / Open the app to learn about your powers and responsibilities.")
            else -> listOf("RICHIESTA RICEVUTA · APPLICATION RECEIVED", "La tua richiesta è in buone mani.", "Your account is awaiting administrator approval.", "Ti avviseremo via email dopo la valutazione. Puoi controllare lo stato nell’app. / We’ll email you after the review. You can also check your status in the app.")
        }
        val paragraphs = body.lineSequence().filter { it.isNotBlank() && it != code }.joinToString("") {
            "<p style=\"margin:0 0 16px;font-size:15px;line-height:1.7;color:#4c5364;overflow-wrap:anywhere\">${escape(it)}</p>"
        }
        val codeBlock = code?.let {
            """<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:24px 0;background:#f0f6f1;border:1px solid #d6e8dc;border-radius:12px"><tr><td style="padding:20px"><p style="margin:0 0 12px;font-size:11px;letter-spacing:1.5px;color:#246a42">CODICE PERSONALE · YOUR CODE</p><p style="margin:0;font-family:Consolas,monospace;font-size:17px;line-height:1.8;letter-spacing:1px;color:#163e29;word-break:break-all;overflow-wrap:anywhere">${escape(it)}</p><p style="margin:12px 0 0;font-size:12px;color:#58685e">${if (kind == "PASSWORD_RESET") "Valido per 30 minuti / Valid for 30 minutes" else "Valido per 24 ore / Valid for 24 hours"}</p></td></tr></table>"""
        }.orEmpty()
        return """<!doctype html>
<html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="color-scheme" content="light"><title>${escape(title)}</title></head>
<body style="margin:0;padding:0;background:#f3f5f4;font-family:Arial,Helvetica,sans-serif;color:#282b3d">
<div style="display:none;font-size:1px;line-height:1px;max-height:0;overflow:hidden;mso-hide:all">${escape(title)} ${escape(subtitle)}</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f3f5f4"><tr><td align="center" style="padding:32px 12px">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;background:#ffffff;border:1px solid #e1e8e3;border-radius:20px;overflow:hidden">
<tr><td style="padding:28px 28px 22px;border-bottom:1px solid #e6ece8"><table role="presentation" cellpadding="0" cellspacing="0"><tr><td><img src="cid:dietreminder-logo" width="44" height="44" alt="" style="display:block;border:0"></td><td style="padding-left:12px;font-size:19px;font-weight:bold;letter-spacing:-0.5px;color:#163e29">Diet Reminder</td></tr></table></td></tr>
<tr><td style="padding:32px 28px 16px"><p style="margin:0 0 18px;color:${if (rejected) "#9a4249" else "#246a42"};font-size:10px;font-weight:bold;letter-spacing:1.4px">${escape(badge)}</p><h1 style="margin:0 0 12px;font-size:30px;line-height:1.2;letter-spacing:-0.7px;color:#193f2b">${escape(title)}</h1><p lang="en" style="margin:0 0 28px;font-size:14px;line-height:1.6;color:#717b75">${escape(subtitle)}</p>$paragraphs$codeBlock</td></tr>
<tr><td style="padding:0 28px 32px"><table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#193f2b;border-radius:12px"><tr><td style="padding:20px;color:#ffffff;font-size:14px;line-height:1.7">${escape(action)}</td></tr></table></td></tr>
<tr><td style="padding:20px 28px;background:#f9faf9;border-top:1px solid #e6ece8;font-size:11px;line-height:1.8;color:#737d76">Notifica automatica relativa al tuo account. Non rispondere a questo messaggio.<br><span lang="en">An automatic notification about your account. Please do not reply.</span><br>Diet Reminder non ti chiederà mai la password via email. / We will never ask for your password by email.</td></tr>
</table></td></tr></table></body></html>"""
    }

    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
}

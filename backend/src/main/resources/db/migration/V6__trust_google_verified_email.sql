-- Google identities were linked only after verifying Google's email_verified claim.
-- Upgrade accounts left awaiting a redundant code by the previous enrollment flow.
WITH upgraded AS (
    UPDATE accounts
    SET email_verified = true,
        enrollment_status = CASE WHEN role = 'ADMIN' THEN 'APPROVED' ELSE 'PENDING_APPROVAL' END,
        enrollment_version = enrollment_version + 1
    WHERE google_subject IS NOT NULL AND NOT email_verified AND enrollment_status = 'PENDING_EMAIL'
    RETURNING id, email, enrollment_status, role
), events AS (
    INSERT INTO enrollment_events(account_id, status, message, created_at)
    SELECT id, enrollment_status,
        CASE WHEN role = 'ADMIN'
            THEN 'Email verificata da Google. Sei l’amministratore iniziale. / Email verified by Google. You are the initial administrator.'
            ELSE 'Email verificata da Google. La richiesta è in attesa dell’amministratore. / Email verified by Google. Your application is awaiting administrator approval.' END,
        (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT
    FROM upgraded
    RETURNING account_id, message
)
INSERT INTO account_mail_outbox(account_id, kind, recipient, subject, body, next_attempt_at)
SELECT u.id, 'STATUS', u.email, 'Diet Reminder - Stato registrazione / Registration status', e.message,
    (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000)::BIGINT
FROM upgraded u JOIN events e ON e.account_id = u.id;

DELETE FROM account_tokens WHERE purpose = 'VERIFY_EMAIL'
    AND account_id IN (SELECT id FROM accounts WHERE google_subject IS NOT NULL AND email_verified);
DELETE FROM account_mail_outbox WHERE kind = 'VERIFY_EMAIL'
    AND account_id IN (SELECT id FROM accounts WHERE google_subject IS NOT NULL AND email_verified);

-- Repair installations created before automatic administrator assignment.
-- Keep existing active administrators; otherwise promote the oldest account.
UPDATE accounts
SET role = 'ADMIN', enabled = TRUE
WHERE id = (SELECT id FROM accounts ORDER BY created_at, id LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE role = 'ADMIN' AND enabled);

-- A permanent row serializes account creation even when accounts is empty.
-- Its counter also prevents concurrent removals of different administrators
-- from both succeeding and leaving no active administrator.
CREATE TABLE account_administration_guard (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    initialized BOOLEAN NOT NULL,
    active_admin_count BIGINT NOT NULL,
    CONSTRAINT active_admin_count_nonnegative CHECK (active_admin_count >= 0),
    CONSTRAINT last_active_administrator CHECK (NOT initialized OR active_admin_count > 0)
);
INSERT INTO account_administration_guard (id, initialized, active_admin_count)
SELECT 1, EXISTS (SELECT 1 FROM accounts), COUNT(*) FILTER (WHERE role = 'ADMIN' AND enabled)
FROM accounts;

CREATE FUNCTION enforce_account_administration() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
DECLARE
    installation_initialized BOOLEAN;
    previous_active INTEGER := 0;
    next_active INTEGER := 0;
BEGIN
    SELECT initialized INTO STRICT installation_initialized
    FROM account_administration_guard WHERE id = 1 FOR UPDATE;

    IF TG_OP = 'INSERT' AND NOT installation_initialized THEN
        NEW.role := 'ADMIN';
        NEW.enabled := TRUE;
    END IF;
    IF TG_OP <> 'INSERT' THEN
        IF OLD.role = 'ADMIN' AND OLD.enabled THEN previous_active := 1; END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        IF NEW.role = 'ADMIN' AND NEW.enabled THEN next_active := 1; END IF;
    END IF;

    UPDATE account_administration_guard
    SET initialized = initialized OR TG_OP = 'INSERT',
        active_admin_count = active_admin_count + next_active - previous_active
    WHERE id = 1;

    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER accounts_administration_insert
BEFORE INSERT ON accounts FOR EACH ROW EXECUTE FUNCTION enforce_account_administration();
CREATE TRIGGER accounts_administration_update
BEFORE UPDATE OF role, enabled ON accounts FOR EACH ROW EXECUTE FUNCTION enforce_account_administration();
CREATE TRIGGER accounts_administration_delete
BEFORE DELETE ON accounts FOR EACH ROW EXECUTE FUNCTION enforce_account_administration();

CREATE FUNCTION prevent_accounts_truncate() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    PERFORM 1 FROM account_administration_guard WHERE id = 1 AND initialized FOR UPDATE;
    IF FOUND THEN
        RAISE EXCEPTION 'At least one active administrator is required'
            USING ERRCODE = '23514', CONSTRAINT = 'last_active_administrator';
    END IF;
    RETURN NULL;
END;
$$;
CREATE TRIGGER accounts_administration_truncate
BEFORE TRUNCATE ON accounts FOR EACH STATEMENT EXECUTE FUNCTION prevent_accounts_truncate();

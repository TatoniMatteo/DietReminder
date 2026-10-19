"""Remove exact local browser-test identities, preserving administrator accounts."""
import os
if os.environ.get("WEB_TEST_URL", "http://127.0.0.1:8088") not in ("http://127.0.0.1:8088", "http://localhost:8088"):
    raise SystemExit("Cleanup is restricted to the local Docker test distribution")
from pathlib import Path
import json,re,subprocess
os.chdir(Path(__file__).resolve().parents[1])
for file in Path('web/test-results').glob('*/created-user.json'):
    account=json.loads(file.read_text())
    uid=account['id']; email=account['email']
    if not re.fullmatch(r'[0-9a-f-]{36}',uid): raise ValueError('Invalid test account ID')
    if not re.fullmatch(r'browser-e2e-[0-9a-f-]{36}@dietreminder\.local', email): raise ValueError('Invalid test account email')
    # On an empty installation the first browser test becomes its administrator.
    # Keep administrators rather than weakening the database invariant for tests.
    sql=(f"DELETE FROM accounts WHERE id = '{uid}' AND email = '{email}' AND role <> 'ADMIN';\n"
         f"SELECT 'Preserved test administrator: {uid}' WHERE EXISTS "
         f"(SELECT 1 FROM accounts WHERE id = '{uid}' AND email = '{email}' AND role = 'ADMIN');\n")
    subprocess.run(['docker','compose','exec','-T','postgres','sh','-c','psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'],input=sql,text=True,check=True)

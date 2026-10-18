import { t } from './i18n'
let csrfToken = ''
export function setCsrf(token: string) {
  csrfToken = token
}
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message)
  }
}
const messages: Record<string, string> = {
  get ACCOUNT_NOT_READY() {
    return t('Completa la verifica email e attendi l’approvazione.')
  },
  get PERMISSION_DENIED() {
    return t('I tuoi permessi non consentono questa operazione.')
  },
  get INVALID_VERIFICATION_TOKEN() {
    return t('Il codice non è valido o è scaduto.')
  },
  get EMAIL_NOT_VERIFIED() {
    return t('L’utente deve prima verificare l’email.')
  },
  get REVIEW_CONFLICT() {
    return t('La richiesta è cambiata. Ricarica prima di decidere.')
  },
  get INVALID_REQUEST() {
    return t('Controlla i dati inseriti.')
  },
  get INVALID_CURRENT_PASSWORD() {
    return t('La password attuale non è corretta.')
  },
  get PASSWORD_UNCHANGED() {
    return t('La nuova password deve essere diversa da quella attuale.')
  },
  get INVALID_CREDENTIALS() {
    return t('Email o password non corrette.')
  },
  get ACCOUNT_EXISTS() {
    return t('Esiste già un account con questa email.')
  },
  get GOOGLE_LINK_REQUIRED() {
    return t('Accedi con la password e collega Google dalle impostazioni.')
  },
  get GOOGLE_ACCOUNT_MISMATCH() {
    return t('Scegli Google con la stessa email del tuo account.')
  },
  get GOOGLE_NOT_CONFIGURED() {
    return t('Google deve ancora essere configurato sul server.')
  },
  get ACCOUNT_DISABLED() {
    return t('Il tuo account è sospeso. Contatta l’amministratore.')
  },
  get WORKSPACE_CLIENT_OUTDATED() {
    return t('Aggiorna l’applicazione per modificare questi dati.')
  },
  get WORKSPACE_CONFLICT() {
    return t('I dati sono cambiati su un altro dispositivo. Ricarica prima di riprovare.')
  },
  get RELEASE_POLICY_CONFLICT() {
    return t('Un altro amministratore ha modificato questi dati. Ricarica la pagina.')
  },
  get DRIVE_RECONNECT_REQUIRED() {
    return t('Ricollega Google Drive per continuare.')
  },
  get INVALID_CSRF() {
    return t('Ricarica la pagina e riprova.')
  },
  get RATE_LIMITED() {
    return t('Troppi tentativi. Attendi qualche minuto.')
  },
  get INVALID_RELEASE_POLICY() {
    return t('Controlla i limiti delle versioni e inserisci un indirizzo HTTPS valido.')
  },
  get ADMIN_PROTECTED() {
    return t('Gli account amministratore sono protetti.')
  },
  get BACKUP_LIMIT_REACHED() {
    return t('Hai raggiunto 50 backup. Elimina una copia precedente.')
  },
}
export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      method,
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        'X-DietReminder-Web': '1',
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new Error(t('Il server non è raggiungibile. Controlla la connessione e riprova.'))
  }
  if (!response.ok) {
    const error = (await response.json().catch(() => ({ code: 'SERVER_ERROR' }))) as {
      code: string
      message?: string
    }
    if (error.code === 'ACCOUNT_NOT_READY')
      window.dispatchEvent(new Event('account-status-changed'))
    if (response.status === 401 && !path.startsWith('/api/web/auth/'))
      window.dispatchEvent(new Event('session-expired'))
    throw new ApiError(
      response.status,
      error.code,
      messages[error.code] ??
        (response.status === 403
          ? t('Non hai i permessi per questa operazione.')
          : (error.message ?? t('Operazione non riuscita. Riprova.'))),
    )
  }
  const text = await response.text()
  return text ? (JSON.parse(text) as T) : (undefined as T)
}

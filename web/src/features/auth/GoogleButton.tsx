import { t } from '../../lib/i18n'
import { useEffect, useRef, useState } from 'react'
import { api } from '../../lib/api'
import type { Providers } from '../../lib/types'
interface GoogleIdApi {
  initialize: (config: {
    client_id: string
    nonce: string
    callback: (response: { credential: string }) => void
  }) => void
  renderButton: (element: HTMLElement, options: object) => void
  cancel: () => void
}
declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdApi } }
  }
}
let googleScript: Promise<void> | null = null
function loadGoogle() {
  if (!googleScript)
    googleScript = new Promise<void>((resolve, reject) => {
      if (window.google) return resolve()
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        googleScript = null
        script.remove()
        reject(new Error(t('Impossibile caricare Google. Controlla la connessione.')))
      }
      document.head.appendChild(script)
    })
  return googleScript
}
export function GoogleButton({
  providers,
  onCredential,
  onError,
}: {
  providers: Providers
  onCredential: (credential: string) => Promise<void>
  onError: (message: string) => void
}) {
  const element = useRef<HTMLDivElement>(null)
  const callback = useRef(onCredential)
  callback.current = onCredential
  const error = useRef(onError)
  error.current = onError
  const [generation, setGeneration] = useState(0)
  useEffect(() => {
    if (!providers.signInEnabled) return
    let active = true
    void Promise.all([loadGoogle(), api<{ nonce: string }>('/api/auth/google/challenge', 'POST')])
      .then(([, challenge]) => {
        if (!active || !element.current) return
        window.google!.accounts.id.initialize({
          client_id: providers.clientId,
          nonce: challenge.nonce,
          callback: (response) => {
            void callback.current(response.credential).finally(() => setGeneration((v) => v + 1))
          },
        })
        element.current.innerHTML = ''
        window.google!.accounts.id.renderButton(element.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          locale: 'it',
          width: 300,
        })
      })
      .catch((e) => {
        if (active) error.current((e as Error).message)
      })
    return () => {
      active = false
      window.google?.accounts.id.cancel()
    }
  }, [providers.clientId, providers.signInEnabled, generation])
  if (!providers.signInEnabled)
    return (
      <p className="muted small google-disabled">
        {t('Accesso Google disponibile dopo la configurazione del server.')}
      </p>
    )
  return <div className="google-button" ref={element} />
}

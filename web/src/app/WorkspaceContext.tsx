import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { api, ApiError } from '../lib/api'
import type { Snapshot, Workspace } from '../lib/types'

interface WorkspaceState {
  workspace: Workspace | null
  loading: boolean
  saving: boolean
  error: string
  conflict: boolean
  refresh: () => Promise<void>
  save: (snapshot: Snapshot, expectedVersion?: number) => Promise<boolean>
}
const Context = createContext<WorkspaceState | null>(null)
export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const [workspace, setWorkspace] = useState<Workspace | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [conflict, setConflict] = useState(false)
  const lock = useRef(false)
  const current = useRef<Workspace | null>(null)
  const refresh = useCallback(async () => {
    if (lock.current) return
    setLoading(true)
    try {
      const value = await api<Workspace>('/api/workspace')
      if (
        !lock.current &&
        !document.querySelector('dialog[open]') &&
        value.version >= (current.current?.version ?? 0)
      ) {
        current.current = value
        setWorkspace(value)
        setError('')
        setConflict(false)
      }
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }, [])
  useEffect(() => {
    void refresh()
  }, [refresh])
  useEffect(() => {
    const interval = setInterval(() => {
      if (
        document.visibilityState === 'visible' &&
        !lock.current &&
        !document.querySelector('dialog[open]') &&
        !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName ?? '')
      )
        void refreshQuietly()
    }, 30000)
    async function refreshQuietly() {
      try {
        const value = await api<Workspace>('/api/workspace')
        if (
          !lock.current &&
          !document.querySelector('dialog[open]') &&
          !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName ?? '') &&
          (!current.current || value.version > current.current.version)
        ) {
          current.current = value
          setWorkspace(value)
        }
      } catch {
        /* A visible error is shown on explicit refresh or save. */
      }
    }
    return () => clearInterval(interval)
  }, [])
  async function save(snapshot: Snapshot, expectedVersion?: number) {
    if (lock.current || !current.current || conflict) return false
    snapshot = { ...snapshot, schemaVersion: 2 }
    lock.current = true
    setSaving(true)
    setError('')
    try {
      const response = await api<{ version: number }>('/api/workspace', 'PUT', {
        baseVersion: expectedVersion ?? workspace!.version,
        requestId: crypto.randomUUID(),
        snapshot,
      })
      const next = { version: response.version, snapshot }
      current.current = next
      setWorkspace(next)
      return true
    } catch (e) {
      setError((e as Error).message)
      if (e instanceof ApiError && e.code === 'WORKSPACE_CONFLICT') setConflict(true)
      return false
    } finally {
      lock.current = false
      setSaving(false)
    }
  }
  return (
    <Context.Provider value={{ workspace, loading, saving, error, conflict, refresh, save }}>
      {children}
    </Context.Provider>
  )
}
export function useWorkspace() {
  const value = useContext(Context)
  if (!value) throw new Error('WorkspaceProvider missing')
  return value
}

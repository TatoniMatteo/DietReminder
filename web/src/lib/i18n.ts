import { useSyncExternalStore } from 'react'
import english from '../locales/en.json'
export type Language = 'it' | 'en'
const dictionary: Record<string, string> = english
const reverse = new Map(Object.entries(dictionary).map(([it, en]) => [en, it]))
let language: Language =
  typeof document !== 'undefined' && document.documentElement.lang === 'en' ? 'en' : 'it'
const listeners = new Set<() => void>()
export const getLanguage = () => language
export const locale = () => (language === 'it' ? 'it-IT' : 'en-GB')
export function setLanguage(value: Language) {
  language = value
  document.documentElement.lang = value
  try {
    localStorage.setItem('dr-language', value)
  } catch {
    /* The selected language remains usable in this tab. */
  }
  listeners.forEach((listener) => listener())
}
if (typeof window !== 'undefined')
  window.addEventListener('storage', (event) => {
    if (event.key === 'dr-language' && (event.newValue === 'it' || event.newValue === 'en')) {
      language = event.newValue
      document.documentElement.lang = language
      listeners.forEach((listener) => listener())
    }
  })
export function useLanguage() {
  return useSyncExternalStore(
    (callback) => {
      listeners.add(callback)
      return () => {
        listeners.delete(callback)
      }
    },
    getLanguage,
    () => 'it' as Language,
  )
}
export function t(key: string, values: Record<string, unknown> = {}): string {
  const source = dictionary[key] ? key : (reverse.get(key) ?? key)
  const template = language === 'en' ? (dictionary[source] ?? key) : source
  return template.replace(/\{(\d+|[a-zA-Z_]+)\}/g, (match, key: string) =>
    Object.prototype.hasOwnProperty.call(values, key) ? String(values[key] ?? '') : match,
  )
}
/** Translated lists keep their canonical values while reacting to language changes. */
export function localizedArray<T>(source: T[]): T[] {
  return new Proxy(source, {
    get(target, key, receiver) {
      const value: unknown = Reflect.get(target, key, receiver)
      return Array.isArray(value)
        ? localizedArray(value)
        : typeof value === 'string'
          ? t(value)
          : value
    },
  })
}

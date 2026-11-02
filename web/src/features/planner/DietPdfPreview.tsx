import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { exportDiet } from '../../lib/diet-export'
import { dayNames, mealNames, formatQuantity } from '../../lib/workspace'
import { t } from '../../lib/i18n'
import type { Snapshot } from '../../lib/types'

export function DietPdfPreview({
  snapshot,
  dietId,
  onClose,
}: {
  snapshot: Snapshot
  dietId: number
  onClose: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const diet = exportDiet(snapshot, dietId)
  useEffect(() => {
    const dialog = ref.current!
    dialog.showModal()
    const title = document.title
    document.title = diet.name
    document.body.classList.add('diet-pdf-open')
    return () => {
      dialog.close()
      document.title = title
      document.body.classList.remove('diet-pdf-open')
    }
  }, [diet.name])
  return createPortal(
    <dialog
      ref={ref}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      className="diet-pdf"
      aria-label={t('Esporta PDF')}
    >
      <div className="diet-pdf-controls">
        <button className="button secondary" onClick={onClose}>
          {t('Chiudi')}
        </button>
        <button autoFocus className="button primary" onClick={() => window.print()}>
          {t('Stampa / Salva PDF')}
        </button>
      </div>
      <article className="diet-pdf-document">
        <h1>{diet.name}</h1>
        {diet.meals.length === 0 && <p>{t('Nessun pasto pianificato.')}</p>}
        {Array.from({ length: 7 }, (_, i) => i).map((day) => {
          const meals = diet.meals.filter(
            (m) =>
              m.day ===
              ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'][day],
          )
          if (!meals.length) return null
          return (
            <section key={day} className="diet-pdf-day">
              <h2>{dayNames[day]}</h2>
              {meals.map((m, index) => (
                <section key={index} className="diet-pdf-meal">
                  <h3>
                    {m.time} · {m.customTypeLabel || mealNames[m.type]}
                  </h3>
                  {m.description && <p>{m.description}</p>}
                  {m.courses.map((c, i) => (
                    <section className="diet-pdf-course" key={i}>
                      <h4>
                        {c.alternativeGroup
                          ? `${t('Alternativa')} ${m.courses.filter((other) => other.alternativeGroup === c.alternativeGroup).indexOf(c) + 1} · `
                          : ''}
                        {c.name || t('Portata {0}', { '0': i + 1 })}
                      </h4>
                      {c.description && <p>{c.description}</p>}
                      {c.items.length > 0 && (
                        <ul>
                          {c.items.map((f, j) => (
                            <li key={j}>
                              {f.name}
                              {formatQuantity(f.amount, f.unit)
                                ? ` — ${formatQuantity(f.amount, f.unit)}`
                                : ''}
                            </li>
                          ))}
                        </ul>
                      )}
                    </section>
                  ))}
                </section>
              ))}
            </section>
          )
        })}
      </article>
    </dialog>,
    document.body,
  )
}

import {
  Children,
  isValidElement,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent,
  type ReactElement,
  type ReactNode,
} from 'react'
import { createPortal } from 'react-dom'
import { Check, ChevronDown } from 'lucide-react'

type Option = { value: string; label: string; disabled: boolean }
type Props = {
  children: ReactNode
  'aria-label': string
  value?: string | number
  defaultValue?: string | number
  name?: string
  className?: string
  disabled?: boolean
  onValueChange?: (value: string) => void
}

/** A themed single-choice combobox; the hidden select participates in native forms. */
export function Select({
  children,
  'aria-label': label,
  value,
  defaultValue,
  name,
  className = '',
  disabled = false,
  onValueChange,
}: Props) {
  const options: Option[] = Children.toArray(children)
    .filter(isValidElement)
    .map((node) => {
      const option = node as ReactElement<{
        value?: string | number
        children: ReactNode
        disabled?: boolean
      }>
      const text = Children.toArray(option.props.children).join('')
      return {
        value: String(option.props.value ?? text),
        label: text,
        disabled: !!option.props.disabled,
      }
    })
  const initial = String(defaultValue ?? options[0]?.value ?? '')
  const [local, setLocal] = useState(initial)
  const selected = String(value ?? local)
  const selectedIndex = Math.max(
    0,
    options.findIndex((option) => option.value === selected),
  )
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(selectedIndex)
  const [position, setPosition] = useState({ top: 0, left: 0, width: 0, maxHeight: 280 })
  const trigger = useRef<HTMLButtonElement>(null)
  const popup = useRef<HTMLDivElement>(null)
  const native = useRef<HTMLSelectElement>(null)
  const search = useRef({ text: '', time: 0 })
  const id = useId()

  useEffect(() => {
    const form = native.current?.form
    const reset = () => {
      setLocal(initial)
      setOpen(false)
    }
    form?.addEventListener('reset', reset)
    return () => form?.removeEventListener('reset', reset)
  }, [initial])
  useEffect(() => {
    if (disabled) setOpen(false)
  }, [disabled])
  useLayoutEffect(() => {
    if (!open) return
    const positionPopup = () => {
      const bounds = trigger.current!.getBoundingClientRect()
      if (bounds.bottom < 0 || bounds.top > window.innerHeight) {
        setOpen(false)
        return
      }
      const below = window.innerHeight - bounds.bottom - 12
      const above = bounds.top - 12
      const maxHeight = Math.max(60, Math.min(280, Math.max(below, above)))
      const height = Math.min(maxHeight, options.length * 40 + 12)
      const width = Math.min(Math.max(bounds.width, 180), window.innerWidth - 24)
      setPosition({
        top:
          below >= height || below >= above
            ? bounds.bottom + 6
            : Math.max(12, bounds.top - height - 6),
        left: Math.max(12, Math.min(bounds.left, window.innerWidth - width - 12)),
        width,
        maxHeight,
      })
    }
    const outside = (event: PointerEvent) => {
      const target = event.target as Node
      if (!trigger.current?.contains(target) && !popup.current?.contains(target)) setOpen(false)
    }
    positionPopup()
    document.addEventListener('pointerdown', outside)
    window.addEventListener('resize', positionPopup)
    window.addEventListener('scroll', positionPopup, true)
    return () => {
      document.removeEventListener('pointerdown', outside)
      window.removeEventListener('resize', positionPopup)
      window.removeEventListener('scroll', positionPopup, true)
    }
  }, [open, options.length])
  useEffect(() => {
    const menu = popup.current
    const option = menu?.querySelector<HTMLElement>(`[data-index="${active}"]`)
    if (!open || !menu || !option) return
    if (option.offsetTop < menu.scrollTop) menu.scrollTop = option.offsetTop
    else if (option.offsetTop + option.offsetHeight > menu.scrollTop + menu.clientHeight)
      menu.scrollTop = option.offsetTop + option.offsetHeight - menu.clientHeight
  }, [active, open])

  function choose(index: number) {
    const option = options[index]
    if (!option || option.disabled) return
    setLocal(option.value)
    onValueChange?.(option.value)
    setOpen(false)
    trigger.current?.focus()
  }
  function show() {
    setActive(selectedIndex)
    setOpen(true)
  }
  function move(from: number, delta: number) {
    let index = from
    for (let count = 0; count < options.length; count++) {
      index = (index + delta + options.length) % options.length
      if (!options[index].disabled) return index
    }
    return from
  }
  function keyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === 'Tab') {
      setOpen(false)
      return
    }
    if (event.key === 'Escape') {
      if (open) {
        event.preventDefault()
        event.stopPropagation()
        setOpen(false)
      }
      return
    }
    if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
      event.preventDefault()
      if (!open) {
        show()
        return
      }
      if (event.key === 'Home') setActive(move(options.length - 1, 1))
      else if (event.key === 'End') setActive(move(0, -1))
      else setActive(move(active, event.key === 'ArrowDown' ? 1 : -1))
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (open) choose(active)
      else show()
    } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      event.preventDefault()
      const now = Date.now()
      const query = now - search.current.time < 700 ? search.current.text + event.key : event.key
      search.current = { text: query, time: now }
      const index = options.findIndex(
        (option) =>
          !option.disabled &&
          option.label.toLocaleLowerCase().startsWith(query.toLocaleLowerCase()),
      )
      if (index >= 0) {
        setActive(index)
        setOpen(true)
      }
    }
  }
  return (
    <span className="select">
      <button
        ref={trigger}
        type="button"
        role="combobox"
        className={`select-trigger ${className}`}
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        aria-activedescendant={open ? `${id}-${active}` : undefined}
        disabled={disabled}
        onClick={() => (open ? setOpen(false) : show())}
        onKeyDown={keyDown}
        onBlur={(event) => {
          if (!popup.current?.contains(event.relatedTarget)) setOpen(false)
        }}
      >
        <span>{options[selectedIndex]?.label}</span>
        <ChevronDown size={16} aria-hidden="true" />
      </button>
      <select
        ref={native}
        name={name}
        value={selected}
        onChange={() => {}}
        disabled={disabled}
        hidden
        aria-hidden="true"
        tabIndex={-1}
      >
        {children}
      </select>
      {open &&
        createPortal(
          <div
            ref={popup}
            id={id}
            role="listbox"
            aria-label={label}
            className="select-popup"
            style={position}
            onPointerDown={(event) => event.preventDefault()}
          >
            {options.map((option, index) => (
              <div
                key={option.value}
                id={`${id}-${index}`}
                role="option"
                aria-selected={option.value === selected}
                aria-disabled={option.disabled || undefined}
                className={`select-option ${active === index ? 'active' : ''}`}
                data-index={index}
                data-value={option.value}
                onPointerMove={() => !option.disabled && setActive(index)}
                onClick={(event) => {
                  event.stopPropagation()
                  choose(index)
                }}
              >
                <span>{option.label}</span>
                {option.value === selected && <Check size={16} aria-hidden="true" />}
              </div>
            ))}
          </div>,
          trigger.current?.closest('dialog') ?? document.body,
        )}
    </span>
  )
}

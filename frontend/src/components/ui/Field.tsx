import type { ComponentPropsWithRef, ReactNode } from 'react'

const CONTROL =
  'block w-full rounded-md border bg-surface px-3 py-2.5 text-base text-ink sm:text-[0.9375rem] ' +
  'placeholder:text-ink-muted transition-colors duration-150 ' +
  'disabled:cursor-not-allowed disabled:bg-surface-2 disabled:text-ink-muted'

const OK_BORDER = 'border-line-strong hover:border-brand-300'
const BAD_BORDER = 'border-danger'

interface FieldProps {
  label: string
  htmlFor: string
  hint?: string
  error?: string
  optional?: boolean
  children: ReactNode
  className?: string
}

export function Field({ label, htmlFor, hint, error, optional, children, className }: FieldProps) {
  return (
    <div className={className}>
      <div className="flex items-baseline justify-between gap-3">
        <label htmlFor={htmlFor} className="text-sm font-semibold text-ink">
          {label}
        </label>
        {optional && <span className="text-xs text-ink-faint">Optional</span>}
      </div>
      {hint && (
        <p id={`${htmlFor}-hint`} className="mt-1 text-xs text-ink-muted">
          {hint}
        </p>
      )}
      <div className="mt-2">{children}</div>
      <FieldError id={htmlFor} message={error} />
    </div>
  )
}

export function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) {
    return null
  }
  return (
    <p id={`${id}-error`} role="alert" className="mt-1.5 text-[0.8125rem] font-medium text-danger-ink">
      {message}
    </p>
  )
}

function describedBy(id: string, hint?: string, error?: string): string | undefined {
  const ids = [hint ? `${id}-hint` : null, error ? `${id}-error` : null].filter(Boolean)
  return ids.length ? ids.join(' ') : undefined
}

type InputProps = ComponentPropsWithRef<'input'> & { error?: string; hint?: string }

export function Input({ id, error, hint, className, ...props }: InputProps) {
  return (
    <input
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={id ? describedBy(id, hint, error) : undefined}
      className={[CONTROL, error ? BAD_BORDER : OK_BORDER, className].filter(Boolean).join(' ')}
      {...props}
    />
  )
}

type SelectProps = ComponentPropsWithRef<'select'> & { error?: string; hint?: string }

export function Select({ id, error, hint, className, children, ...props }: SelectProps) {
  return (
    <select
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={id ? describedBy(id, hint, error) : undefined}
      className={[CONTROL, 'pr-9', error ? BAD_BORDER : OK_BORDER, className]
        .filter(Boolean)
        .join(' ')}
      {...props}
    >
      {children}
    </select>
  )
}

type TextareaProps = ComponentPropsWithRef<'textarea'> & { error?: string; hint?: string }

export function Textarea({ id, error, hint, className, ...props }: TextareaProps) {
  return (
    <textarea
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={id ? describedBy(id, hint, error) : undefined}
      className={[CONTROL, 'resize-y', error ? BAD_BORDER : OK_BORDER, className]
        .filter(Boolean)
        .join(' ')}
      {...props}
    />
  )
}

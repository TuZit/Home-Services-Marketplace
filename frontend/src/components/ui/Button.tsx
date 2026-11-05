import type { ButtonHTMLAttributes, ReactNode } from 'react'

export type ButtonVariant = 'primary' | 'accent' | 'secondary' | 'ghost' | 'danger'
export type ButtonSize = 'sm' | 'md' | 'lg'

const BASE =
  'inline-flex items-center justify-center gap-2 font-semibold whitespace-nowrap ' +
  'touch-manipulation transition-[background-color,border-color,color,box-shadow,transform] ' +
  'duration-150 ease-out active:translate-y-px ' +
  'disabled:pointer-events-none disabled:opacity-55 ' +
  'aria-disabled:pointer-events-none aria-disabled:opacity-55'

const VARIANTS: Record<ButtonVariant, string> = {
  primary:
    'bg-brand-600 text-brand-50 hover:bg-brand-700 shadow-[0_1px_2px_oklch(0.28_0.02_62/0.10)]',
  accent: 'bg-accent-500 text-ink hover:bg-accent-400',
  secondary: 'border border-line-strong bg-surface text-ink hover:bg-surface-2',
  ghost: 'text-ink-muted hover:bg-surface-2 hover:text-ink',
  danger: 'bg-danger text-white hover:bg-danger-ink',
}

const SIZES: Record<ButtonSize, string> = {
  sm: 'h-9 rounded-md px-3 text-[0.8125rem]',
  md: 'h-10 rounded-md px-4 text-sm',
  lg: 'h-12 rounded-lg px-5 text-[0.9375rem]',
}

export function buttonClass(
  variant: ButtonVariant = 'primary',
  size: ButtonSize = 'md',
  extra = '',
): string {
  return [BASE, VARIANTS[variant], SIZES[size], extra].filter(Boolean).join(' ')
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  /** Shows progress without changing the label, so assistive tech still names the action. */
  loading?: boolean
  icon?: ReactNode
}

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  icon,
  className,
  children,
  disabled,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={buttonClass(variant, size, className)}
      disabled={disabled}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading ? (
        <span
          className="size-3.5 shrink-0 animate-spin rounded-full border-2 border-current border-t-transparent"
          aria-hidden="true"
        />
      ) : (
        icon
      )}
      {children}
    </button>
  )
}

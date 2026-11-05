import { AlertTriangleIcon, RefreshIcon } from './icons'

interface ErrorAlertProps {
  message: string
  title?: string
  onRetry?: () => void
}

export default function ErrorAlert({
  message,
  title = 'Something went wrong',
  onRetry,
}: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-lg border border-danger/25 bg-danger-soft px-4 py-3"
    >
      <AlertTriangleIcon className="mt-0.5 size-5 shrink-0 text-danger" />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-danger-ink">{title}</p>
        <p className="mt-0.5 text-sm text-danger-ink/85">{message}</p>
      </div>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex shrink-0 items-center gap-1.5 rounded-md border border-danger/40 px-2.5 py-1.5 text-[0.8125rem] font-semibold text-danger-ink transition-colors hover:bg-danger/10"
        >
          <RefreshIcon className="size-3.5" />
          Try again
        </button>
      )}
    </div>
  )
}

interface SpinnerProps {
  label?: string
}

export default function Spinner({ label = 'Loading' }: SpinnerProps) {
  return (
    <div className="flex justify-center py-16" role="status">
      <span
        className="size-6 animate-spin rounded-full border-2 border-line-strong border-t-brand-600"
        aria-hidden="true"
      />
      <span className="sr-only">{label}</span>
    </div>
  )
}

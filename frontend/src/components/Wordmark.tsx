import { HomeIcon } from './icons'

interface WordmarkProps {
  className?: string
}

export default function Wordmark({ className }: WordmarkProps) {
  return (
    <span className={['inline-flex items-center gap-2.5', className].filter(Boolean).join(' ')}>
      <span className="flex size-9 items-center justify-center rounded-lg bg-brand-600 text-brand-50 shadow-[0_1px_2px_oklch(0.28_0.02_62/0.2)]">
        <HomeIcon className="size-5" />
      </span>
      <span
        translate="no"
        className="text-[1.0625rem] font-bold tracking-[-0.02em] text-ink"
      >
        HomeServices
      </span>
    </span>
  )
}

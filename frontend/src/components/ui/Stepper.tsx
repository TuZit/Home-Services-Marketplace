import { CheckIcon, XIcon } from '../icons'

export interface StepperStep {
  key: string
  label: string
}

interface StepperProps {
  steps: StepperStep[]
  currentIndex: number
  cancelled?: boolean
  label?: string
}

export default function Stepper({ steps, currentIndex, cancelled = false, label }: StepperProps) {
  const current = steps[currentIndex]
  const total = steps.length
  const percent = ((currentIndex + 0.5) / total) * 100

  return (
    <div aria-label={label} role="group">
      {/* Compact form: small screens get a progress bar instead of a squeezed rail. */}
      <div className="sm:hidden">
        <div className="flex items-center justify-between text-[0.8125rem]">
          <span className="font-semibold text-ink">
            {cancelled ? 'Cancelled' : (current?.label ?? '')}
          </span>
          {!cancelled && (
            <span className="text-ink-muted tabular-nums">
              Step {currentIndex + 1} of {total}
            </span>
          )}
        </div>
        <div className="mt-2 h-2 overflow-hidden rounded-full bg-surface-3">
          <div
            className={[
              'h-full rounded-full transition-[width] duration-500 ease-out',
              cancelled ? 'bg-danger' : 'bg-brand-500',
            ].join(' ')}
            style={{ width: cancelled ? '100%' : `${percent}%` }}
          />
        </div>
      </div>

      {/* Full rail: horizontal steps with connectors. */}
      <ol className="hidden sm:flex sm:items-start">
        {steps.map((step, index) => {
          const done = index < currentIndex
          const active = index === currentIndex
          const isCancelled = cancelled && active

          return (
            <li key={step.key} className="flex flex-1 flex-col items-center gap-2 text-center">
              <div className="flex w-full items-center">
                <span
                  className={[
                    'h-0.5 flex-1 rounded-full',
                    index === 0 ? 'bg-transparent' : done || active ? 'bg-brand-400' : 'bg-line',
                  ].join(' ')}
                />
                <span
                  className={[
                    'flex size-8 shrink-0 items-center justify-center rounded-full border text-[0.75rem] font-bold transition-colors duration-300',
                    isCancelled
                      ? 'border-danger bg-danger text-white'
                      : done
                        ? 'border-brand-600 bg-brand-600 text-brand-50'
                        : active
                          ? 'border-brand-600 bg-surface text-brand-700'
                          : 'border-line-strong bg-surface text-ink-faint',
                  ].join(' ')}
                  aria-current={active ? 'step' : undefined}
                >
                  {isCancelled ? (
                    <XIcon className="size-4" />
                  ) : done ? (
                    <CheckIcon className="size-4" />
                  ) : (
                    index + 1
                  )}
                </span>
                <span
                  className={[
                    'h-0.5 flex-1 rounded-full',
                    index === total - 1
                      ? 'bg-transparent'
                      : done
                        ? 'bg-brand-400'
                        : 'bg-line',
                  ].join(' ')}
                />
              </div>
              <span
                className={[
                  'text-[0.8125rem] font-medium',
                  active ? 'text-ink' : done ? 'text-ink-muted' : 'text-ink-faint',
                ].join(' ')}
              >
                {step.label}
              </span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

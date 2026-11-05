import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useAreas, useService, useServiceRequest } from '../../api/hooks'
import { usePageTitle } from '../../lib/usePageTitle'
import { getErrorMessage } from '../../lib/errors'
import { formatDate, formatMoney, formatWindow } from '../../lib/format'
import { REQUEST_STEPS, requestStatusMeta, requestStepIndex } from '../../lib/status'
import { buttonClass } from '../../components/ui/Button'
import Badge from '../../components/ui/Badge'
import Skeleton from '../../components/ui/Skeleton'
import Stepper from '../../components/ui/Stepper'
import ErrorAlert from '../../components/ErrorAlert'
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  CalendarIcon,
  CheckIcon,
  MapPinIcon,
  UsersIcon,
  WalletIcon,
  XIcon,
} from '../../components/icons'

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="min-w-0">
      <dt className="text-[0.6875rem] font-semibold uppercase tracking-wider text-ink-muted">
        {label}
      </dt>
      <dd className="mt-1 text-sm text-ink">{children}</dd>
    </div>
  )
}

export default function RequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const request = useServiceRequest(id)
  const [cancelArmed, setCancelArmed] = useState(false)

  const details = request.data?.details
  const service = useService(details?.serviceId ?? undefined)
  const areas = useAreas()

  const status = requestStatusMeta(request.data?.status)
  usePageTitle(service.data?.name ?? 'Service request')

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['service-request', id] })

  const confirm = useMutation({
    mutationFn: () => apiClient.post(`/service-requests/${id}/confirm`),
    onSuccess: invalidate,
  })

  const cancel = useMutation({
    mutationFn: () => apiClient.post(`/service-requests/${id}/cancel`),
    onSuccess: () => {
      setCancelArmed(false)
      void invalidate()
    },
  })

  if (request.isLoading) {
    return (
      <div className="mx-auto w-full max-w-5xl">
        <Skeleton className="h-4 w-40" />
        <Skeleton className="mt-5 h-9 w-2/3 max-w-lg" />
        <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
          <div className="space-y-6">
            <Skeleton className="h-40 rounded-xl" />
            <Skeleton className="h-56 rounded-xl" />
          </div>
          <Skeleton className="h-48 rounded-xl" />
        </div>
      </div>
    )
  }

  if (request.isError || !request.data || !details) {
    return (
      <div className="mx-auto w-full max-w-2xl space-y-5">
        <ErrorAlert
          title="We could not open this request"
          message={getErrorMessage(request.error)}
          onRetry={() => void request.refetch()}
        />
        <Link to="/catalogue" className={buttonClass('secondary', 'md')}>
          <ArrowLeftIcon className="size-4" />
          Back to the catalogue
        </Link>
      </div>
    )
  }

  const currentStatus = request.data.status
  const isCancelled = currentStatus === 'CANCELLED'
  const isSelected = currentStatus === 'PROVIDER_SELECTED'
  const canPickProvider = currentStatus === 'CONFIRMED' || currentStatus === 'PROVIDERS_FOUND'
  const activeMutation = confirm.isError ? confirm : cancel
  const areaName = areas.data?.find((area) => area.code === details.areaCode)?.name

  return (
    <div className="mx-auto w-full max-w-5xl">
      <nav aria-label="Breadcrumb">
        <Link
          to="/catalogue"
          className="inline-flex items-center gap-1.5 rounded-md text-[0.8125rem] font-medium text-ink-muted transition-colors hover:text-ink"
        >
          <ArrowLeftIcon className="size-3.5" />
          Back to the catalogue
        </Link>
      </nav>

      <header className="mt-5 flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-eyebrow uppercase text-brand-700">Service request</p>
          <h1 className="mt-2 text-display-sm text-ink">
            {service.data?.name ?? 'Service request'}
          </h1>
          <p className="mt-1.5 text-[0.8125rem] text-ink-muted tabular-nums">
            Created {formatDate(request.data.createdAt)}
          </p>
        </div>
        <Badge tone={status.tone}>{status.label}</Badge>
      </header>

      <section className="mt-8 rounded-xl border border-line bg-surface px-5 py-6">
        <Stepper
          steps={REQUEST_STEPS}
          currentIndex={requestStepIndex(currentStatus)}
          cancelled={isCancelled}
          label="Request progress"
        />
        <p className="mt-5 border-t border-line pt-4 text-sm text-ink-muted">{status.description}</p>
      </section>

      <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem] lg:items-start">
        <div className="space-y-6">
          <section className="rounded-xl border border-line bg-surface p-5 sm:p-6">
            <h2 className="text-base font-semibold text-ink">What you asked for</h2>
            <p className="mt-3 whitespace-pre-wrap text-[0.9375rem] leading-relaxed text-ink-muted">
              {details.description}
            </p>
          </section>

          <section className="rounded-xl border border-line bg-surface p-5 sm:p-6">
            <h2 className="text-base font-semibold text-ink">Details</h2>
            <dl className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Detail label="Service">{service.data?.name ?? details.serviceId}</Detail>

              <Detail label="Budget">
                {typeof details.budgetAmount === 'number' ? (
                  <span className="inline-flex items-center gap-1.5 tabular-nums">
                    <WalletIcon className="size-4 text-ink-muted" />
                    {formatMoney({
                      amount: details.budgetAmount,
                      currency: details.budgetCurrency ?? 'VND',
                    })}
                  </span>
                ) : (
                  <span className="text-ink-muted">Open to quotes</span>
                )}
              </Detail>

              <Detail label="When">
                <span className="inline-flex items-start gap-1.5 tabular-nums">
                  <CalendarIcon className="mt-0.5 size-4 shrink-0 text-ink-muted" />
                  {formatWindow(details.preferredStart, details.preferredEnd)}
                </span>
              </Detail>

              <Detail label="Where">
                <span className="inline-flex items-start gap-1.5">
                  <MapPinIcon className="mt-0.5 size-4 shrink-0 text-ink-muted" />
                  <span>
                    {details.addressSnapshot}
                    {areaName ? ` · ${areaName}` : ''}
                  </span>
                </span>
              </Detail>

              <Detail label="Version">
                <span className="tabular-nums">
                  v{request.data.currentVersion} · {details.version}
                </span>
              </Detail>

              <Detail label="Last updated">
                <span className="tabular-nums">{formatDate(request.data.updatedAt)}</span>
              </Detail>
            </dl>
          </section>
        </div>

        <aside className="space-y-4 lg:sticky lg:top-24">
          {activeMutation.isError && (
            <ErrorAlert
              title={activeMutation === confirm ? 'Could not confirm' : 'Could not cancel'}
              message={getErrorMessage(activeMutation.error)}
            />
          )}

          <section className="rounded-xl border border-line bg-surface p-5">
            <h2 className="text-eyebrow uppercase text-ink-muted">Next step</h2>

            {currentStatus === 'DRAFT' && (
              <>
                <p className="mt-3 text-sm leading-relaxed text-ink-muted">
                  Confirm the request and we will look for providers who cover this service and
                  area.
                </p>
                <button
                  type="button"
                  onClick={() => confirm.mutate()}
                  disabled={confirm.isPending}
                  aria-busy={confirm.isPending || undefined}
                  className={buttonClass('primary', 'lg', 'mt-4 w-full')}
                >
                  {confirm.isPending ? (
                    <span
                      className="size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
                      aria-hidden="true"
                    />
                  ) : (
                    <CheckIcon className="size-4" />
                  )}
                  {confirm.isPending ? 'Confirming…' : 'Confirm request'}
                </button>
              </>
            )}

            {canPickProvider && (
              <>
                <p className="mt-3 text-sm leading-relaxed text-ink-muted">
                  {currentStatus === 'PROVIDERS_FOUND'
                    ? 'Providers matched your request. Compare them and pick the one you want.'
                    : 'We are matching providers now. You can look at the current matches at any time.'}
                </p>
                <Link
                  to={`/customer/requests/${id}/providers`}
                  className={buttonClass('primary', 'lg', 'mt-4 w-full')}
                >
                  <UsersIcon className="size-4" />
                  Compare providers
                </Link>
              </>
            )}

            {isSelected && (
              <p className="mt-3 text-sm leading-relaxed text-ink-muted">
                You have chosen a provider. They will follow up with a quote for this job.
              </p>
            )}

            {isCancelled && (
              <>
                <p className="mt-3 text-sm leading-relaxed text-ink-muted">
                  This request was cancelled. Providers can no longer see it.
                </p>
                <Link
                  to="/customer/requests/new"
                  className={buttonClass('primary', 'md', 'mt-4 w-full')}
                >
                  Post a new request
                </Link>
              </>
            )}

            <Link
              to="/catalogue"
              className="mt-3 inline-flex items-center gap-1.5 rounded-md text-[0.8125rem] font-semibold text-ink-muted transition-colors hover:text-ink"
            >
              Browse more services
              <ArrowRightIcon className="size-3.5" />
            </Link>
          </section>

          {!isCancelled && (
            <section className="rounded-xl border border-line bg-surface p-5">
              <h2 className="text-eyebrow uppercase text-ink-muted">Manage</h2>

              {cancelArmed ? (
                <div className="mt-3">
                  <p className="text-sm leading-relaxed text-ink-muted">
                    Cancel this request? Providers stop seeing it and it cannot be undone.
                  </p>
                  <div className="mt-4 flex gap-2">
                    <button
                      type="button"
                      onClick={() => cancel.mutate()}
                      disabled={cancel.isPending}
                      className={buttonClass('danger', 'md', 'flex-1')}
                    >
                      {cancel.isPending ? 'Cancelling…' : 'Yes, cancel it'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setCancelArmed(false)}
                      className={buttonClass('secondary', 'md', 'flex-1')}
                    >
                      Keep it
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setCancelArmed(true)}
                  className="mt-3 inline-flex items-center gap-1.5 rounded-md text-sm font-semibold text-danger-ink transition-colors hover:text-danger"
                >
                  <XIcon className="size-4" />
                  Cancel this request
                </button>
              )}
            </section>
          )}
        </aside>
      </div>
    </div>
  )
}

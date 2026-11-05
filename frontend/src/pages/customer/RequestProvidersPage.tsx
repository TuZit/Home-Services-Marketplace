import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useEligibleProviders, useServiceRequest } from '../../api/hooks'
import { usePageTitle } from '../../lib/usePageTitle'
import { getErrorMessage } from '../../lib/errors'
import { DAY_LABELS, formatTimeOfDay } from '../../lib/format'
import { prettyCode } from '../../lib/status'
import { buttonClass } from '../../components/ui/Button'
import Badge from '../../components/ui/Badge'
import Avatar from '../../components/ui/Avatar'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorAlert from '../../components/ErrorAlert'
import { ArrowLeftIcon, CheckIcon, ClockIcon, UsersIcon } from '../../components/icons'
import type { AvailabilityWindow } from '../../types'

function windowLabel(window: AvailabilityWindow): string {
  const day = DAY_LABELS[window.dayOfWeek] ?? `Day ${window.dayOfWeek}`
  return `${day} ${formatTimeOfDay(window.startTime)}–${formatTimeOfDay(window.endTime)}`
}

function sortedWindows(windows?: AvailabilityWindow[]): AvailabilityWindow[] {
  if (!windows?.length) {
    return []
  }
  return [...windows].sort(
    (left, right) =>
      left.dayOfWeek - right.dayOfWeek || left.startTime.localeCompare(right.startTime),
  )
}

export default function RequestProvidersPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const request = useServiceRequest(id)

  const status = request.data?.status
  const canDiscover = status === 'CONFIRMED'
  const providers = useEligibleProviders(id, canDiscover)

  usePageTitle('Compare providers')

  const items = providers.data?.providers ?? []
  const noResultReason = providers.data?.noResultReason

  const closedTitle =
    status === 'DRAFT'
      ? 'Confirm the request first'
      : status === 'PROVIDER_SELECTED'
        ? 'A provider is already selected'
        : status === 'CANCELLED'
          ? 'This request was cancelled'
          : 'Matching is not available'

  const closedCopy =
    status === 'DRAFT'
      ? 'Providers are only matched after you confirm this request.'
      : status === 'PROVIDER_SELECTED'
        ? 'Matching closed when you chose your provider. They will follow up with a quote.'
        : status === 'CANCELLED'
          ? 'Cancelled requests are no longer matched with providers.'
          : 'This request cannot be matched with providers right now.'

  const select = useMutation({
    mutationFn: (providerId: string) =>
      apiClient.put(`/service-requests/${id}/provider-selection`, { providerId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-request', id] })
      navigate(`/customer/requests/${id}`)
    },
  })

  return (
    <div className="mx-auto w-full max-w-4xl">
      <nav aria-label="Breadcrumb">
        <Link
          to={`/customer/requests/${id}`}
          className="inline-flex items-center gap-1.5 rounded-md text-[0.8125rem] font-medium text-ink-muted transition-colors hover:text-ink"
        >
          <ArrowLeftIcon className="size-3.5" />
          Back to the request
        </Link>
      </nav>

      <header className="mt-5">
        <p className="text-eyebrow uppercase text-brand-700">Compare</p>
        <h1 className="mt-2 text-display-sm text-ink">Choose your provider</h1>
        <p className="mt-2 max-w-2xl text-body text-ink-muted">
          {canDiscover
            ? 'Providers who offer this service, cover your area, and are free inside your window.'
            : 'Matching is closed for this request.'}
        </p>
      </header>

      {select.isError && (
        <div className="mt-6">
          <ErrorAlert
            title="We could not select that provider"
            message={getErrorMessage(select.error)}
          />
        </div>
      )}

      {canDiscover && !providers.isLoading && !providers.isError && items.length > 0 && (
        <div className="mt-8 flex items-center justify-between border-y border-line py-4">
          <p role="status" className="text-[0.8125rem] text-ink-muted">
            {items.length} {items.length === 1 ? 'provider' : 'providers'} matched
          </p>
          {typeof providers.data?.requestVersion === 'number' && (
            <p className="text-[0.8125rem] text-ink-muted tabular-nums">
              checked against version {providers.data.requestVersion}
            </p>
          )}
        </div>
      )}

      <div className="mt-6">
        {(request.isLoading || (canDiscover && providers.isLoading)) && (
          <ul className="space-y-4">
            {[0, 1, 2].map((index) => (
              <li key={index} className="rounded-xl border border-line bg-surface p-5">
                <div className="flex items-center gap-3.5">
                  <Skeleton className="size-11 rounded-lg" />
                  <div className="flex-1">
                    <Skeleton className="h-5 w-44" />
                    <Skeleton className="mt-2 h-4 w-28" />
                  </div>
                  <Skeleton className="h-10 w-32 rounded-md" />
                </div>
                <Skeleton className="mt-5 h-16 w-full" />
              </li>
            ))}
          </ul>
        )}

        {!request.isLoading && !canDiscover && (
          <EmptyState
            icon={<ClockIcon className="size-6" />}
            title={closedTitle}
            description={closedCopy}
            action={
              <Link to={`/customer/requests/${id}`} className={buttonClass('secondary', 'md')}>
                Back to the request
              </Link>
            }
          />
        )}

        {canDiscover && providers.isError && (
          <ErrorAlert
            title="We could not load providers"
            message={getErrorMessage(providers.error)}
            onRetry={() => void providers.refetch()}
          />
        )}

        {canDiscover && !providers.isLoading && !providers.isError && items.length === 0 && (
          <EmptyState
            icon={<UsersIcon className="size-6" />}
            title="No eligible providers yet"
            description={
              noResultReason
                ? `Matching stopped at: ${prettyCode(noResultReason).toLowerCase()}. We keep looking as new providers join.`
                : 'Nobody offers this service in your area right now. We keep looking as new providers join.'
            }
            action={
              <div className="flex flex-wrap justify-center gap-2">
                <button
                  type="button"
                  onClick={() => void providers.refetch()}
                  className={buttonClass('primary', 'md')}
                >
                  Check again
                </button>
                <Link to={`/customer/requests/${id}`} className={buttonClass('secondary', 'md')}>
                  Back to the request
                </Link>
              </div>
            }
          />
        )}

        {items.length > 0 && (
          <ul className="space-y-4">
            {items.map((provider, index) => {
              const name = provider.businessName ?? 'Service provider'
              const isSelecting = select.isPending && select.variables === provider.providerId
              const windows = sortedWindows(provider.availabilityWindows)
              const reasons = provider.reasonCodes ?? []

              return (
                <li
                  key={provider.providerId}
                  className="animate-rise rounded-xl border border-line bg-surface p-5 transition-[border-color,box-shadow] duration-200 hover:border-brand-200 hover:shadow-lift"
                  style={{ animationDelay: `${Math.min(index, 8) * 26}ms` }}
                >
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="flex min-w-0 items-start gap-3.5">
                      <Avatar name={name} />
                      <div className="min-w-0">
                        <h2 className="text-base font-semibold text-ink">{name}</h2>
                        {provider.description && (
                          <p className="mt-1 max-w-prose text-sm leading-relaxed text-ink-muted">
                            {provider.description}
                          </p>
                        )}
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => select.mutate(provider.providerId)}
                      disabled={isSelecting}
                      aria-busy={isSelecting || undefined}
                      className={buttonClass('primary', 'md', 'shrink-0')}
                    >
                      {isSelecting ? (
                        <span
                          className="size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
                          aria-hidden="true"
                        />
                      ) : (
                        <CheckIcon className="size-4" />
                      )}
                      Select provider
                    </button>
                  </div>

                  {provider.experienceSummary && (
                    <div className="mt-4 border-t border-line pt-4">
                      <p className="text-[0.6875rem] font-semibold uppercase tracking-wider text-ink-muted">
                        Experience
                      </p>
                      <p className="mt-1 text-sm leading-relaxed text-ink-muted">
                        {provider.experienceSummary}
                      </p>
                    </div>
                  )}

                  <dl className="mt-4 grid gap-4 border-t border-line pt-4 sm:grid-cols-2">
                    <div className="min-w-0">
                      <dt className="text-[0.6875rem] font-semibold uppercase tracking-wider text-ink-muted">
                        Why this match
                      </dt>
                      <dd className="mt-2">
                        {reasons.length > 0 ? (
                          <span className="flex flex-wrap gap-1.5">
                            {reasons.map((reason) => (
                              <Badge key={reason} tone="brand" icon={<CheckIcon className="size-3.5" />}>
                                {prettyCode(reason)}
                              </Badge>
                            ))}
                          </span>
                        ) : (
                          <span className="text-sm text-ink-muted">Matched your request</span>
                        )}
                      </dd>
                    </div>

                    <div className="min-w-0">
                      <dt className="text-[0.6875rem] font-semibold uppercase tracking-wider text-ink-muted">
                        Availability
                      </dt>
                      <dd className="mt-2">
                        {windows.length > 0 ? (
                          <span className="flex flex-wrap gap-1.5">
                            {windows.map((window, windowIndex) => (
                              <Badge
                                key={`${window.dayOfWeek}-${window.startTime}-${windowIndex}`}
                                tone="outline"
                                icon={<ClockIcon className="size-3.5" />}
                              >
                                {windowLabel(window)}
                              </Badge>
                            ))}
                          </span>
                        ) : (
                          <span className="text-sm text-ink-muted">No weekly hours published</span>
                        )}
                      </dd>
                    </div>
                  </dl>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}

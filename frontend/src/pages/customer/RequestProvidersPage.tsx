import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useEligibleProviders, useServiceRequest } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import { formatMoney } from '../../lib/format'

export default function RequestProvidersPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const request = useServiceRequest(id)
  const providers = useEligibleProviders(id)

  const select = useMutation({
    mutationFn: (providerId: string) =>
      apiClient.put(`/service-requests/${id}/provider-selection`, { providerId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-request', id] })
      navigate(`/customer/requests/${id}`)
    },
  })

  if (providers.isLoading) {
    return <Spinner />
  }

  if (providers.isError) {
    return <ErrorAlert message={getErrorMessage(providers.error)} />
  }

  const items = providers.data ?? []

  return (
    <div className="mx-auto w-full max-w-3xl">
      <div className="flex items-center gap-3">
        <Link to={`/customer/requests/${id}`} className="text-sm text-slate-500 underline">
          Back to request
        </Link>
        <span className="text-slate-300">/</span>
        <h1 className="text-2xl font-bold">Eligible providers</h1>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        {request.data?.title ? `Request: ${request.data.title}` : 'Choose a provider for this request.'}
      </p>

      {select.isError && (
        <div className="mt-6">
          <ErrorAlert message={getErrorMessage(select.error)} />
        </div>
      )}

      {items.length === 0 ? (
        <p className="mt-8 text-sm text-slate-500">
          No eligible providers found yet. Please check back shortly.
        </p>
      ) : (
        <div className="mt-6 grid grid-cols-1 gap-4">
          {items.map((provider) => (
            <div
              key={provider.providerId}
              className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <h2 className="text-lg font-semibold">
                  {provider.businessName ?? provider.fullName}
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  {typeof provider.rating === 'number' && provider.rating > 0
                    ? `★ ${provider.rating} (${provider.reviewCount ?? 0} reviews)`
                    : 'No ratings yet'}
                  {typeof provider.distanceKm === 'number' && (
                    <span className="ml-2 text-xs text-slate-400">{provider.distanceKm.toFixed(1)} km</span>
                  )}
                </p>
                <p className="mt-2 text-sm text-slate-700">
                  {provider.available ? 'Available' : 'Not currently available'}
                  {provider.nextAvailableSlot && (
                    <span className="ml-2 text-xs text-slate-400">
                      next slot: {provider.nextAvailableSlot}
                    </span>
                  )}
                </p>
                {(provider.estimatedPrice || provider.basePrice) && (
                  <p className="mt-2 text-sm font-semibold text-slate-900">
                    {formatMoney(provider.estimatedPrice ?? provider.basePrice)}
                  </p>
                )}
              </div>

              <button
                type="button"
                onClick={() => select.mutate(provider.providerId)}
                disabled={select.isPending}
                className="shrink-0 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
              >
                {select.isPending ? 'Selecting…' : 'Select provider'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

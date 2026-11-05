import { Link, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useServiceRequest } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import { formatDate, formatMoney, statusLabel } from '../../lib/format'

export default function RequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const request = useServiceRequest(id)

  const confirm = useMutation({
    mutationFn: () => apiClient.post(`/service-requests/${id}/confirm`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['service-request', id] }),
  })

  const cancel = useMutation({
    mutationFn: () => apiClient.post(`/service-requests/${id}/cancel`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['service-request', id] }),
  })

  if (request.isLoading) {
    return <Spinner />
  }

  if (request.isError) {
    return (
      <div className="space-y-4">
        <ErrorAlert message={getErrorMessage(request.error)} />
        <Link to="/customer/requests/new" className="text-sm text-slate-600 underline">
          Create a new request instead
        </Link>
      </div>
    )
  }

  const data = request.data

  return (
    <div className="mx-auto w-full max-w-3xl">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">{data?.title ?? 'Service request'}</h1>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">
          {statusLabel(data?.status)}
        </span>
      </div>

      <div className="mt-6 space-y-6">
        {(confirm.isError || cancel.isError) && (
          <ErrorAlert message={getErrorMessage(confirm.isError ? confirm.error : cancel.error)} />
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-sm font-semibold text-slate-400">Details</h2>
          <p className="mt-3 whitespace-pre-wrap text-slate-700">{data?.description}</p>

          <dl className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <dt className="text-xs font-medium text-slate-400">Service</dt>
              <dd className="mt-1 text-sm text-slate-800">{data?.service?.name ?? data?.serviceId}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-400">Budget</dt>
              <dd className="mt-1 text-sm text-slate-800">{formatMoney(data?.budget)}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-400">Preferred date</dt>
              <dd className="mt-1 text-sm text-slate-800">{formatDate(data?.preferredDate)}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-400">Preferred time</dt>
              <dd className="mt-1 text-sm text-slate-800">{data?.preferredTime ?? '—'}</dd>
            </div>
          </dl>

          {data?.address && (
            <div className="mt-4 border-t border-slate-100 pt-4 text-sm text-slate-600">
              {data.address.line1}
              {data.address.line2 ? `, ${data.address.line2}` : ''}, {data.address.city}
              {data.address.postalCode ? ` ${data.address.postalCode}` : ''}, {data.address.country}
            </div>
          )}
        </section>

        <div className="flex flex-wrap gap-3">
          <Link
            to={`/customer/requests/${id}/providers`}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            View eligible providers
          </Link>

          {data?.status === 'DRAFT' && (
            <button
              type="button"
              onClick={() => confirm.mutate()}
              disabled={confirm.isPending}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
            >
              {confirm.isPending ? 'Confirming…' : 'Confirm request'}
            </button>
          )}

          {data && !['CANCELLED', 'COMPLETED'].includes(data.status) && (
            <button
              type="button"
              onClick={() => cancel.mutate()}
              disabled={cancel.isPending}
              className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
            >
              {cancel.isPending ? 'Cancelling…' : 'Cancel request'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

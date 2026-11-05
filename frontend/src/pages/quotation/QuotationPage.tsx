import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useQuotation } from '../../api/hooks'
import { useAuthStore } from '../../store/auth'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import { formatDate, formatMoney, statusLabel } from '../../lib/format'
import type { Booking } from '../../types'

export default function QuotationPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const quotation = useQuotation(id)

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['quotation', id] })

  const submit = useMutation({
    mutationFn: () => apiClient.post(`/quotations/${id}/submit`),
    onSuccess: invalidate,
  })

  const withdraw = useMutation({
    mutationFn: () => apiClient.post(`/quotations/${id}/withdraw`),
    onSuccess: invalidate,
  })

  const accept = useMutation({
    mutationFn: () =>
      apiClient.post<Booking>(`/quotations/${id}/accept`).then((res) => res.data),
    onSuccess: (booking) => navigate(`/bookings/${booking.id}`),
  })

  const reject = useMutation({
    mutationFn: () => apiClient.post(`/quotations/${id}/reject`),
    onSuccess: invalidate,
  })

  if (quotation.isLoading) {
    return <Spinner />
  }

  if (quotation.isError) {
    return <ErrorAlert message={getErrorMessage(quotation.error)} />
  }

  const data = quotation.data
  const failed = [submit, withdraw, accept, reject].find((m) => m.isError)

  return (
    <div className="mx-auto w-full max-w-2xl">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Quotation</h1>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">
          {statusLabel(data?.status)}
        </span>
      </div>

      <div className="mt-6 space-y-6">
        {failed && <ErrorAlert message={getErrorMessage(failed.error)} />}

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <div className="flex items-end justify-between gap-4">
            <div>
              <p className="text-xs font-medium text-slate-400">Quoted amount</p>
              <p className="mt-1 text-3xl font-bold">{formatMoney(data?.amount)}</p>
            </div>
            <div className="text-right text-sm text-slate-500">
              <p>Provider: {data?.providerName ?? data?.providerId}</p>
              {data?.createdAt && <p className="mt-1">Created {formatDate(data.createdAt)}</p>}
              {data?.validUntil && <p className="mt-1">Valid until {formatDate(data.validUntil)}</p>}
            </div>
          </div>

          {data?.description && (
            <p className="mt-4 whitespace-pre-wrap border-t border-slate-100 pt-4 text-sm text-slate-600">
              {data.description}
            </p>
          )}
        </section>

        {data && (
          <div className="flex flex-wrap gap-3">
            {user?.role === 'PROVIDER' && data.status === 'DRAFT' && (
              <button
                type="button"
                onClick={() => submit.mutate()}
                disabled={submit.isPending}
                className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
              >
                {submit.isPending ? 'Submitting…' : 'Submit quotation'}
              </button>
            )}

            {user?.role === 'PROVIDER' && data.status === 'SUBMITTED' && (
              <button
                type="button"
                onClick={() => withdraw.mutate()}
                disabled={withdraw.isPending}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
              >
                {withdraw.isPending ? 'Withdrawing…' : 'Withdraw'}
              </button>
            )}

            {user?.role === 'CUSTOMER' && data.status === 'SUBMITTED' && (
              <>
                <button
                  type="button"
                  onClick={() => accept.mutate()}
                  disabled={accept.isPending}
                  className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
                >
                  {accept.isPending ? 'Accepting…' : 'Accept'}
                </button>
                <button
                  type="button"
                  onClick={() => reject.mutate()}
                  disabled={reject.isPending}
                  className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                >
                  {reject.isPending ? 'Rejecting…' : 'Reject'}
                </button>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

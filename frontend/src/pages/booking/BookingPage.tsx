import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useBooking } from '../../api/hooks'
import { useAuthStore } from '../../store/auth'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { applyFieldErrors, getErrorMessage } from '../../lib/errors'
import { formatDate, formatMoney, statusLabel } from '../../lib/format'
import type { Payment, Review } from '../../types'

const reviewSchema = z.object({
  rating: z.number().int().min(1).max(5),
  comment: z.string().optional(),
})

type ReviewFormValues = z.infer<typeof reviewSchema>

const inputClass =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none'
const labelClass = 'block text-sm font-medium text-slate-700'

export default function BookingPage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const booking = useBooking(id)

  const [payment, setPayment] = useState<Payment | null>(null)
  const [reviewSaved, setReviewSaved] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ReviewFormValues>({
    resolver: zodResolver(reviewSchema),
    defaultValues: { rating: 5, comment: '' },
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['booking', id] })

  const start = useMutation({
    mutationFn: () => apiClient.post(`/bookings/${id}/start`),
    onSuccess: invalidate,
  })

  const complete = useMutation({
    mutationFn: () => apiClient.post(`/bookings/${id}/complete`),
    onSuccess: invalidate,
  })

  const cancelBooking = useMutation({
    mutationFn: () => apiClient.post(`/bookings/${id}/cancel`),
    onSuccess: invalidate,
  })

  const pay = useMutation({
    mutationFn: () =>
      apiClient.post<Payment>(`/bookings/${id}/payments`).then((res) => res.data),
    onSuccess: (result) => {
      setPayment(result)
      invalidate()
    },
  })

  const simulateResult = useMutation({
    mutationFn: (paymentId: string) => apiClient.post(`/payments/${paymentId}/result`),
    onSuccess: () => {
      setPayment(null)
      invalidate()
    },
  })

  const review = useMutation({
    mutationFn: (payload: { rating: number; comment?: string }) =>
      apiClient.post<Review>(`/bookings/${id}/reviews`, payload).then((res) => res.data),
    onSuccess: () => {
      reset({ rating: 5, comment: '' })
      setReviewSaved(true)
      invalidate()
    },
    onError: (error) => applyFieldErrors(error, setError),
  })

  const onSubmitReview = handleSubmit((values) => {
    review.mutate({ rating: values.rating, comment: values.comment || undefined })
  })

  if (booking.isLoading) {
    return <Spinner />
  }

  if (booking.isError) {
    return <ErrorAlert message={getErrorMessage(booking.error)} />
  }

  const data = booking.data
  const failed = [start, complete, cancelBooking, pay, simulateResult, review].find(
    (m) => m.isError,
  )

  return (
    <div className="mx-auto w-full max-w-2xl">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Booking</h1>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">
          {statusLabel(data?.status)}
        </span>
      </div>

      <div className="mt-6 space-y-6">
        {failed && <ErrorAlert message={getErrorMessage(failed.error)} />}
        {reviewSaved && (
          <div className="rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
            Thank you! Your review has been submitted.
          </div>
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <dt className="text-xs font-medium text-slate-400">Total</dt>
              <dd className="mt-1 text-2xl font-bold">{formatMoney(data?.total)}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-400">Provider</dt>
              <dd className="mt-1 text-sm text-slate-800">{data?.providerName ?? data?.providerId}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-400">Scheduled</dt>
              <dd className="mt-1 text-sm text-slate-800">{formatDate(data?.scheduledAt)}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-400">Created</dt>
              <dd className="mt-1 text-sm text-slate-800">{formatDate(data?.createdAt)}</dd>
            </div>
          </dl>
        </section>

        {data && (
          <div className="flex flex-wrap gap-3">
            {user?.role === 'PROVIDER' && ['RESERVED', 'SCHEDULED'].includes(data.status) && (
              <button
                type="button"
                onClick={() => start.mutate()}
                disabled={start.isPending}
                className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
              >
                {start.isPending ? 'Starting…' : 'Start job'}
              </button>
            )}

            {user?.role === 'PROVIDER' && data.status === 'IN_PROGRESS' && (
              <button
                type="button"
                onClick={() => complete.mutate()}
                disabled={complete.isPending}
                className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
              >
                {complete.isPending ? 'Completing…' : 'Complete job'}
              </button>
            )}

            {!['COMPLETED', 'CANCELLED'].includes(data.status) && (
              <button
                type="button"
                onClick={() => cancelBooking.mutate()}
                disabled={cancelBooking.isPending}
                className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
              >
                {cancelBooking.isPending ? 'Cancelling…' : 'Cancel booking'}
              </button>
            )}

            {user?.role === 'CUSTOMER' && !['COMPLETED', 'CANCELLED'].includes(data.status) && (
              <button
                type="button"
                onClick={() => pay.mutate()}
                disabled={pay.isPending}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
              >
                {pay.isPending ? 'Creating payment…' : 'Pay now'}
              </button>
            )}
          </div>
        )}

        {payment && (
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <p className="text-sm font-medium text-slate-700">Payment created</p>
            <p className="mt-2 text-sm text-slate-500">
              Payment id: {payment.id} · {statusLabel(payment.status)} · {formatMoney(payment.amount)}
            </p>
            <button
              type="button"
              onClick={() => simulateResult.mutate(payment.id)}
              disabled={simulateResult.isPending}
              className="mt-3 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {simulateResult.isPending ? 'Processing…' : 'Simulate payment result'}
            </button>
          </div>
        )}

        {user?.role === 'CUSTOMER' && data?.status === 'COMPLETED' && (
          <section className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-lg font-semibold">Leave a review</h2>
            {review.isError && (
              <div className="mt-4">
                <ErrorAlert message={getErrorMessage(review.error)} />
              </div>
            )}
            <form onSubmit={onSubmitReview} className="mt-4 space-y-4" noValidate>
              <div>
                <label htmlFor="rating" className={labelClass}>
                  Rating
                </label>
                <select
                  id="rating"
                  {...register('rating', { valueAsNumber: true })}
                  className={inputClass}
                >
                  <option value={5}>5 — Excellent</option>
                  <option value={4}>4 — Good</option>
                  <option value={3}>3 — Average</option>
                  <option value={2}>2 — Poor</option>
                  <option value={1}>1 — Terrible</option>
                </select>
                {errors.rating && (
                  <p className="mt-1 text-sm text-red-600">{errors.rating.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="comment" className={labelClass}>
                  Comment (optional)
                </label>
                <textarea id="comment" rows={3} {...register('comment')} className={inputClass} />
              </div>

              <button
                type="submit"
                disabled={review.isPending}
                className="rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60"
              >
                {review.isPending ? 'Submitting…' : 'Submit review'}
              </button>
            </form>
          </section>
        )}
      </div>
    </div>
  )
}

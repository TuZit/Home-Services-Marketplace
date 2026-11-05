import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useAdminReviews } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import { formatDate } from '../../lib/format'

export default function AdminReviewsPage() {
  const queryClient = useQueryClient()
  const reviews = useAdminReviews()

  const toggle = useMutation({
    mutationFn: ({ reviewId, hidden }: { reviewId: string; hidden: boolean }) =>
      apiClient.post(`/admin/reviews/${reviewId}/${hidden ? 'restore' : 'hide'}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-reviews'] }),
  })

  if (reviews.isLoading) {
    return <Spinner />
  }

  if (reviews.isError) {
    return <ErrorAlert message={getErrorMessage(reviews.error)} />
  }

  const items = reviews.data ?? []

  return (
    <div>
      <h1 className="text-2xl font-bold">Review moderation</h1>
      <p className="mt-1 text-sm text-slate-500">Hide or restore customer reviews.</p>

      {toggle.isError && (
        <div className="mt-6">
          <ErrorAlert message={getErrorMessage(toggle.error)} />
        </div>
      )}

      {items.length === 0 ? (
        <p className="mt-8 text-sm text-slate-500">No reviews found.</p>
      ) : (
        <div className="mt-6 space-y-4">
          {items.map((review) => (
            <div
              key={review.id}
              className="flex flex-wrap items-start justify-between gap-4 rounded-xl border border-slate-200 bg-white p-5"
            >
              <div>
                <p className="font-semibold">
                  {'★'.repeat(review.rating)}
                  <span className="ml-2 text-xs font-normal text-slate-400">
                    {review.rating}/5
                  </span>
                </p>
                {review.comment && (
                  <p className="mt-2 text-sm text-slate-600">{review.comment}</p>
                )}
                <p className="mt-2 text-xs text-slate-400">
                  Booking {review.bookingId}
                  {review.createdAt && ` · ${formatDate(review.createdAt)}`}
                </p>
              </div>
              <div className="flex flex-col items-end gap-2">
                <span
                  className={`rounded-full px-2 py-1 text-xs font-medium ${
                    review.hidden ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                  }`}
                >
                  {review.hidden ? 'Hidden' : 'Visible'}
                </span>
                <button
                  type="button"
                  onClick={() => toggle.mutate({ reviewId: review.id, hidden: review.hidden })}
                  disabled={toggle.isPending}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
                >
                  {review.hidden ? 'Restore' : 'Hide'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

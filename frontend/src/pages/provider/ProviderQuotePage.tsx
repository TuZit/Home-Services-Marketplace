import { Link, useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useServiceRequest } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { applyFieldErrors, getErrorMessage } from '../../lib/errors'
import { formatMoney } from '../../lib/format'
import type { Quotation } from '../../types'

const quoteSchema = z.object({
  amount: z.string().min(1, 'Enter a quote amount'),
  description: z.string().min(5, 'Add a short description of your quote'),
})

type QuoteFormValues = z.infer<typeof quoteSchema>

const inputClass =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none'
const labelClass = 'block text-sm font-medium text-slate-700'

export default function ProviderQuotePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const request = useServiceRequest(id)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<QuoteFormValues>({
    resolver: zodResolver(quoteSchema),
    defaultValues: { amount: '', description: '' },
  })

  const mutation = useMutation({
    mutationFn: (payload: { amount: { amount: number; currency: string }; description: string }) =>
      apiClient
        .post<Quotation>(`/provider/requests/${id}/quotations`, payload)
        .then((res) => res.data),
    onSuccess: (quotation) => navigate(`/quotations/${quotation.id}`),
    onError: (error) => applyFieldErrors(error, setError),
  })

  const onSubmit = handleSubmit((values) => {
    mutation.mutate({
      amount: { amount: Number(values.amount), currency: 'USD' },
      description: values.description,
    })
  })

  return (
    <div className="mx-auto w-full max-w-xl">
      <div className="flex items-center gap-3">
        <Link to="/provider/profile" className="text-sm text-slate-500 underline">
          Provider home
        </Link>
        <span className="text-slate-300">/</span>
        <h1 className="text-2xl font-bold">Submit a quote</h1>
      </div>

      {request.data && (
        <div className="mt-4 rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">Request details</h2>
          <p className="mt-2 whitespace-pre-wrap text-sm text-slate-600">
            {request.data.details.description}
          </p>
          <p className="mt-3 text-sm text-slate-500">
            Customer budget:{' '}
            {typeof request.data.details.budgetAmount === 'number'
              ? formatMoney({
                  amount: request.data.details.budgetAmount,
                  currency: request.data.details.budgetCurrency ?? 'VND',
                })
              : 'not set'}
          </p>
        </div>
      )}

      <div className="mt-6 space-y-4">
        {mutation.isError && <ErrorAlert message={getErrorMessage(mutation.error)} />}

        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div>
            <label htmlFor="amount" className={labelClass}>
              Quote amount (USD)
            </label>
            <input id="amount" type="number" min="0" step="0.01" {...register('amount')} className={inputClass} />
            {errors.amount && <p className="mt-1 text-sm text-red-600">{errors.amount.message}</p>}
          </div>

          <div>
            <label htmlFor="description" className={labelClass}>
              Description
            </label>
            <textarea id="description" rows={4} {...register('description')} className={inputClass} />
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60"
          >
            {mutation.isPending ? 'Submitting…' : 'Create draft quotation'}
          </button>
        </form>
      </div>

      {request.isLoading && <Spinner />}
    </div>
  )
}

import { useEffect, useRef } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router-dom'
import { apiClient } from '../../api/client'
import { useAddresses, useCategories, useCategoryServices } from '../../api/hooks'
import { applyFieldErrors, getErrorMessage } from '../../lib/errors'
import ErrorAlert from '../../components/ErrorAlert'
import Spinner from '../../components/Spinner'
import type { ServiceRequest } from '../../types'

const requestSchema = z.object({
  categoryId: z.string().min(1, 'Select a category'),
  serviceId: z.string().min(1, 'Select a service'),
  title: z.string().min(3, 'Title must be at least 3 characters'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  line1: z.string().min(1, 'Street address is required'),
  city: z.string().min(1, 'City is required'),
  postalCode: z.string().optional(),
  country: z.string().min(1, 'Country is required'),
  preferredDate: z.string().optional(),
  preferredTime: z.string().optional(),
  budget: z.string().optional(),
})

type RequestFormValues = z.infer<typeof requestSchema>

interface CreateRequestPayload {
  serviceId: string
  title: string
  description: string
  address: {
    line1: string
    city: string
    postalCode?: string | null
    country: string
  }
  preferredDate?: string | null
  preferredTime?: string | null
  budget?: { amount: number; currency: string }
}

const inputClass =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none'
const labelClass = 'block text-sm font-medium text-slate-700'

export default function NewRequestPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()

  const categories = useCategories()
  const addresses = useAddresses()

  const initial = (location.state as { serviceId?: string; categoryId?: string } | null) ?? {}

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = useForm<RequestFormValues>({
    resolver: zodResolver(requestSchema),
    defaultValues: {
      categoryId: initial.categoryId ?? '',
      serviceId: initial.serviceId ?? '',
      title: '',
      description: '',
      line1: '',
      city: '',
      postalCode: '',
      country: '',
      preferredDate: '',
      preferredTime: '',
      budget: '',
    },
  })

  const categoryId = watch('categoryId')
  const services = useCategoryServices(categoryId || undefined)

  // Clear the selected service whenever the user switches category.
  const previousCategory = useRef(categoryId)
  useEffect(() => {
    if (previousCategory.current !== categoryId) {
      setValue('serviceId', '')
      previousCategory.current = categoryId
    }
  }, [categoryId, setValue])

  const mutation = useMutation({
    mutationFn: (payload: CreateRequestPayload) =>
      apiClient.post<ServiceRequest>('/service-requests', payload).then((res) => res.data),
    onSuccess: (request) => {
      queryClient.setQueryData(['service-request', request.id], request)
      navigate(`/customer/requests/${request.id}`)
    },
    onError: (error) => applyFieldErrors(error, setError),
  })

  const onSubmit = handleSubmit((values) => {
    mutation.mutate({
      serviceId: values.serviceId,
      title: values.title,
      description: values.description,
      address: {
        line1: values.line1,
        city: values.city,
        postalCode: values.postalCode || null,
        country: values.country,
      },
      preferredDate: values.preferredDate || null,
      preferredTime: values.preferredTime || null,
      budget: values.budget
        ? { amount: Number(values.budget), currency: 'USD' }
        : undefined,
    })
  })

  return (
    <div className="mx-auto w-full max-w-2xl">
      <h1 className="text-2xl font-bold">Create a service request</h1>
      <p className="mt-1 text-sm text-slate-500">
        Tell providers what you need and where the work should happen.
      </p>

      <div className="mt-6 space-y-4">
        {mutation.isError && <ErrorAlert message={getErrorMessage(mutation.error)} />}

        {addresses.data && addresses.data.length > 0 && (
          <div className="rounded-lg border border-slate-200 bg-white p-4">
            <p className="text-sm font-medium text-slate-700">Saved addresses</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {addresses.data.map((address) => (
                <button
                  key={address.id}
                  type="button"
                  onClick={() => {
                    setValue('line1', address.line1)
                    setValue('city', address.city)
                    setValue('postalCode', address.postalCode ?? '')
                    setValue('country', address.country)
                  }}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50"
                >
                  {address.line1}, {address.city}
                </button>
              ))}
            </div>
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div>
            <label htmlFor="categoryId" className={labelClass}>
              Category
            </label>
            <select id="categoryId" {...register('categoryId')} className={inputClass}>
              <option value="">Select a category…</option>
              {(categories.data ?? []).map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
            {errors.categoryId && (
              <p className="mt-1 text-sm text-red-600">{errors.categoryId.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="serviceId" className={labelClass}>
              Service
            </label>
            <select
              id="serviceId"
              {...register('serviceId')}
              disabled={!categoryId}
              className={inputClass}
            >
              <option value="">Select a service…</option>
              {(services.data ?? []).map((service) => (
                <option key={service.id} value={service.id}>
                  {service.name}
                </option>
              ))}
            </select>
            {services.isLoading && categoryId && (
              <p className="mt-1 text-xs text-slate-400">Loading services…</p>
            )}
            {errors.serviceId && (
              <p className="mt-1 text-sm text-red-600">{errors.serviceId.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="title" className={labelClass}>
              Title
            </label>
            <input id="title" type="text" {...register('title')} className={inputClass} />
            {errors.title && <p className="mt-1 text-sm text-red-600">{errors.title.message}</p>}
          </div>

          <div>
            <label htmlFor="description" className={labelClass}>
              Description
            </label>
            <textarea
              id="description"
              rows={4}
              {...register('description')}
              className={inputClass}
            />
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          <fieldset className="space-y-4 rounded-lg border border-slate-200 p-4">
            <legend className="px-1 text-sm font-medium text-slate-700">Address</legend>
            <div>
              <label htmlFor="line1" className={labelClass}>
                Street address
              </label>
              <input id="line1" type="text" {...register('line1')} className={inputClass} />
              {errors.line1 && (
                <p className="mt-1 text-sm text-red-600">{errors.line1.message}</p>
              )}
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="city" className={labelClass}>
                  City
                </label>
                <input id="city" type="text" {...register('city')} className={inputClass} />
                {errors.city && (
                  <p className="mt-1 text-sm text-red-600">{errors.city.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="postalCode" className={labelClass}>
                  Postal code
                </label>
                <input id="postalCode" type="text" {...register('postalCode')} className={inputClass} />
              </div>
            </div>
            <div>
              <label htmlFor="country" className={labelClass}>
                Country
              </label>
              <input id="country" type="text" {...register('country')} className={inputClass} />
              {errors.country && (
                <p className="mt-1 text-sm text-red-600">{errors.country.message}</p>
              )}
            </div>
          </fieldset>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="preferredDate" className={labelClass}>
                Preferred date
              </label>
              <input id="preferredDate" type="date" {...register('preferredDate')} className={inputClass} />
            </div>
            <div>
              <label htmlFor="preferredTime" className={labelClass}>
                Preferred time
              </label>
              <input id="preferredTime" type="time" {...register('preferredTime')} className={inputClass} />
            </div>
          </div>

          <div>
            <label htmlFor="budget" className={labelClass}>
              Budget (optional, USD)
            </label>
            <input id="budget" type="number" min="0" step="0.01" {...register('budget')} className={inputClass} />
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60"
          >
            {mutation.isPending ? 'Creating request…' : 'Create request'}
          </button>
        </form>
      </div>

      {categories.isLoading && <Spinner />}
    </div>
  )
}

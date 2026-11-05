import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useCategories, useCategoryServices } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import type { ServiceCategory } from '../../types'

const categorySchema = z.object({
  name: z.string().min(1, 'Category name is required'),
  description: z.string().optional(),
})

const serviceSchema = z.object({
  categoryId: z.string().min(1, 'Select a category'),
  name: z.string().min(1, 'Service name is required'),
  description: z.string().optional(),
  price: z.string().optional(),
  durationMinutes: z.string().optional(),
})

type CategoryFormValues = z.infer<typeof categorySchema>
type ServiceFormValues = z.infer<typeof serviceSchema>

const inputClass =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none'
const labelClass = 'block text-sm font-medium text-slate-700'

export default function AdminCatalogPage() {
  const queryClient = useQueryClient()
  const categories = useCategories()
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const services = useCategoryServices(selectedCategoryId || undefined)

  const categoryForm = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: { name: '', description: '' },
  })

  const serviceForm = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceSchema),
    defaultValues: { categoryId: '', name: '', description: '', price: '', durationMinutes: '' },
  })

  const createCategory = useMutation({
    mutationFn: (payload: CategoryFormValues) =>
      apiClient.post<ServiceCategory>('/admin/categories', payload).then((res) => res.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      categoryForm.reset({ name: '', description: '' })
    },
  })

  const createService = useMutation({
    mutationFn: (payload: {
      categoryId: string
      name: string
      description?: string
      price?: { amount: number; currency: string }
      durationMinutes?: number
    }) => apiClient.post('/admin/services', payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['category-services', variables.categoryId] })
      setSelectedCategoryId(variables.categoryId)
      serviceForm.reset({
        categoryId: variables.categoryId,
        name: '',
        description: '',
        price: '',
        durationMinutes: '',
      })
    },
  })

  const toggleService = useMutation({
    mutationFn: ({ serviceId, active }: { serviceId: string; active: boolean }) =>
      active
        ? apiClient.put(`/admin/services/${serviceId}`, { active: true })
        : apiClient.delete(`/admin/services/${serviceId}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['category-services', selectedCategoryId] }),
  })

  const onSubmitCategory = categoryForm.handleSubmit((values) => createCategory.mutate(values))

  const onSubmitService = serviceForm.handleSubmit((values) => {
    createService.mutate({
      categoryId: values.categoryId,
      name: values.name,
      description: values.description || undefined,
      price: values.price ? { amount: Number(values.price), currency: 'USD' } : undefined,
      durationMinutes: values.durationMinutes ? Number(values.durationMinutes) : undefined,
    })
  })

  if (categories.isLoading) {
    return <Spinner />
  }

  if (categories.isError) {
    return <ErrorAlert message={getErrorMessage(categories.error)} />
  }

  const items = categories.data ?? []

  return (
    <div>
      <h1 className="text-2xl font-bold">Catalogue management</h1>
      <p className="mt-1 text-sm text-slate-500">Create categories and services, and control availability.</p>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-semibold">New category</h2>
          {createCategory.isError && (
            <div className="mt-4">
              <ErrorAlert message={getErrorMessage(createCategory.error)} />
            </div>
          )}
          <form onSubmit={onSubmitCategory} className="mt-4 space-y-4" noValidate>
            <div>
              <label htmlFor="categoryName" className={labelClass}>
                Name
              </label>
              <input id="categoryName" type="text" {...categoryForm.register('name')} className={inputClass} />
              {categoryForm.formState.errors.name && (
                <p className="mt-1 text-sm text-red-600">
                  {categoryForm.formState.errors.name.message}
                </p>
              )}
            </div>
            <div>
              <label htmlFor="categoryDescription" className={labelClass}>
                Description
              </label>
              <textarea
                id="categoryDescription"
                rows={3}
                {...categoryForm.register('description')}
                className={inputClass}
              />
            </div>
            <button
              type="submit"
              disabled={createCategory.isPending}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {createCategory.isPending ? 'Creating…' : 'Create category'}
            </button>
          </form>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-semibold">New service</h2>
          {createService.isError && (
            <div className="mt-4">
              <ErrorAlert message={getErrorMessage(createService.error)} />
            </div>
          )}
          <form onSubmit={onSubmitService} className="mt-4 space-y-4" noValidate>
            <div>
              <label htmlFor="serviceCategoryId" className={labelClass}>
                Category
              </label>
              <select
                id="serviceCategoryId"
                {...serviceForm.register('categoryId')}
                className={inputClass}
              >
                <option value="">Select a category…</option>
                {items.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              {serviceForm.formState.errors.categoryId && (
                <p className="mt-1 text-sm text-red-600">
                  {serviceForm.formState.errors.categoryId.message}
                </p>
              )}
            </div>
            <div>
              <label htmlFor="serviceName" className={labelClass}>
                Name
              </label>
              <input id="serviceName" type="text" {...serviceForm.register('name')} className={inputClass} />
              {serviceForm.formState.errors.name && (
                <p className="mt-1 text-sm text-red-600">{serviceForm.formState.errors.name.message}</p>
              )}
            </div>
            <div>
              <label htmlFor="serviceDescription" className={labelClass}>
                Description
              </label>
              <textarea
                id="serviceDescription"
                rows={2}
                {...serviceForm.register('description')}
                className={inputClass}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="servicePrice" className={labelClass}>
                  Price (USD)
                </label>
                <input
                  id="servicePrice"
                  type="number"
                  min="0"
                  step="0.01"
                  {...serviceForm.register('price')}
                  className={inputClass}
                />
              </div>
              <div>
                <label htmlFor="serviceDuration" className={labelClass}>
                  Duration (min)
                </label>
                <input
                  id="serviceDuration"
                  type="number"
                  min="0"
                  {...serviceForm.register('durationMinutes')}
                  className={inputClass}
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={createService.isPending}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {createService.isPending ? 'Creating…' : 'Create service'}
            </button>
          </form>
        </section>
      </div>

      <section className="mt-8 rounded-xl border border-slate-200 bg-white p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Services by category</h2>
          <select
            aria-label="View services by category"
            value={selectedCategoryId}
            onChange={(event) => setSelectedCategoryId(event.target.value)}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">Select a category…</option>
            {items.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        {toggleService.isError && (
          <div className="mt-4">
            <ErrorAlert message={getErrorMessage(toggleService.error)} />
          </div>
        )}

        {selectedCategoryId && services.isLoading && <Spinner />}

        {selectedCategoryId && !services.isLoading && (services.data ?? []).length === 0 && (
          <p className="mt-4 text-sm text-slate-500">No services in this category yet.</p>
        )}

        {selectedCategoryId && (services.data ?? []).length > 0 && (
          <div className="mt-4 divide-y divide-slate-100">
            {(services.data ?? []).map((service) => (
              <div key={service.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <p className="font-medium">{service.name}</p>
                  <p className="text-sm text-slate-500">
                    {service.active ? 'Active' : 'Inactive'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => toggleService.mutate({ serviceId: service.id, active: service.active })}
                  disabled={toggleService.isPending}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium disabled:opacity-60 ${
                    service.active
                      ? 'border border-red-300 text-red-700 hover:bg-red-50'
                      : 'bg-slate-900 text-white hover:bg-slate-700'
                  }`}
                >
                  {service.active ? 'Deactivate' : 'Activate'}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

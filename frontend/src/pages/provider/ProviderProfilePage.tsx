import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useProviderProfile } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { applyFieldErrors, getErrorMessage } from '../../lib/errors'

const profileSchema = z.object({
  businessName: z.string().optional(),
  bio: z.string().optional(),
  phone: z.string().optional(),
})

type ProfileFormValues = z.infer<typeof profileSchema>

const inputClass =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none'
const labelClass = 'block text-sm font-medium text-slate-700'

export default function ProviderProfilePage() {
  const queryClient = useQueryClient()
  const profile = useProviderProfile()

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { businessName: '', bio: '', phone: '' },
  })

  useEffect(() => {
    if (profile.data) {
      reset({
        businessName: profile.data.businessName ?? '',
        bio: profile.data.bio ?? '',
        phone: profile.data.phone ?? '',
      })
    }
  }, [profile.data, reset])

  const save = useMutation({
    mutationFn: (payload: ProfileFormValues) => apiClient.put('/provider/profile', payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['provider-profile'] }),
    onError: (error) => applyFieldErrors(error, setError),
  })

  const verify = useMutation({
    mutationFn: () => apiClient.post('/provider/submit-verification'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['provider-profile'] }),
  })

  const onSubmit = handleSubmit((values) => save.mutate(values))

  if (profile.isLoading) {
    return <Spinner />
  }

  if (profile.isError) {
    return <ErrorAlert message={getErrorMessage(profile.error)} />
  }

  const data = profile.data

  return (
    <div className="mx-auto w-full max-w-2xl">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Provider profile</h1>
        <span
          className={`rounded-full px-3 py-1 text-xs font-medium ${
            data?.verified ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
          }`}
        >
          {data?.verified ? 'Verified' : 'Pending verification'}
        </span>
      </div>

      {typeof data?.rating === 'number' && (
        <p className="mt-2 text-sm text-slate-500">
          ★ {data.rating} ({data.reviewCount ?? 0} reviews)
        </p>
      )}

      <div className="mt-6 space-y-6">
        {(save.isError || verify.isError) && (
          <ErrorAlert message={getErrorMessage(save.isError ? save.error : verify.error)} />
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-semibold">Business details</h2>
          <form onSubmit={onSubmit} className="mt-4 space-y-4" noValidate>
            <div>
              <label htmlFor="businessName" className={labelClass}>
                Business name
              </label>
              <input id="businessName" type="text" {...register('businessName')} className={inputClass} />
              {errors.businessName && (
                <p className="mt-1 text-sm text-red-600">{errors.businessName.message}</p>
              )}
            </div>

            <div>
              <label htmlFor="bio" className={labelClass}>
                Bio
              </label>
              <textarea id="bio" rows={4} {...register('bio')} className={inputClass} />
            </div>

            <div>
              <label htmlFor="phone" className={labelClass}>
                Phone
              </label>
              <input id="phone" type="tel" {...register('phone')} className={inputClass} />
            </div>

            <button
              type="submit"
              disabled={save.isPending}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {save.isPending ? 'Saving…' : 'Save profile'}
            </button>
          </form>
        </section>

        {!data?.verified && (
          <section className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="text-lg font-semibold">Verification</h2>
            <p className="mt-1 text-sm text-slate-500">
              Submit your profile for admin verification to start receiving requests.
            </p>
            <button
              type="button"
              onClick={() => verify.mutate()}
              disabled={verify.isPending}
              className="mt-4 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {verify.isPending ? 'Submitting…' : 'Submit for verification'}
            </button>
          </section>
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-semibold">Services & areas</h2>
          {data?.services && data.services.length > 0 ? (
            <ul className="mt-3 list-inside list-disc text-sm text-slate-600">
              {data.services.map((service) => (
                <li key={service.id}>{service.name}</li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-slate-500">No services configured yet.</p>
          )}
          {data?.areas && data.areas.length > 0 && (
            <p className="mt-3 text-sm text-slate-600">Areas: {data.areas.join(', ')}</p>
          )}
        </section>
      </div>
    </div>
  )
}

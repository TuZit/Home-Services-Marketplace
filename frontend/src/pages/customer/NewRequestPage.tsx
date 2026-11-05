import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router-dom'
import { apiClient } from '../../api/client'
import { useAddresses, useAreas, useCategories, useCategoryServices } from '../../api/hooks'
import { applyFieldErrors, getErrorMessage } from '../../lib/errors'
import { usePageTitle } from '../../lib/usePageTitle'
import { formatWindow } from '../../lib/format'
import { buttonClass } from '../../components/ui/Button'
import { Field, Input, Select, Textarea } from '../../components/ui/Field'
import Stepper from '../../components/ui/Stepper'
import Badge from '../../components/ui/Badge'
import ErrorAlert from '../../components/ErrorAlert'
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  CalendarIcon,
  CheckIcon,
  ClockIcon,
  MapPinIcon,
  PlusIcon,
  WalletIcon,
  categoryIcon,
} from '../../components/icons'
import type { Address, GeoArea, ServiceRequest } from '../../types'

const requestSchema = z
  .object({
    categoryId: z.string().min(1, 'Choose a category'),
    serviceId: z.string().min(1, 'Choose a service'),
    description: z.string().trim().min(10, 'Describe the job in at least 10 characters'),
    addressId: z.string().min(1, 'Choose where the work happens'),
    preferredStart: z.string().min(1, 'Pick a start date and time'),
    preferredEnd: z.string().min(1, 'Pick an end date and time'),
    budget: z
      .string()
      .optional()
      .refine(
        (value) => !value || (!Number.isNaN(Number(value)) && Number(value) >= 0),
        'Enter a number of 0 or more',
      ),
  })
  .refine(
    (values) =>
      !values.preferredStart ||
      !values.preferredEnd ||
      new Date(values.preferredEnd).getTime() > new Date(values.preferredStart).getTime(),
    { message: 'The end must be after the start', path: ['preferredEnd'] },
  )

type RequestFormValues = z.infer<typeof requestSchema>

const addressSchema = z.object({
  label: z.string().trim().min(1, 'Give it a name, like “Home”'),
  addressLine: z.string().trim().min(1, 'Street address is required'),
  areaCode: z.string().min(1, 'Choose an area'),
})

type AddressFormValues = z.infer<typeof addressSchema>

const STEPS = [
  { key: 'service', label: 'Service' },
  { key: 'details', label: 'Job' },
  { key: 'where', label: 'Where & when' },
]

const STEP_FIELDS: Array<Array<keyof RequestFormValues>> = [
  ['categoryId', 'serviceId'],
  ['description', 'budget'],
  ['addressId', 'preferredStart', 'preferredEnd'],
]

const STEP_TITLES = [
  { title: 'What do you need done?', copy: 'Pick the category, then the exact service.' },
  { title: 'Describe the job', copy: 'The clearer your description, the sharper the quotes.' },
  {
    title: 'Where and when',
    copy: 'Providers only see requests inside the areas they cover.',
  },
]

const CHOICE =
  'group flex cursor-pointer items-start gap-3 rounded-lg border border-line bg-surface p-4 ' +
  'transition-[border-color,background-color] duration-150 hover:border-brand-300 ' +
  'has-[:checked]:border-brand-500 has-[:checked]:bg-brand-50 ' +
  'has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus'

/** `datetime-local` values are local; the API wants an offset-aware instant. */
function toInstant(value: string): string {
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : parsed.toISOString()
}

function areaName(list: GeoArea[] | undefined, code?: string | null): string {
  if (!code) {
    return ''
  }
  return list?.find((area) => area.code === code)?.name ?? code
}

function SummaryRow({
  icon,
  label,
  children,
}: {
  icon: ReactNode
  label: string
  children: ReactNode
}) {
  return (
    <div className="flex gap-3">
      <span className="mt-0.5 text-ink-muted">{icon}</span>
      <div className="min-w-0 flex-1">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-wider text-ink-muted">
          {label}
        </p>
        <div className="mt-0.5 text-sm text-ink">{children}</div>
      </div>
    </div>
  )
}

export default function NewRequestPage() {
  usePageTitle('Post a request')
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()

  const categories = useCategories()
  const addresses = useAddresses()
  const areas = useAreas()

  const initial = (location.state as { serviceId?: string; categoryId?: string } | null) ?? {}
  const [step, setStep] = useState(0)
  const [showAddressForm, setShowAddressForm] = useState(false)

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    setError,
    setFocus,
    trigger,
    formState: { errors },
  } = useForm<RequestFormValues>({
    resolver: zodResolver(requestSchema),
    defaultValues: {
      categoryId: initial.categoryId ?? '',
      serviceId: initial.serviceId ?? '',
      description: '',
      addressId: '',
      preferredStart: '',
      preferredEnd: '',
      budget: '',
    },
  })

  const addressForm = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: { label: '', addressLine: '', areaCode: '' },
  })

  const categoryId = watch('categoryId')
  const serviceId = watch('serviceId')
  const description = watch('description')
  const budget = watch('budget')
  const addressId = watch('addressId')
  const preferredStart = watch('preferredStart')
  const preferredEnd = watch('preferredEnd')

  const services = useCategoryServices(categoryId || undefined)

  const previousCategory = useRef(categoryId)
  useEffect(() => {
    if (previousCategory.current !== categoryId) {
      setValue('serviceId', '')
      previousCategory.current = categoryId
    }
  }, [categoryId, setValue])

  // Pre-select the default address once the address book arrives.
  const addressList = addresses.data ?? []
  const preselected = useRef(false)
  useEffect(() => {
    if (preselected.current || addressList.length === 0) {
      return
    }
    preselected.current = true
    const preferred = addressList.find((address) => address.isDefault) ?? addressList[0]
    setValue('addressId', preferred.id, { shouldValidate: true })
  }, [addressList, setValue])

  useEffect(() => {
    if (!addresses.isLoading && addressList.length === 0) {
      setShowAddressForm(true)
    }
  }, [addresses.isLoading, addressList.length])

  const createAddress = useMutation({
    mutationFn: (payload: AddressFormValues) =>
      apiClient
        .post<Address>('/me/addresses', { ...payload, isDefault: addressList.length === 0 })
        .then((res) => res.data),
    onSuccess: (address) => {
      void queryClient.invalidateQueries({ queryKey: ['addresses'] })
      setValue('addressId', address.id, { shouldValidate: true })
      addressForm.reset()
      setShowAddressForm(false)
    },
    onError: (error) => applyFieldErrors(error, addressForm.setError),
  })

  const createRequest = useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiClient.post<ServiceRequest>('/service-requests', payload).then((res) => res.data),
    onSuccess: (request) => {
      queryClient.setQueryData(['service-request', request.id], request)
      navigate(`/customer/requests/${request.id}`)
    },
    onError: (error) => applyFieldErrors(error, setError),
  })

  const onSubmit = handleSubmit((values) => {
    createRequest.mutate({
      categoryId: values.categoryId,
      serviceId: values.serviceId,
      description: values.description,
      addressId: values.addressId,
      preferredStart: toInstant(values.preferredStart),
      preferredEnd: toInstant(values.preferredEnd),
      budgetAmount: values.budget ? Number(values.budget) : null,
      budgetCurrency: 'VND',
    })
  })

  const goNext = async () => {
    const fields = STEP_FIELDS[step]
    const valid = await trigger(fields)
    if (!valid) {
      const found = fields.find((field) => Boolean((errors as Record<string, unknown>)[field]))
      if (found) {
        setFocus(found)
      }
      return
    }
    setStep((current) => Math.min(current + 1, STEPS.length - 1))
  }

  const selectedCategory = categories.data?.find((category) => category.id === categoryId)
  const selectedService = services.data?.find((service) => service.id === serviceId)
  const selectedAddress = addressList.find((address) => address.id === addressId)
  const selectedArea = areas.data?.find((area) => area.code === selectedAddress?.areaCode)
  const isLastStep = step === STEPS.length - 1

  return (
    <div className="mx-auto w-full max-w-5xl">
      <header>
        <p className="text-eyebrow uppercase text-brand-700">New request</p>
        <h1 className="mt-2 text-display-sm text-ink">Tell us what you need</h1>
        <p className="mt-2 max-w-2xl text-body text-ink-muted">
          Three short steps. It stays a private draft until you confirm it.
        </p>
      </header>

      <div className="mt-8 rounded-xl border border-line bg-surface px-5 py-6">
        <Stepper steps={STEPS} currentIndex={step} label="Request progress" />
      </div>

      {createRequest.isError && (
        <div className="mt-6">
          <ErrorAlert
            title="We could not create your request"
            message={getErrorMessage(createRequest.error)}
          />
        </div>
      )}

      <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem] lg:items-start">
        <form id="new-request-form" onSubmit={onSubmit} className="space-y-6" noValidate>
          <section className="rounded-xl border border-line bg-surface p-5 sm:p-6">
            <h2 className="text-base font-semibold text-ink">{STEP_TITLES[step].title}</h2>
            <p className="mt-1 text-sm text-ink-muted">{STEP_TITLES[step].copy}</p>

            <div className="mt-6">
              {step === 0 && (
                <div className="space-y-6">
                  {categories.isLoading && (
                    <p role="status" className="text-sm text-ink-muted">
                      Loading categories…
                    </p>
                  )}

                  {categories.isError && (
                    <ErrorAlert
                      title="We could not load categories"
                      message={getErrorMessage(categories.error)}
                      onRetry={() => void categories.refetch()}
                    />
                  )}

                  {(categories.data ?? []).length > 0 && (
                    <fieldset>
                      <legend className="text-sm font-semibold text-ink">Category</legend>
                      <div className="mt-3 grid gap-3 sm:grid-cols-2">
                        {(categories.data ?? []).map((category) => {
                          const Icon = categoryIcon(category.name, category.slug)
                          return (
                            <label key={category.id} className={CHOICE}>
                              <input
                                type="radio"
                                value={category.id}
                                {...register('categoryId')}
                                className="sr-only"
                              />
                              <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-brand-50 text-brand-600 group-has-[:checked]:bg-brand-100">
                                <Icon className="size-4.5" />
                              </span>
                              <span className="min-w-0">
                                <span className="block text-sm font-semibold text-ink">
                                  {category.name}
                                </span>
                                {category.description && (
                                  <span className="mt-0.5 block line-clamp-2 text-[0.8125rem] text-ink-muted">
                                    {category.description}
                                  </span>
                                )}
                              </span>
                            </label>
                          )
                        })}
                      </div>
                      {errors.categoryId && (
                        <p role="alert" className="mt-2 text-[0.8125rem] font-medium text-danger-ink">
                          {errors.categoryId.message}
                        </p>
                      )}
                    </fieldset>
                  )}

                  <fieldset disabled={!categoryId}>
                    <legend className="text-sm font-semibold text-ink">Service</legend>
                    {!categoryId && (
                      <p className="mt-3 text-sm text-ink-muted">
                        Choose a category first, then pick the service.
                      </p>
                    )}

                    {categoryId && services.isLoading && (
                      <p role="status" className="mt-3 text-sm text-ink-muted">
                        Loading services…
                      </p>
                    )}

                    {categoryId && !services.isLoading && (services.data ?? []).length === 0 && (
                      <p className="mt-3 text-sm text-ink-muted">
                        This category has no bookable services yet. Pick another one.
                      </p>
                    )}

                    {categoryId && (services.data ?? []).length > 0 && (
                      <div className="mt-3 space-y-3">
                        {(services.data ?? []).map((service) => (
                          <label key={service.id} className={CHOICE}>
                            <input
                              type="radio"
                              value={service.id}
                              {...register('serviceId')}
                              className="sr-only"
                            />
                            <span className="min-w-0 flex-1">
                              <span className="block text-sm font-semibold text-ink">
                                {service.name}
                              </span>
                              {service.description && (
                                <span className="mt-1 block text-[0.8125rem] text-ink-muted">
                                  {service.description}
                                </span>
                              )}
                            </span>
                          </label>
                        ))}
                      </div>
                    )}

                    {errors.serviceId && (
                      <p role="alert" className="mt-2 text-[0.8125rem] font-medium text-danger-ink">
                        {errors.serviceId.message}
                      </p>
                    )}
                  </fieldset>
                </div>
              )}

              {step === 1 && (
                <div className="space-y-5">
                  <Field
                    label="Description"
                    htmlFor="description"
                    hint="What needs doing, plus anything the provider should bring or know."
                    error={errors.description?.message}
                  >
                    <Textarea
                      id="description"
                      rows={6}
                      placeholder="The kitchen tap drips constantly and the cabinet under the sink stays wet."
                      error={errors.description?.message}
                      hint="What needs doing, plus anything the provider should bring or know."
                      {...register('description')}
                    />
                  </Field>

                  <Field
                    label="Budget"
                    htmlFor="budget"
                    optional
                    hint="A rough figure helps providers send realistic quotes. Leave it blank to stay open."
                    error={errors.budget?.message}
                  >
                    <div className="relative">
                      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm font-semibold text-ink-muted">
                        ₫
                      </span>
                      <Input
                        id="budget"
                        type="text"
                        inputMode="decimal"
                        className="pl-7"
                        placeholder="500000"
                        error={errors.budget?.message}
                        hint="A rough figure helps providers send realistic quotes. Leave it blank to stay open."
                        {...register('budget')}
                      />
                    </div>
                  </Field>
                </div>
              )}

              {step === 2 && (
                <div className="space-y-6">
                  <fieldset>
                    <legend className="text-sm font-semibold text-ink">Address</legend>

                    {addresses.isLoading && (
                      <p role="status" className="mt-3 text-sm text-ink-muted">
                        Loading your addresses…
                      </p>
                    )}

                    {addressList.length > 0 && (
                      <div className="mt-3 space-y-3">
                        {addressList.map((address) => (
                          <label key={address.id} className={CHOICE}>
                            <input
                              type="radio"
                              value={address.id}
                              {...register('addressId')}
                              className="sr-only"
                            />
                            <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-brand-50 text-brand-600 group-has-[:checked]:bg-brand-100">
                              <MapPinIcon className="size-4.5" />
                            </span>
                            <span className="min-w-0 flex-1">
                              <span className="flex flex-wrap items-center gap-2">
                                <span className="text-sm font-semibold text-ink">
                                  {address.label ?? 'Address'}
                                </span>
                                {address.isDefault && <Badge tone="neutral">Default</Badge>}
                              </span>
                              <span className="mt-0.5 block text-[0.8125rem] text-ink-muted">
                                {address.addressLine}
                                {address.areaCode ? ` · ${areaName(areas.data, address.areaCode)}` : ''}
                              </span>
                            </span>
                          </label>
                        ))}
                      </div>
                    )}

                    {errors.addressId && (
                      <p role="alert" className="mt-2 text-[0.8125rem] font-medium text-danger-ink">
                        {errors.addressId.message}
                      </p>
                    )}

                    {addressList.length > 0 && !showAddressForm && (
                      <button
                        type="button"
                        onClick={() => setShowAddressForm(true)}
                        className={buttonClass('secondary', 'sm', 'mt-3')}
                      >
                        <PlusIcon className="size-4" />
                        Add another address
                      </button>
                    )}

                    {showAddressForm && (
                      <div className="mt-4 rounded-lg border border-line bg-surface-2/60 p-4">
                        <p className="text-sm font-semibold text-ink">New address</p>

                        {createAddress.isError && (
                          <div className="mt-3">
                            <ErrorAlert
                              title="We could not save that address"
                              message={getErrorMessage(createAddress.error)}
                            />
                          </div>
                        )}

                        <div className="mt-3 space-y-4">
                          <Field
                            label="Label"
                            htmlFor="address-label"
                            error={addressForm.formState.errors.label?.message}
                          >
                            <Input
                              id="address-label"
                              type="text"
                              placeholder="Home"
                              error={addressForm.formState.errors.label?.message}
                              {...addressForm.register('label')}
                            />
                          </Field>

                          <Field
                            label="Street address"
                            htmlFor="address-line"
                            error={addressForm.formState.errors.addressLine?.message}
                          >
                            <Input
                              id="address-line"
                              type="text"
                              autoComplete="street-address"
                              placeholder="12 Le Loi"
                              error={addressForm.formState.errors.addressLine?.message}
                              {...addressForm.register('addressLine')}
                            />
                          </Field>

                          <Field
                            label="Area"
                            htmlFor="address-area"
                            error={addressForm.formState.errors.areaCode?.message}
                          >
                            <Select
                              id="address-area"
                              error={addressForm.formState.errors.areaCode?.message}
                              {...addressForm.register('areaCode')}
                            >
                              <option value="">Select an area…</option>
                              {(areas.data ?? []).map((area) => (
                                <option key={area.code} value={area.code}>
                                  {area.name}
                                  {area.type === 'district' ? '' : ' (province)'}
                                </option>
                              ))}
                            </Select>
                          </Field>
                        </div>

                        <div className="mt-4 flex gap-2">
                          <button
                            type="button"
                            onClick={addressForm.handleSubmit((values) =>
                              createAddress.mutate(values),
                            )}
                            disabled={createAddress.isPending}
                            aria-busy={createAddress.isPending || undefined}
                            className={buttonClass('primary', 'md')}
                          >
                            {createAddress.isPending ? 'Saving…' : 'Save address'}
                          </button>
                          {addressList.length > 0 && (
                            <button
                              type="button"
                              onClick={() => {
                                addressForm.reset()
                                setShowAddressForm(false)
                              }}
                              className={buttonClass('ghost', 'md')}
                            >
                              Cancel
                            </button>
                          )}
                        </div>
                      </div>
                    )}
                  </fieldset>

                  <div className="grid gap-5 sm:grid-cols-2">
                    <Field
                      label="Earliest start"
                      htmlFor="preferredStart"
                      error={errors.preferredStart?.message}
                    >
                      <Input
                        id="preferredStart"
                        type="datetime-local"
                        error={errors.preferredStart?.message}
                        {...register('preferredStart')}
                      />
                    </Field>

                    <Field
                      label="Latest end"
                      htmlFor="preferredEnd"
                      error={errors.preferredEnd?.message}
                    >
                      <Input
                        id="preferredEnd"
                        type="datetime-local"
                        error={errors.preferredEnd?.message}
                        {...register('preferredEnd')}
                      />
                    </Field>
                  </div>

                  <p className="flex items-start gap-2 rounded-lg bg-accent-50 px-3 py-2.5 text-[0.8125rem] leading-relaxed text-ink-muted">
                    <ClockIcon className="mt-0.5 size-4 shrink-0 text-accent-600" />
                    Give a window, not an exact minute. Providers book a slot inside it.
                  </p>
                </div>
              )}
            </div>
          </section>
        </form>

        <aside className="space-y-4 lg:sticky lg:top-24">
          <section className="rounded-xl border border-line bg-surface p-5">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-eyebrow uppercase text-ink-muted">Your request</h2>
              <span className="text-[0.8125rem] text-ink-muted tabular-nums">
                Step {step + 1} of {STEPS.length}
              </span>
            </div>

            <div className="mt-4 space-y-4 border-t border-line pt-4">
              <SummaryRow icon={<CheckIcon className="size-4" />} label="Service">
                {selectedService ? (
                  selectedService.name
                ) : selectedCategory ? (
                  <span className="text-ink-muted">{selectedCategory.name} · pick a service</span>
                ) : (
                  <span className="text-ink-muted">Not chosen yet</span>
                )}
              </SummaryRow>

              <SummaryRow icon={<WalletIcon className="size-4" />} label="Budget">
                {budget ? (
                  <span className="tabular-nums">
                    {new Intl.NumberFormat(undefined, {
                      style: 'currency',
                      currency: 'VND',
                      maximumFractionDigits: 0,
                    }).format(Number(budget) || 0)}
                  </span>
                ) : (
                  <span className="text-ink-muted">Open to quotes</span>
                )}
              </SummaryRow>

              <SummaryRow icon={<CalendarIcon className="size-4" />} label="When">
                {preferredStart || preferredEnd ? (
                  <span className="tabular-nums">{formatWindow(preferredStart, preferredEnd)}</span>
                ) : (
                  <span className="text-ink-muted">Not set yet</span>
                )}
              </SummaryRow>

              <SummaryRow icon={<MapPinIcon className="size-4" />} label="Where">
                {selectedAddress ? (
                  <span>
                    {selectedAddress.label ? `${selectedAddress.label} · ` : ''}
                    {selectedAddress.addressLine}
                    {selectedArea ? `, ${selectedArea.name}` : ''}
                  </span>
                ) : (
                  <span className="text-ink-muted">Not set yet</span>
                )}
              </SummaryRow>

              {description && (
                <SummaryRow icon={<CheckIcon className="size-4" />} label="Description">
                  <span className="line-clamp-3">{description}</span>
                </SummaryRow>
              )}
            </div>
          </section>

          <section className="rounded-xl border border-line bg-surface p-5">
            <div className="flex flex-col gap-2">
              {isLastStep ? (
                <button
                  type="submit"
                  form="new-request-form"
                  disabled={createRequest.isPending}
                  aria-busy={createRequest.isPending || undefined}
                  className={buttonClass('primary', 'lg', 'w-full')}
                >
                  {createRequest.isPending ? (
                    <span
                      className="size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
                      aria-hidden="true"
                    />
                  ) : (
                    <CheckIcon className="size-4" />
                  )}
                  {createRequest.isPending ? 'Saving draft…' : 'Save draft'}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => void goNext()}
                  className={buttonClass('primary', 'lg', 'w-full')}
                >
                  Continue
                  <ArrowRightIcon className="size-4" />
                </button>
              )}

              <button
                type="button"
                onClick={() => setStep((current) => Math.max(current - 1, 0))}
                disabled={step === 0}
                className={buttonClass('ghost', 'md', 'w-full')}
              >
                <ArrowLeftIcon className="size-4" />
                Back
              </button>
            </div>

            <p className="mt-4 text-[0.8125rem] leading-relaxed text-ink-muted">
              This saves a private draft. You confirm it on the next screen before providers can
              see it.
            </p>
          </section>
        </aside>
      </div>
    </div>
  )
}

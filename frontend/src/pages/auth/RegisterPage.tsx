import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/auth'
import { getErrorMessage } from '../../lib/errors'
import { usePageTitle } from '../../lib/usePageTitle'
import { Field, Input } from '../../components/ui/Field'
import Button from '../../components/ui/Button'
import Wordmark from '../../components/Wordmark'
import ErrorAlert from '../../components/ErrorAlert'
import { GearIcon, UserIcon } from '../../components/icons'

const registerSchema = z
  .object({
    displayName: z.string().trim().min(1, 'Full name is required'),
    email: z.string().email('Enter a valid email address'),
    password: z.string().min(6, 'Password must be at least 6 characters'),
    confirmPassword: z.string().min(1, 'Confirm your password'),
    role: z.enum(['CUSTOMER', 'PROVIDER']),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type RegisterFormValues = z.infer<typeof registerSchema>

const ROLE_CHOICE =
  'group flex cursor-pointer items-start gap-3 rounded-lg border border-line bg-surface p-4 ' +
  'transition-[border-color,background-color] duration-150 hover:border-brand-300 ' +
  'has-[:checked]:border-brand-500 has-[:checked]:bg-brand-50 ' +
  'has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus'

const ROLE_OPTIONS = [
  {
    value: 'CUSTOMER' as const,
    label: 'I need a service',
    copy: 'Post jobs and compare providers.',
    Icon: UserIcon,
  },
  {
    value: 'PROVIDER' as const,
    label: 'I provide services',
    copy: 'Quote on jobs in your area.',
    Icon: GearIcon,
  },
]

export default function RegisterPage() {
  usePageTitle('Create an account')
  const registerUser = useAuthStore((state) => state.register)
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      displayName: '',
      email: '',
      password: '',
      confirmPassword: '',
      role: 'CUSTOMER',
    },
  })

  const mutation = useMutation({
    mutationFn: registerUser,
    onSuccess: () => navigate('/login', { replace: true }),
  })

  const onSubmit = handleSubmit((values) => {
    mutation.mutate({
      displayName: values.displayName,
      email: values.email,
      password: values.password,
      role: values.role,
    })
  })

  return (
    <div className="mx-auto w-full max-w-md py-4">
      <Wordmark />

      <h1 className="mt-7 text-display-sm text-ink">Create your account</h1>
      <p className="mt-2 text-body text-ink-muted">
        One account for posting jobs, or for quoting on them.
      </p>

      <div className="mt-7 rounded-xl border border-line bg-surface p-6">
        {mutation.isError && (
          <div className="mb-5">
            <ErrorAlert
              title="We could not create your account"
              message={getErrorMessage(mutation.error)}
            />
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-5" noValidate>
          <fieldset>
            <legend className="text-sm font-semibold text-ink">I am joining as</legend>
            <div className="mt-2 grid gap-3">
              {ROLE_OPTIONS.map(({ value, label, copy, Icon }) => (
                <label key={value} className={ROLE_CHOICE}>
                  <input type="radio" value={value} {...register('role')} className="sr-only" />
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-brand-50 text-brand-600 group-has-[:checked]:bg-brand-100">
                    <Icon className="size-4.5" />
                  </span>
                  <span className="min-w-0">
                    <span className="block text-sm font-semibold text-ink">{label}</span>
                    <span className="mt-0.5 block text-[0.8125rem] text-ink-muted">{copy}</span>
                  </span>
                </label>
              ))}
            </div>
            {errors.role && (
              <p role="alert" className="mt-2 text-[0.8125rem] font-medium text-danger-ink">
                {errors.role.message}
              </p>
            )}
          </fieldset>

          <Field label="Full name" htmlFor="displayName" error={errors.displayName?.message}>
            <Input
              id="displayName"
              type="text"
              autoComplete="name"
              error={errors.displayName?.message}
              {...register('displayName')}
            />
          </Field>

          <Field label="Email" htmlFor="email" error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              spellCheck={false}
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register('email')}
            />
          </Field>

          <Field
            label="Password"
            htmlFor="password"
            hint="At least 6 characters."
            error={errors.password?.message}
          >
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              error={errors.password?.message}
              hint="At least 6 characters."
              {...register('password')}
            />
          </Field>

          <Field
            label="Confirm password"
            htmlFor="confirmPassword"
            error={errors.confirmPassword?.message}
          >
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
          </Field>

          <Button type="submit" size="lg" loading={mutation.isPending} className="w-full">
            {mutation.isPending ? 'Creating account…' : 'Create account'}
          </Button>
        </form>
      </div>

      <p className="mt-6 text-sm text-ink-muted">
        Already have an account?{' '}
        <Link
          to="/login"
          className="font-semibold text-brand-700 underline decoration-brand-200 underline-offset-4 transition-colors hover:decoration-brand-500"
        >
          Log in
        </Link>
      </p>
    </div>
  )
}

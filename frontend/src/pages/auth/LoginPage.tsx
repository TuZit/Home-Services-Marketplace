import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/auth'
import { getErrorMessage } from '../../lib/errors'
import { usePageTitle } from '../../lib/usePageTitle'
import { Field, Input } from '../../components/ui/Field'
import Button from '../../components/ui/Button'
import Wordmark from '../../components/Wordmark'
import ErrorAlert from '../../components/ErrorAlert'

const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export default function LoginPage() {
  usePageTitle('Log in')
  const login = useAuthStore((state) => state.login)
  const navigate = useNavigate()
  const location = useLocation()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: () => {
      const from = (location.state as { from?: string } | null)?.from ?? '/catalogue'
      navigate(from, { replace: true })
    },
  })

  const onSubmit = handleSubmit((values) => mutation.mutate(values))

  return (
    <div className="mx-auto w-full max-w-md py-4">
      <Wordmark />

      <h1 className="mt-7 text-display-sm text-ink">Welcome back</h1>
      <p className="mt-2 text-body text-ink-muted">
        Log in to track your requests, quotes, and bookings.
      </p>

      <div className="mt-7 rounded-xl border border-line bg-surface p-6">
        {mutation.isError && (
          <div className="mb-5">
            <ErrorAlert title="We could not sign you in" message={getErrorMessage(mutation.error)} />
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-5" noValidate>
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

          <Field label="Password" htmlFor="password" error={errors.password?.message}>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />
          </Field>

          <Button type="submit" size="lg" loading={mutation.isPending} className="w-full">
            {mutation.isPending ? 'Logging in…' : 'Log in'}
          </Button>
        </form>
      </div>

      <p className="mt-6 text-sm text-ink-muted">
        New to HomeServices?{' '}
        <Link
          to="/register"
          className="font-semibold text-brand-700 underline decoration-brand-200 underline-offset-4 transition-colors hover:decoration-brand-500"
        >
          Create an account
        </Link>
      </p>
    </div>
  )
}

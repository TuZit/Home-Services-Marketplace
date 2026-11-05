import { useAdminDashboard } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import { formatMoney } from '../../lib/format'

export default function AdminDashboardPage() {
  const dashboard = useAdminDashboard()

  if (dashboard.isLoading) {
    return <Spinner />
  }

  if (dashboard.isError) {
    return <ErrorAlert message={getErrorMessage(dashboard.error)} />
  }

  const data = dashboard.data

  const stats = [
    { label: 'Total users', value: data?.totalUsers },
    { label: 'Providers', value: data?.totalProviders },
    { label: 'Service requests', value: data?.totalRequests },
    { label: 'Bookings', value: data?.totalBookings },
    { label: 'Pending verifications', value: data?.pendingVerifications },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold">Admin dashboard</h1>
      <p className="mt-1 text-sm text-slate-500">Platform overview and key metrics.</p>

      <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((stat) => (
          <div key={stat.label} className="rounded-xl border border-slate-200 bg-white p-5">
            <p className="text-sm font-medium text-slate-400">{stat.label}</p>
            <p className="mt-2 text-3xl font-bold">
              {typeof stat.value === 'number' ? stat.value : '—'}
            </p>
          </div>
        ))}

        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <p className="text-sm font-medium text-slate-400">Revenue</p>
          <p className="mt-2 text-3xl font-bold">{formatMoney(data?.revenue)}</p>
        </div>
      </div>

      {data?.recentBookings && data.recentBookings.length > 0 && (
        <section className="mt-8 rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="text-lg font-semibold">Recent bookings</h2>
          <div className="mt-4 divide-y divide-slate-100">
            {data.recentBookings.map((booking) => (
              <div key={booking.id} className="flex items-center justify-between py-3 text-sm">
                <span className="text-slate-600">{booking.id}</span>
                <span className="text-slate-500">{booking.status}</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

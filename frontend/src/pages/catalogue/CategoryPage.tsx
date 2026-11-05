import { Link, useParams } from 'react-router-dom'
import { useCategories, useCategoryServices } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'
import { formatMoney } from '../../lib/format'

export default function CategoryPage() {
  const { categoryId } = useParams<{ categoryId: string }>()
  const categories = useCategories()
  const services = useCategoryServices(categoryId)

  const category = categories.data?.find((item) => item.id === categoryId)

  if (services.isLoading) {
    return <Spinner />
  }

  if (services.isError) {
    return <ErrorAlert message={getErrorMessage(services.error)} />
  }

  const items = services.data ?? []

  return (
    <div>
      <div className="flex items-center gap-3">
        <Link to="/catalogue" className="text-sm text-slate-500 underline">
          All categories
        </Link>
        <span className="text-slate-300">/</span>
        <h1 className="text-2xl font-bold">{category?.name ?? 'Services'}</h1>
      </div>

      {category?.description && <p className="mt-2 text-sm text-slate-500">{category.description}</p>}

      {items.length === 0 ? (
        <p className="mt-8 text-sm text-slate-500">No active services in this category yet.</p>
      ) : (
        <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((service) => (
            <div
              key={service.id}
              className="flex flex-col rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
            >
              <div className="flex items-start justify-between gap-3">
                <h2 className="text-lg font-semibold">{service.name}</h2>
                {service.price && (
                  <span className="whitespace-nowrap text-sm font-semibold text-slate-900">
                    {formatMoney(service.price)}
                  </span>
                )}
              </div>
              {service.description && (
                <p className="mt-2 flex-1 text-sm text-slate-500">{service.description}</p>
              )}
              {typeof service.durationMinutes === 'number' && service.durationMinutes > 0 && (
                <p className="mt-3 text-xs text-slate-400">~{service.durationMinutes} min</p>
              )}
              <Link
                to="/customer/requests/new"
                state={{ serviceId: service.id, categoryId: service.categoryId }}
                className="mt-4 rounded-md bg-slate-900 px-3 py-2 text-center text-sm font-medium text-white hover:bg-slate-700"
              >
                Request this service
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

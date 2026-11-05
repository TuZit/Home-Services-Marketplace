import { Link } from 'react-router-dom'
import { useCategories } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'

export default function CataloguePage() {
  const categories = useCategories()

  if (categories.isLoading) {
    return <Spinner />
  }

  if (categories.isError) {
    return <ErrorAlert message={getErrorMessage(categories.error)} />
  }

  const items = categories.data ?? []

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Service catalogue</h1>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        Browse service categories and find a trusted provider near you.
      </p>

      {items.length === 0 ? (
        <p className="mt-8 text-sm text-slate-500">No service categories are available yet.</p>
      ) : (
        <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((category) => (
            <Link
              key={category.id}
              to={`/catalogue/${category.id}`}
              className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md"
            >
              <h2 className="text-lg font-semibold">{category.name}</h2>
              {category.description && (
                <p className="mt-2 line-clamp-3 text-sm text-slate-500">{category.description}</p>
              )}
              {typeof category.serviceCount === 'number' && (
                <p className="mt-4 text-xs font-medium text-slate-400">
                  {category.serviceCount} service{category.serviceCount === 1 ? '' : 's'}
                </p>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

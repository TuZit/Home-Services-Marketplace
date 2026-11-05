import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useCategories, useCategoryServices } from '../../api/hooks'
import { usePageTitle } from '../../lib/usePageTitle'
import { getErrorMessage } from '../../lib/errors'
import { buttonClass } from '../../components/ui/Button'
import Badge from '../../components/ui/Badge'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorAlert from '../../components/ErrorAlert'
import {
  ArrowLeftIcon,
  ChevronRightIcon,
  SearchIcon,
  categoryIcon,
} from '../../components/icons'
import type { Service } from '../../types'

type SortKey = 'featured' | 'name'

const SORT_LABEL: Record<SortKey, string> = {
  featured: 'Suggested order',
  name: 'Name A–Z',
}

function sortServices(services: Service[], key: SortKey): Service[] {
  if (key === 'featured') {
    return services
  }
  return [...services].sort((left, right) => left.name.localeCompare(right.name))
}

export default function CategoryPage() {
  const { categoryId } = useParams<{ categoryId: string }>()
  const categories = useCategories()
  const services = useCategoryServices(categoryId)

  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<SortKey>('featured')

  const category = categories.data?.find((item) => item.id === categoryId)
  usePageTitle(category?.name ?? 'Services')

  const items = services.data ?? []

  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    const matched = needle
      ? items.filter((service) =>
          `${service.name} ${service.description ?? ''}`.toLowerCase().includes(needle),
        )
      : items
    return sortServices(matched, sort)
  }, [items, query, sort])

  const CategoryGlyph = categoryIcon(category?.name, category?.slug)

  return (
    <div>
      <nav aria-label="Breadcrumb">
        <ol className="flex flex-wrap items-center gap-1.5 text-[0.8125rem] text-ink-muted">
          <li>
            <Link
              to="/catalogue"
              className="inline-flex items-center gap-1.5 rounded-md font-medium transition-colors hover:text-ink"
            >
              <ArrowLeftIcon className="size-3.5" />
              All categories
            </Link>
          </li>
          <li aria-hidden="true" className="text-line-strong">
            /
          </li>
          <li aria-current="page" className="font-semibold text-ink">
            {category?.name ?? 'Services'}
          </li>
        </ol>
      </nav>

      <header className="mt-5 flex items-start gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
          <CategoryGlyph className="size-6" />
        </span>
        <div className="min-w-0">
          <h1 className="text-display-sm text-ink">{category?.name ?? 'Services'}</h1>
          {category?.description && (
            <p className="mt-2 max-w-2xl text-body text-ink-muted">{category.description}</p>
          )}
        </div>
      </header>

      {!services.isLoading && !services.isError && items.length > 0 && (
        <div className="mt-8 flex flex-col gap-3 border-y border-line py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative sm:max-w-xs sm:flex-1">
            <label htmlFor="service-search" className="sr-only">
              Search these services
            </label>
            <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-faint" />
            <input
              id="service-search"
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search these services"
              className="h-10 w-full rounded-md border border-line-strong bg-surface pl-9 pr-3 text-base text-ink transition-colors placeholder:text-ink-muted hover:border-brand-300 focus:border-brand-400 sm:text-sm"
            />
          </div>

          <div className="flex items-center gap-3">
            <p role="status" className="text-[0.8125rem] text-ink-muted">
              {visible.length} of {items.length}
            </p>
            <label htmlFor="service-sort" className="sr-only">
              Sort services
            </label>
            <select
              id="service-sort"
              value={sort}
              onChange={(event) => setSort(event.target.value as SortKey)}
              className="h-10 rounded-md border border-line-strong bg-surface px-2.5 text-base text-ink transition-colors hover:border-brand-300 focus:border-brand-400 sm:text-sm"
            >
              {(Object.keys(SORT_LABEL) as SortKey[]).map((key) => (
                <option key={key} value={key}>
                  {SORT_LABEL[key]}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      <div className="mt-6">
        {services.isLoading && (
          <div className="divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface">
            {[0, 1, 2].map((index) => (
              <div key={index} className="grid gap-4 p-5 sm:grid-cols-[1fr_auto] sm:items-center">
                <div>
                  <Skeleton className="h-5 w-48" />
                  <Skeleton className="mt-3 h-4 w-full max-w-md" />
                  <Skeleton className="mt-3 h-6 w-24 rounded-full" />
                </div>
                <Skeleton className="h-14 w-32 rounded-lg" />
              </div>
            ))}
          </div>
        )}

        {services.isError && (
          <ErrorAlert
            title="We could not load these services"
            message={getErrorMessage(services.error)}
            onRetry={() => void services.refetch()}
          />
        )}

        {!services.isLoading && !services.isError && items.length === 0 && (
          <EmptyState
            icon={<CategoryGlyph className="size-6" />}
            title="No services here yet"
            description="This category has no bookable services yet. Tell us what you need and we will find someone."
            action={
              <Link to="/customer/requests/new" className={buttonClass('primary', 'md')}>
                Post a request
              </Link>
            }
          />
        )}

        {!services.isLoading && !services.isError && items.length > 0 && visible.length === 0 && (
          <EmptyState
            icon={<SearchIcon className="size-6" />}
            title="Nothing matches that search"
            description="Try another word, or clear the filter to see every service in this category."
            action={
              <button
                type="button"
                onClick={() => setQuery('')}
                className={buttonClass('secondary', 'md')}
              >
                Clear search
              </button>
            }
          />
        )}

        {visible.length > 0 && (
          <ul className="divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface">
            {visible.map((service, index) => {
              return (
                <li
                  key={service.id}
                  className="animate-rise grid gap-4 p-5 transition-colors duration-150 hover:bg-surface-2/60 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:gap-8"
                  style={{ animationDelay: `${Math.min(index, 8) * 26}ms` }}
                >
                  <div className="min-w-0">
                    <h2 className="text-base font-semibold text-ink">{service.name}</h2>
                    {service.description && (
                      <p className="mt-1.5 max-w-prose text-sm leading-relaxed text-ink-muted">
                        {service.description}
                      </p>
                    )}
                    {!service.active && (
                      <span className="mt-3 inline-block">
                        <Badge tone="warning">Not bookable</Badge>
                      </span>
                    )}
                  </div>

                  <div className="flex items-center justify-between gap-5 sm:flex-col sm:items-end sm:gap-2">
                    <p className="text-[0.8125rem] text-ink-muted">Quotes from providers</p>
                    <Link
                      to="/customer/requests/new"
                      state={{ serviceId: service.id, categoryId: service.categoryId }}
                      className={buttonClass('primary', 'md')}
                    >
                      Request
                      <ChevronRightIcon className="size-4" />
                    </Link>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}

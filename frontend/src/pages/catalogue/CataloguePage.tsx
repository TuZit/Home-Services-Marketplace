import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCategories } from '../../api/hooks'
import { usePageTitle } from '../../lib/usePageTitle'
import { getErrorMessage } from '../../lib/errors'
import { buttonClass } from '../../components/ui/Button'
import Skeleton from '../../components/ui/Skeleton'
import EmptyState from '../../components/ui/EmptyState'
import ErrorAlert from '../../components/ErrorAlert'
import {
  ArrowRightIcon,
  CalendarIcon,
  ChevronRightIcon,
  SearchIcon,
  ShieldCheckIcon,
  SlidersIcon,
  WalletIcon,
  categoryIcon,
} from '../../components/icons'

const TRUST = [
  {
    icon: ShieldCheckIcon,
    title: 'Vetted providers',
    copy: 'Every professional is verified before they can quote on your job.',
  },
  {
    icon: WalletIcon,
    title: 'Quotes you can compare',
    copy: 'Compare who covers your area and when they are free, then choose.',
  },
  {
    icon: CalendarIcon,
    title: 'Booking on your terms',
    copy: 'Pick the day and time slot that works, and track it to completion.',
  },
]

const STEPS = [
  {
    title: 'Describe the job',
    copy: 'Tell us what needs doing, where, and roughly when. It takes about a minute.',
  },
  {
    title: 'Compare providers',
    copy: 'We surface the providers who cover your service and area, and the hours they work.',
  },
  {
    title: 'Book and relax',
    copy: 'Pick your provider, confirm the slot, and follow the job to the end.',
  },
]

export default function CataloguePage() {
  usePageTitle('Find a home service')
  const categories = useCategories()
  const [query, setQuery] = useState('')

  const items = categories.data ?? []
  const needle = query.trim().toLowerCase()
  const filtered = needle
    ? items.filter((category) =>
        `${category.name} ${category.slug ?? ''} ${category.description ?? ''}`
          .toLowerCase()
          .includes(needle),
      )
    : items

  const shortcuts = items.slice(0, 5)

  return (
    <div>
      <section className="hero-texture relative overflow-hidden rounded-2xl border border-line bg-surface px-6 py-10 sm:px-10 sm:py-12">
        <div className="relative grid gap-10 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)] lg:items-center">
          <div>
            <p className="text-eyebrow uppercase text-brand-700">Home services, handled</p>
            <h1 className="mt-3 text-display-sm text-ink sm:text-display">
              Something broken? Get it sorted by someone you can trust.
            </h1>
            <p className="mt-4 max-w-xl text-body text-ink-muted">
              Describe the job once. Compare local providers on coverage and availability, then
              book the one that fits your schedule.
            </p>

            <div className="relative mt-7 max-w-md">
              <label htmlFor="category-search" className="sr-only">
                Search service categories
              </label>
              <SearchIcon className="pointer-events-none absolute left-3.5 top-1/2 size-5 -translate-y-1/2 text-ink-faint" />
              <input
                id="category-search"
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Try “cleaning”, “plumbing”, or “moving”"
                className="h-12 w-full rounded-lg border border-line-strong bg-surface pl-11 pr-4 text-base text-ink transition-colors placeholder:text-ink-muted hover:border-brand-300 focus:border-brand-400 sm:text-[0.9375rem]"
              />
            </div>

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <Link to="/customer/requests/new" className={buttonClass('primary', 'lg')}>
                Post a request
                <ArrowRightIcon className="size-4" />
              </Link>
              <a href="#how-it-works" className={buttonClass('secondary', 'lg')}>
                See how it works
              </a>
            </div>

            {shortcuts.length > 0 && (
              <div className="mt-8 flex flex-wrap items-center gap-2">
                <span className="text-[0.8125rem] font-medium text-ink-muted">Popular:</span>
                {shortcuts.map((category) => {
                  const Icon = categoryIcon(category.name, category.slug)
                  return (
                    <Link
                      key={category.id}
                      to={`/catalogue/${category.id}`}
                      className="inline-flex items-center gap-1.5 rounded-full border border-line-strong bg-surface px-3 py-1.5 text-[0.8125rem] font-medium text-ink-muted transition-colors duration-150 hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700"
                    >
                      <Icon className="size-3.5" />
                      {category.name}
                    </Link>
                  )
                })}
              </div>
            )}
          </div>

          <ul className="grid gap-3 sm:grid-cols-3 lg:grid-cols-1">
            {TRUST.map(({ icon: Icon, title, copy }) => (
              <li
                key={title}
                className="flex gap-3.5 rounded-xl border border-line/80 bg-surface/75 p-4 backdrop-blur-[2px]"
              >
                <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-accent-100 text-accent-700">
                  <Icon className="size-5" />
                </span>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-ink">{title}</p>
                  <p className="mt-0.5 text-[0.8125rem] leading-relaxed text-ink-muted">{copy}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section id="how-it-works" className="mt-16">
        <p className="text-eyebrow uppercase text-brand-700">How it works</p>
        <h2 className="mt-2 text-display-sm text-ink">Three steps, no phone calls</h2>

        <ol className="mt-8 grid gap-x-8 gap-y-8 sm:grid-cols-3">
          {STEPS.map((step, index) => (
            <li key={step.title} className="border-t border-line pt-5">
              <span
                className="text-[0.8125rem] font-bold tabular-nums text-accent-600"
                aria-hidden="true"
              >
                {String(index + 1).padStart(2, '0')}
              </span>
              <h3 className="mt-2 text-base font-semibold text-ink">{step.title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-ink-muted">{step.copy}</p>
            </li>
          ))}
        </ol>
      </section>

      <section id="categories" className="mt-16">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-eyebrow uppercase text-brand-700">Browse</p>
            <h2 className="mt-2 text-display-sm text-ink">
              {needle ? `Matching “${query.trim()}”` : 'All service categories'}
            </h2>
          </div>
          {!categories.isLoading && !categories.isError && (
            <p role="status" className="text-[0.8125rem] font-medium text-ink-muted">
              {filtered.length} {filtered.length === 1 ? 'category' : 'categories'}
            </p>
          )}
        </div>

        <div className="mt-6">
          {categories.isLoading && (
            <div className="grid gap-4 sm:grid-cols-[repeat(auto-fit,minmax(16rem,1fr))]">
              {[0, 1, 2, 3, 4, 5].map((index) => (
                <div key={index} className="rounded-xl border border-line bg-surface p-5">
                  <Skeleton className="size-11 rounded-lg" />
                  <Skeleton className="mt-4 h-5 w-2/3" />
                  <Skeleton className="mt-3 h-4 w-full" />
                  <Skeleton className="mt-2 h-4 w-4/5" />
                </div>
              ))}
            </div>
          )}

          {categories.isError && (
            <ErrorAlert
              title="We could not load the catalogue"
              message={getErrorMessage(categories.error)}
              onRetry={() => void categories.refetch()}
            />
          )}

          {!categories.isLoading && !categories.isError && filtered.length === 0 && (
            <EmptyState
              icon={needle ? <SearchIcon className="size-6" /> : <SlidersIcon className="size-6" />}
              title={needle ? `No categories match “${query.trim()}”` : 'No categories yet'}
              description={
                needle
                  ? 'Try a shorter word, or browse everything below.'
                  : 'Providers are still being onboarded. Post a request and we will match you manually.'
              }
              action={
                needle ? (
                  <button
                    type="button"
                    onClick={() => setQuery('')}
                    className={buttonClass('secondary', 'md')}
                  >
                    Clear search
                  </button>
                ) : (
                  <Link to="/customer/requests/new" className={buttonClass('primary', 'md')}>
                    Post a request
                  </Link>
                )
              }
            />
          )}

          {filtered.length > 0 && (
            <ul className="grid gap-4 sm:grid-cols-[repeat(auto-fit,minmax(16rem,1fr))]">
              {filtered.map((category, index) => {
                const Icon = categoryIcon(category.name, category.slug)
                return (
                  <li
                    key={category.id}
                    className="animate-rise"
                    style={{ animationDelay: `${Math.min(index, 8) * 26}ms` }}
                  >
                    <Link
                      to={`/catalogue/${category.id}`}
                      className="group flex h-full flex-col rounded-xl border border-line bg-surface p-5 transition-[transform,box-shadow,border-color] duration-200 ease-out hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-lift"
                    >
                      <span className="flex size-11 items-center justify-center rounded-lg bg-brand-50 text-brand-600 transition-colors duration-200 group-hover:bg-brand-100">
                        <Icon className="size-5" />
                      </span>
                      <h3 className="mt-4 text-base font-semibold text-ink">{category.name}</h3>
                      {category.description && (
                        <p className="mt-1.5 line-clamp-2 text-sm leading-relaxed text-ink-muted">
                          {category.description}
                        </p>
                      )}
                      <span className="mt-4 inline-flex items-center gap-1 pt-1 text-[0.8125rem] font-semibold text-brand-700">
                        View services
                        <ChevronRightIcon className="size-3.5 transition-transform duration-200 group-hover:translate-x-0.5" />
                      </span>
                    </Link>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </section>
    </div>
  )
}

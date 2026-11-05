import { Link } from 'react-router-dom'
import { HomeIcon, ShieldCheckIcon } from './icons'

const LINK =
  'focus-on-dark rounded-md text-brand-200 transition-colors duration-150 hover:text-brand-50'

const LINK_LIST = [
  { to: '/catalogue', label: 'Browse services' },
  { to: '/customer/requests/new', label: 'Post a request' },
]

const ACCOUNT_LINKS = [
  { to: '/login', label: 'Log in' },
  { to: '/register', label: 'Create an account' },
]

export default function SiteFooter() {
  return (
    <footer className="mt-20 bg-brand-900 text-brand-100">
      <div className="mx-auto grid max-w-7xl gap-10 px-4 py-14 sm:px-6 lg:grid-cols-[1.6fr_1fr_1fr] lg:px-8">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="flex size-9 items-center justify-center rounded-lg bg-brand-50 text-brand-800">
              <HomeIcon className="size-5" />
            </span>
            <span translate="no" className="text-[1.0625rem] font-bold tracking-[-0.02em] text-brand-50">
              HomeServices
            </span>
          </div>
          <p className="mt-4 max-w-sm text-sm leading-relaxed text-brand-200">
            Describe the job, compare verified local providers, and book the one you trust.
          </p>
          <p className="mt-5 inline-flex items-center gap-2 text-[0.8125rem] font-medium text-brand-200">
            <ShieldCheckIcon className="size-4 text-accent-400" />
            Every provider is vetted before they can quote.
          </p>
        </div>

        <nav aria-label="Marketplace">
          <h2 className="text-eyebrow text-brand-100">Marketplace</h2>
          <ul className="mt-4 space-y-3 text-sm leading-relaxed">
            {LINK_LIST.map((item) => (
              <li key={item.to}>
                <Link to={item.to} className={LINK}>
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <nav aria-label="Account">
          <h2 className="text-eyebrow text-brand-100">Account</h2>
          <ul className="mt-4 space-y-3 text-sm leading-relaxed">
            {ACCOUNT_LINKS.map((item) => (
              <li key={item.to}>
                <Link to={item.to} className={LINK}>
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </div>

      <div className="border-t border-brand-800">
        <p className="mx-auto max-w-7xl px-4 py-6 text-[0.8125rem] text-brand-200 sm:px-6 lg:px-8">
          HomeServices marketplace. Clearly priced work, booked with confidence.
        </p>
      </div>
    </footer>
  )
}

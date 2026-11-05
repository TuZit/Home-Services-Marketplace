import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'
import { useCustomerProfile } from '../api/hooks'
import type { Role } from '../types'
import { buttonClass } from './ui/Button'
import Badge from './ui/Badge'
import Avatar from './ui/Avatar'
import Wordmark from './Wordmark'
import SiteFooter from './SiteFooter'
import { LogOutIcon, MenuIcon, XIcon } from './icons'

interface NavItem {
  to: string
  label: string
}

const ROLE_LABEL: Record<Role, string> = {
  CUSTOMER: 'Customer',
  PROVIDER: 'Provider',
  ADMIN: 'Admin',
}

function navItemsFor(role?: Role): NavItem[] {
  if (role === 'ADMIN') {
    return [
      { to: '/admin/dashboard', label: 'Dashboard' },
      { to: '/admin/providers', label: 'Providers' },
      { to: '/admin/catalog', label: 'Catalogue' },
      { to: '/admin/reviews', label: 'Reviews' },
    ]
  }
  if (role === 'PROVIDER') {
    return [
      { to: '/catalogue', label: 'Catalogue' },
      { to: '/provider/profile', label: 'My profile' },
    ]
  }
  return [{ to: '/catalogue', label: 'Catalogue' }]
}

const linkClass = ({ isActive }: { isActive: boolean }) =>
  [
    'rounded-md px-3 py-2 text-sm font-medium transition-colors duration-150',
    isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-muted hover:bg-surface-2 hover:text-ink',
  ].join(' ')

export default function Layout() {
  const user = useAuthStore((state) => state.user)
  const accessToken = useAuthStore((state) => state.accessToken)
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()
  const location = useLocation()

  const [menuOpen, setMenuOpen] = useState(false)
  const mainRef = useRef<HTMLElement>(null)
  const previousPath = useRef(location.pathname)

  // A client-side route change tells assistive tech nothing on its own.
  useEffect(() => {
    if (previousPath.current === location.pathname) {
      return
    }
    previousPath.current = location.pathname
    setMenuOpen(false)
    mainRef.current?.focus()
    window.scrollTo({ top: 0, behavior: 'auto' })
  }, [location.pathname])

  useEffect(() => {
    if (!menuOpen) {
      return
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false)
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [menuOpen])

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const navItems = navItemsFor(user?.role)
  const canPostRequest = !user || user.role === 'CUSTOMER'
  const profile = useCustomerProfile(Boolean(accessToken) && user?.role === 'CUSTOMER')
  const displayName = profile.data?.fullName ?? user?.fullName ?? user?.email ?? ''

  return (
    <div className="flex min-h-screen flex-col bg-canvas">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-brand-600 focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-brand-50"
      >
        Skip to content
      </a>

      <header className="sticky top-0 z-40 border-b border-line bg-canvas/85 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4 sm:px-6 lg:px-8">
          <Link to="/catalogue" aria-label="HomeServices — go to the service catalogue">
            <Wordmark />
          </Link>

          <nav aria-label="Primary" className="hidden items-center gap-1 md:flex">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} className={linkClass}>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto hidden items-center gap-2 md:flex">
            {canPostRequest && (
              <Link to="/customer/requests/new" className={buttonClass('primary', 'sm')}>
                Post a request
              </Link>
            )}

            {accessToken && user ? (
              <>
                <span className="flex items-center gap-2.5 pl-1">
                  <Avatar name={displayName} size="sm" />
                  <span className="max-w-[9rem] truncate text-sm font-semibold text-ink">
                    {displayName}
                  </span>
                  <Badge tone="neutral">{ROLE_LABEL[user.role]}</Badge>
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className={buttonClass('ghost', 'sm')}
                >
                  <LogOutIcon className="size-4" />
                  Log out
                </button>
              </>
            ) : (
              <Link to="/login" className={buttonClass('ghost', 'sm')}>
                Log in
              </Link>
            )}
          </div>

          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            aria-expanded={menuOpen}
            aria-controls="site-menu"
            className={buttonClass('secondary', 'sm', 'ml-auto px-2.5 md:hidden')}
          >
            {menuOpen ? <XIcon className="size-5" /> : <MenuIcon className="size-5" />}
            <span className="sr-only">{menuOpen ? 'Close menu' : 'Open menu'}</span>
          </button>
        </div>

        {menuOpen && (
          <div id="site-menu" className="animate-fade border-t border-line bg-surface md:hidden">
            <nav aria-label="Mobile" className="mx-auto max-w-7xl px-4 py-4 sm:px-6">
              <ul className="flex flex-col">
                {navItems.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      className={({ isActive }) =>
                        [
                          'flex min-h-11 items-center rounded-md px-3 text-[0.9375rem] font-medium',
                          isActive ? 'bg-brand-50 text-brand-700' : 'text-ink hover:bg-surface-2',
                        ].join(' ')
                      }
                    >
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>

              <div className="mt-4 flex flex-col gap-2 border-t border-line pt-4">
                {canPostRequest && (
                  <Link
                    to="/customer/requests/new"
                    className={buttonClass('primary', 'md', 'w-full')}
                  >
                    Post a request
                  </Link>
                )}

                {accessToken && user ? (
                  <>
                    <p className="flex items-center gap-2.5 px-1 text-sm text-ink-muted">
                      <Avatar name={displayName} size="sm" />
                      <span className="min-w-0 flex-1 truncate font-semibold text-ink">
                        {displayName}
                      </span>
                      <Badge tone="neutral">{ROLE_LABEL[user.role]}</Badge>
                    </p>
                    <button
                      type="button"
                      onClick={handleLogout}
                      className={buttonClass('secondary', 'md', 'w-full')}
                    >
                      <LogOutIcon className="size-4" />
                      Log out
                    </button>
                  </>
                ) : (
                  <div className="grid grid-cols-2 gap-2">
                    <Link to="/login" className={buttonClass('secondary', 'md')}>
                      Log in
                    </Link>
                    <Link to="/register" className={buttonClass('primary', 'md')}>
                      Create account
                    </Link>
                  </div>
                )}
              </div>
            </nav>
          </div>
        )}
      </header>

      <main id="main" ref={mainRef} tabIndex={-1} className="flex-1">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10 lg:px-8">
          <Outlet />
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}

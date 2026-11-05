import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-2 py-1 text-sm font-medium ${
    isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
  }`

export default function Layout() {
  const user = useAuthStore((state) => state.user)
  const accessToken = useAuthStore((state) => state.accessToken)
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <Link to="/catalogue" className="text-lg font-semibold tracking-tight">
            HomeServices
          </Link>

          <div className="flex flex-wrap items-center gap-1">
            <NavLink to="/catalogue" className={navLinkClass}>
              Catalogue
            </NavLink>

            {user?.role === 'CUSTOMER' && (
              <NavLink to="/customer/requests/new" className={navLinkClass}>
                New request
              </NavLink>
            )}

            {user?.role === 'PROVIDER' && (
              <NavLink to="/provider/profile" className={navLinkClass}>
                Provider profile
              </NavLink>
            )}

            {user?.role === 'ADMIN' && (
              <>
                <NavLink to="/admin/dashboard" className={navLinkClass}>
                  Dashboard
                </NavLink>
                <NavLink to="/admin/providers" className={navLinkClass}>
                  Providers
                </NavLink>
                <NavLink to="/admin/catalog" className={navLinkClass}>
                  Catalog
                </NavLink>
                <NavLink to="/admin/reviews" className={navLinkClass}>
                  Reviews
                </NavLink>
              </>
            )}

            {accessToken ? (
              <button
                type="button"
                onClick={handleLogout}
                className="ml-2 rounded-md border border-slate-300 px-2 py-1 text-sm font-medium text-slate-600 hover:bg-slate-100"
              >
                Log out{user ? ` (${user.fullName ?? user.email})` : ''}
              </button>
            ) : (
              <>
                <NavLink to="/login" className={navLinkClass}>
                  Log in
                </NavLink>
                <NavLink to="/register" className={navLinkClass}>
                  Register
                </NavLink>
              </>
            )}
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

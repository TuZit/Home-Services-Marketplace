import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAuth from './components/RequireAuth'
import RequireRole from './components/RequireRole'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import CataloguePage from './pages/catalogue/CataloguePage'
import CategoryPage from './pages/catalogue/CategoryPage'
import NewRequestPage from './pages/customer/NewRequestPage'
import RequestDetailPage from './pages/customer/RequestDetailPage'
import RequestProvidersPage from './pages/customer/RequestProvidersPage'
import QuotationPage from './pages/quotation/QuotationPage'
import BookingPage from './pages/booking/BookingPage'
import ProviderProfilePage from './pages/provider/ProviderProfilePage'
import ProviderQuotePage from './pages/provider/ProviderQuotePage'
import AdminProvidersPage from './pages/admin/AdminProvidersPage'
import AdminCatalogPage from './pages/admin/AdminCatalogPage'
import AdminReviewsPage from './pages/admin/AdminReviewsPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/catalogue" replace />} />

        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route path="/catalogue" element={<CataloguePage />} />
        <Route path="/catalogue/:categoryId" element={<CategoryPage />} />

        <Route
          path="/customer/requests/new"
          element={
            <RequireAuth>
              <RequireRole role="CUSTOMER">
                <NewRequestPage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/customer/requests/:id"
          element={
            <RequireAuth>
              <RequireRole role="CUSTOMER">
                <RequestDetailPage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/customer/requests/:id/providers"
          element={
            <RequireAuth>
              <RequireRole role="CUSTOMER">
                <RequestProvidersPage />
              </RequireRole>
            </RequireAuth>
          }
        />

        <Route
          path="/quotations/:id"
          element={
            <RequireAuth>
              <QuotationPage />
            </RequireAuth>
          }
        />
        <Route
          path="/bookings/:id"
          element={
            <RequireAuth>
              <BookingPage />
            </RequireAuth>
          }
        />

        <Route
          path="/provider/profile"
          element={
            <RequireAuth>
              <RequireRole role="PROVIDER">
                <ProviderProfilePage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/provider/requests/:id/quote"
          element={
            <RequireAuth>
              <RequireRole role="PROVIDER">
                <ProviderQuotePage />
              </RequireRole>
            </RequireAuth>
          }
        />

        <Route
          path="/admin/providers"
          element={
            <RequireAuth>
              <RequireRole role="ADMIN">
                <AdminProvidersPage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/catalog"
          element={
            <RequireAuth>
              <RequireRole role="ADMIN">
                <AdminCatalogPage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/reviews"
          element={
            <RequireAuth>
              <RequireRole role="ADMIN">
                <AdminReviewsPage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/admin/dashboard"
          element={
            <RequireAuth>
              <RequireRole role="ADMIN">
                <AdminDashboardPage />
              </RequireRole>
            </RequireAuth>
          }
        />

        <Route path="*" element={<Navigate to="/catalogue" replace />} />
      </Route>
    </Routes>
  )
}

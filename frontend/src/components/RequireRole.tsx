import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../store/auth'
import type { Role } from '../types'

interface RequireRoleProps {
  role: Role
  children: ReactNode
}

export default function RequireRole({ role, children }: RequireRoleProps) {
  const user = useAuthStore((state) => state.user)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (user.role !== role) {
    return <Navigate to="/catalogue" replace />
  }

  return <>{children}</>
}

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import { useAdminUsers } from '../../api/hooks'
import Spinner from '../../components/Spinner'
import ErrorAlert from '../../components/ErrorAlert'
import { getErrorMessage } from '../../lib/errors'

type ProviderAction = 'verify' | 'reject' | 'suspend'

export default function AdminProvidersPage() {
  const queryClient = useQueryClient()
  const users = useAdminUsers()

  const action = useMutation({
    mutationFn: ({ providerId, actionType }: { providerId: string; actionType: ProviderAction }) =>
      apiClient.post(`/admin/providers/${providerId}/${actionType}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
  })

  if (users.isLoading) {
    return <Spinner />
  }

  if (users.isError) {
    return <ErrorAlert message={getErrorMessage(users.error)} />
  }

  const providers = (users.data ?? []).filter((user) => user.role === 'PROVIDER')

  return (
    <div>
      <h1 className="text-2xl font-bold">Provider management</h1>
      <p className="mt-1 text-sm text-slate-500">Verify, reject, or suspend provider accounts.</p>

      {action.isError && (
        <div className="mt-6">
          <ErrorAlert message={getErrorMessage(action.error)} />
        </div>
      )}

      {providers.length === 0 ? (
        <p className="mt-8 text-sm text-slate-500">No provider accounts found.</p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-400">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {providers.map((provider) => (
                <tr key={provider.id}>
                  <td className="px-4 py-3 font-medium">{provider.fullName}</td>
                  <td className="px-4 py-3 text-slate-500">{provider.email}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`rounded-full px-2 py-1 text-xs font-medium ${
                        provider.status === 'ACTIVE'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-amber-100 text-amber-700'
                      }`}
                    >
                      {provider.status ?? 'Pending'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => action.mutate({ providerId: provider.id, actionType: 'verify' })}
                        disabled={action.isPending}
                        className="rounded-md bg-slate-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700 disabled:opacity-60"
                      >
                        Verify
                      </button>
                      <button
                        type="button"
                        onClick={() => action.mutate({ providerId: provider.id, actionType: 'reject' })}
                        disabled={action.isPending}
                        className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
                      >
                        Reject
                      </button>
                      <button
                        type="button"
                        onClick={() => action.mutate({ providerId: provider.id, actionType: 'suspend' })}
                        disabled={action.isPending}
                        className="rounded-md border border-red-300 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                      >
                        Suspend
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

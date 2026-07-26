import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '@/shared/api'
import { useAuthStore } from '@/shared/store/authStore'
import type { Order } from '@/types'
import { Package, Download, X, ChevronDown, ChevronRight, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'

type StatusFilter = 'ALL' | 'PLACED' | 'PREPARING' | 'READY' | 'DELIVERED' | 'CANCELLED'

const STATUS_BADGE: Record<string, string> = {
  DRAFT:     'bg-gray-100 text-gray-700',
  PLACED:    'bg-blue-100 text-blue-700',
  PREPARING: 'bg-amber-100 text-amber-700',
  READY:     'bg-yellow-100 text-yellow-800',
  DELIVERED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-red-100 text-red-700',
}

const NEXT_STATUS: Record<string, string> = {
  PLACED:    'PREPARING',
  PREPARING: 'READY',
  READY:     'DELIVERED',
}

const STATUS_ACTION_LABEL: Record<string, string> = {
  PLACED:    '→ Mark Preparing',
  PREPARING: '→ Mark Ready',
  READY:     '→ Mark Delivered',
}

export default function OrdersPage() {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const [activeFilter, setActiveFilter] = useState<StatusFilter>('ALL')
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['orders'],
    queryFn: () => orderApi.getOrders().then(r => r.data.data?.content ?? []),
    refetchInterval: 15_000,
  })

  const orders: Order[] = data ?? []

  const filtered = activeFilter === 'ALL'
    ? orders
    : orders.filter(o => o.status === activeFilter)

  // Update status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      orderApi.updateStatus(id, status),
    onSuccess: (_, { status }) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      toast.success(`Order status updated to ${status}`)
    },
    onError: () => toast.error('Failed to update order status'),
  })

  // Cancel order mutation
  const cancelMutation = useMutation({
    mutationFn: (id: string) => orderApi.cancelOrder(id, 'Cancelled by staff'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      toast.success('Order cancelled')
    },
    onError: () => toast.error('Failed to cancel order'),
  })

  const canChangeStatus = ['BUSINESS_OWNER', 'OUTLET_MANAGER', 'CASHIER'].includes(user?.role ?? '')

  const counts: Record<string, number> = { ALL: orders.length }
  orders.forEach(o => { counts[o.status] = (counts[o.status] ?? 0) + 1 })

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Order Management</h1>
          <p className="text-sm text-gray-500 mt-1">
            {orders.length} total orders · auto-refreshes every 15s
          </p>
        </div>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-4 py-2 border border-gray-300 hover:bg-gray-50 rounded-xl text-sm font-medium text-gray-600 transition-colors"
        >
          <RefreshCw className="w-4 h-4" /> Refresh
        </button>
      </div>

      {/* Status filter tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1 flex-wrap">
        {(['ALL', 'PLACED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED'] as StatusFilter[]).map(s => (
          <button
            key={s}
            onClick={() => setActiveFilter(s)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-colors ${
              activeFilter === s
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            {s} {counts[s] !== undefined && counts[s] > 0 ? `(${counts[s]})` : ''}
          </button>
        ))}
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-400">
            <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            Loading orders...
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center py-16 text-gray-400">
            <Package className="w-12 h-12 mb-3 opacity-30" />
            <p className="text-sm">No orders found for this filter.</p>
          </div>
        ) : (
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                <th className="px-4 py-3 text-left w-6"></th>
                <th className="px-4 py-3 text-left">Order #</th>
                <th className="px-4 py-3 text-left hidden md:table-cell">Type</th>
                <th className="px-4 py-3 text-center">Status</th>
                <th className="px-4 py-3 text-right">Total</th>
                <th className="px-4 py-3 text-center">Payment</th>
                <th className="px-4 py-3 text-left hidden lg:table-cell">Date / Time</th>
                {canChangeStatus && <th className="px-4 py-3 text-right">Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((order: Order) => (
                <>
                  <tr
                    key={order.id}
                    className="hover:bg-gray-50/80 transition-colors cursor-pointer"
                    onClick={() => setExpandedId(expandedId === order.id ? null : order.id)}
                  >
                    <td className="px-4 py-3">
                      {expandedId === order.id
                        ? <ChevronDown className="w-4 h-4 text-gray-400" />
                        : <ChevronRight className="w-4 h-4 text-gray-400" />
                      }
                    </td>
                    <td className="px-4 py-3 font-bold text-gray-900">#{order.orderNumber}</td>
                    <td className="px-4 py-3 hidden md:table-cell text-gray-600 text-xs font-medium">
                      <span className="px-2 py-0.5 bg-gray-100 rounded-lg">{order.orderType}</span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${STATUS_BADGE[order.status] ?? ''}`}>
                        {order.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-gray-900">₹{Number(order.total).toFixed(2)}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                        order.paymentStatus === 'PAID' ? 'bg-emerald-100 text-emerald-700' :
                        order.paymentStatus === 'PARTIAL' ? 'bg-amber-100 text-amber-700' :
                        'bg-gray-100 text-gray-600'
                      }`}>
                        {order.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell text-gray-500 text-xs">
                      {new Date(order.createdAt).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}
                    </td>
                    {canChangeStatus && (
                      <td className="px-4 py-3 text-right" onClick={e => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-2">
                          {NEXT_STATUS[order.status] && (
                            <button
                              onClick={() => updateStatusMutation.mutate({ id: order.id, status: NEXT_STATUS[order.status] })}
                              className="text-xs px-2.5 py-1 bg-blue-50 hover:bg-blue-100 text-blue-700 font-semibold rounded-lg transition-colors whitespace-nowrap"
                            >
                              {STATUS_ACTION_LABEL[order.status]}
                            </button>
                          )}
                          {!['DELIVERED', 'CANCELLED'].includes(order.status) && (
                            <button
                              onClick={() => {
                                if (confirm(`Cancel order #${order.orderNumber}?`)) {
                                  cancelMutation.mutate(order.id)
                                }
                              }}
                              className="text-xs p-1.5 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                              title="Cancel Order"
                            >
                              <X className="w-3.5 h-3.5" />
                            </button>
                          )}
                          {order.paymentStatus === 'PAID' && (
                            <button
                              onClick={() => toast.info('Invoice PDF download requires backend')}
                              className="text-xs p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                              title="Download Invoice"
                            >
                              <Download className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>

                  {/* Expanded order items row */}
                  {expandedId === order.id && (
                    <tr key={`${order.id}-expanded`} className="bg-blue-50/40">
                      <td colSpan={canChangeStatus ? 8 : 7} className="px-8 py-4">
                        <div className="space-y-2">
                          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Order Items</p>
                          {order.items?.length > 0 ? order.items.map((item, i) => (
                            <div key={i} className="flex items-center justify-between text-sm py-1 border-b border-blue-100 last:border-0">
                              <div className="flex items-center gap-3">
                                <span className="w-6 h-6 bg-blue-100 text-blue-700 rounded-lg text-xs font-bold flex items-center justify-center">{item.quantity}×</span>
                                <span className="font-medium text-gray-800">{item.menuItemName}</span>
                                {item.kdsNotes && <span className="text-xs text-gray-500 italic">— {item.kdsNotes}</span>}
                              </div>
                              <div className="text-right">
                                <span className="font-semibold text-gray-900">₹{Number(item.totalPrice).toFixed(2)}</span>
                                <span className={`ml-2 text-xs font-semibold px-1.5 py-0.5 rounded ${
                                  item.kdsStatus === 'DONE' ? 'bg-emerald-100 text-emerald-700' :
                                  item.kdsStatus === 'PREPARING' ? 'bg-amber-100 text-amber-700' :
                                  'bg-gray-100 text-gray-600'
                                }`}>{item.kdsStatus}</span>
                              </div>
                            </div>
                          )) : (
                            <p className="text-sm text-gray-400 italic">No item details available</p>
                          )}
                          {order.notes && (
                            <p className="text-xs text-amber-700 bg-amber-50 px-2 py-1 rounded-lg mt-2">📝 {order.notes}</p>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

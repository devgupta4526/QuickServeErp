import { useQuery } from '@tanstack/react-query'
import { orderApi } from '@/shared/api'
import type { Order } from '@/types'
import { Package } from 'lucide-react'

const STATUS_BADGES: Record<string, string> = {
  DRAFT:     'bg-gray-100 text-gray-700',
  PLACED:    'bg-blue-100 text-blue-700',
  PREPARING: 'bg-orange-100 text-orange-700',
  READY:     'bg-yellow-100 text-yellow-700',
  DELIVERED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
}

export default function OrdersPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['orders'],
    queryFn: () => orderApi.getOrders().then(r => r.data.data?.content ?? []),
    refetchInterval: 15_000,
  })

  const orders: Order[] = data ?? []

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Orders</h1>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-400">Loading orders...</div>
        ) : orders.length === 0 ? (
          <div className="flex flex-col items-center py-16 text-gray-400">
            <Package className="w-12 h-12 mb-3 opacity-40" />
            <p>No orders yet. Start taking orders from the POS.</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-700">Order #</th>
                <th className="text-left px-4 py-3 font-medium text-gray-700 hidden md:table-cell">Type</th>
                <th className="text-center px-4 py-3 font-medium text-gray-700">Status</th>
                <th className="text-right px-4 py-3 font-medium text-gray-700">Total</th>
                <th className="text-center px-4 py-3 font-medium text-gray-700">Payment</th>
                <th className="text-left px-4 py-3 font-medium text-gray-700 hidden lg:table-cell">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {orders.map((order: Order) => (
                <tr key={order.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">#{order.orderNumber}</td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-600">{order.orderType}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${STATUS_BADGES[order.status] ?? ''}`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right font-medium">₹{Number(order.total).toFixed(2)}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      order.paymentStatus === 'PAID' ? 'bg-green-100 text-green-700' :
                      order.paymentStatus === 'PARTIAL' ? 'bg-yellow-100 text-yellow-700' :
                      'bg-gray-100 text-gray-700'
                    }`}>
                      {order.paymentStatus}
                    </span>
                  </td>
                  <td className="px-4 py-3 hidden lg:table-cell text-gray-500 text-xs">
                    {new Date(order.createdAt).toLocaleString('en-IN')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

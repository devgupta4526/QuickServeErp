import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { toast } from 'sonner'
import { Clock, CheckCircle, ChefHat, RefreshCw } from 'lucide-react'
import { kdsApi } from '@/shared/api'
import { useAuthStore } from '@/shared/store/authStore'
import type { Order, OrderItem, KdsStatus } from '@/types'
import { Client as StompClient } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const STATUS_COLORS: Record<KdsStatus, string> = {
  PENDING:    'bg-yellow-100 border-yellow-400 text-yellow-800',
  PREPARING:  'bg-orange-100 border-orange-400 text-orange-800',
  DONE:       'bg-green-100  border-green-400  text-green-800',
}

function formatElapsed(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'Just now'
  if (mins < 60) return `${mins}m ago`
  return `${Math.floor(mins / 60)}h ${mins % 60}m ago`
}

export default function KdsPage() {
  const { user } = useAuthStore()
  const outletId = user?.outletId ?? ''
  const queryClient = useQueryClient()
  const stompRef = useRef<StompClient | null>(null)
  const [elapsed, setElapsed] = useState(0)

  // Tick every 30 seconds to update elapsed time display
  useEffect(() => {
    const timer = setInterval(() => setElapsed(n => n + 1), 30_000)
    return () => clearInterval(timer)
  }, [])

  // Fetch active orders
  const { data: orders = [], isLoading, refetch } = useQuery<Order[]>({
    queryKey: ['kds', outletId],
    queryFn: () => kdsApi.getActiveOrders(outletId).then(r => r.data.data ?? []),
    refetchInterval: 10_000, // Poll every 10s as fallback
    enabled: !!outletId,
  })

  // WebSocket connection for real-time updates
  useEffect(() => {
    if (!outletId) return
    const client = new StompClient({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        client.subscribe(`/topic/kds/${outletId}`, () => {
          queryClient.invalidateQueries({ queryKey: ['kds', outletId] })
        })
      },
    })
    client.activate()
    stompRef.current = client
    return () => { client.deactivate() }
  }, [outletId, queryClient])

  // Update item status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ itemId, status }: { itemId: string; status: string }) =>
      kdsApi.updateItemStatus(itemId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kds', outletId] })
    },
    onError: () => toast.error('Failed to update item status'),
  })

  const cycleStatus = (item: OrderItem) => {
    const next: Record<KdsStatus, KdsStatus> = {
      PENDING: 'PREPARING',
      PREPARING: 'DONE',
      DONE: 'PREPARING',
    }
    updateStatusMutation.mutate({ itemId: item.id, status: next[item.kdsStatus] })
  }

  if (!outletId) {
    return (
      <div className="p-8 text-center text-gray-500">
        <ChefHat className="w-16 h-16 mx-auto mb-4 opacity-30" />
        <p>No outlet assigned. Please contact your manager.</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-950 text-white p-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <ChefHat className="w-8 h-8 text-orange-400" />
          <div>
            <h1 className="text-xl font-bold">Kitchen Display</h1>
            <p className="text-sm text-gray-400">{orders.length} active orders</p>
          </div>
        </div>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 rounded-lg text-sm transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="bg-gray-800 rounded-xl h-64 animate-pulse" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-80 text-gray-500">
          <CheckCircle className="w-20 h-20 mb-4 text-green-600 opacity-60" />
          <p className="text-xl font-medium">All clear! No pending orders.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {orders.map(order => (
            <div
              key={order.id}
              className={`bg-gray-900 rounded-xl border-2 overflow-hidden ${
                order.status === 'PLACED' ? 'border-yellow-500' : 'border-orange-500'
              }`}
            >
              {/* Order header */}
              <div className="px-4 py-3 bg-gray-800 flex justify-between items-center">
                <div>
                  <p className="font-bold text-lg">#{order.orderNumber}</p>
                  <p className="text-xs text-gray-400">
                    {order.tableId ? `Table ${order.tableId}` : order.orderType}
                  </p>
                </div>
                <div className="text-right">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                    order.status === 'PLACED' ? 'bg-yellow-900 text-yellow-300' : 'bg-orange-900 text-orange-300'
                  }`}>
                    {order.status}
                  </span>
                  <p className="text-xs text-gray-500 mt-1">
                    <Clock className="inline w-3 h-3 mr-1" />
                    {formatElapsed(order.createdAt)}
                  </p>
                </div>
              </div>

              {/* Order items */}
              <div className="p-3 space-y-2">
                {order.items?.map(item => (
                  <button
                    key={item.id}
                    onClick={() => cycleStatus(item)}
                    className={`w-full text-left px-3 py-2 rounded-lg border ${STATUS_COLORS[item.kdsStatus]} transition-all`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-sm">
                        {item.quantity}× {item.menuItemName}
                      </span>
                      <span className="text-xs font-medium capitalize">{item.kdsStatus}</span>
                    </div>
                    {item.kdsNotes && (
                      <p className="text-xs mt-1 opacity-70">{item.kdsNotes}</p>
                    )}
                  </button>
                ))}
              </div>

              {/* Notes */}
              {order.notes && (
                <div className="px-3 pb-3">
                  <p className="text-xs text-yellow-400 bg-yellow-900/30 rounded px-2 py-1">
                    📝 {order.notes}
                  </p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

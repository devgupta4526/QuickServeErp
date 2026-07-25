import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { menuApi, orderApi } from '@/shared/api'
import type { Category, MenuItem } from '@/types'

export default function QrMenuPage() {
  // outletId from URL param
  const outletId = window.location.pathname.split('/').pop() ?? ''
  const [cart, setCart] = useState<{ item: MenuItem; qty: number }[]>([])

  const { data: items = [] } = useQuery<MenuItem[]>({
    queryKey: ['publicMenu', outletId],
    queryFn: () => menuApi.getPublicMenu(outletId).then(r => r.data.data ?? []),
    enabled: !!outletId,
  })

  const addToCart = (item: MenuItem) => {
    setCart(prev => {
      const existing = prev.find(c => c.item.id === item.id)
      if (existing) return prev.map(c => c.item.id === item.id ? { ...c, qty: c.qty + 1 } : c)
      return [...prev, { item, qty: 1 }]
    })
  }

  const total = cart.reduce((acc, c) => acc + c.item.basePrice * c.qty, 0)

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      {/* Header */}
      <header className="bg-blue-600 text-white p-6 text-center">
        <div className="w-12 h-12 bg-white rounded-xl mx-auto mb-3 flex items-center justify-center">
          <span className="text-blue-600 font-bold text-xl">QS</span>
        </div>
        <h1 className="text-xl font-bold">Menu</h1>
        <p className="text-blue-200 text-sm">Self-order — powered by QuickServe</p>
      </header>

      {/* Items */}
      <div className="max-w-lg mx-auto px-4 py-6 space-y-4">
        {items.map(item => (
          <div key={item.id} className="bg-white rounded-xl border border-gray-200 flex items-center gap-4 p-4">
            {item.imageUrl && (
              <img src={item.imageUrl} alt={item.name} className="w-20 h-20 rounded-lg object-cover" />
            )}
            <div className="flex-1 min-w-0">
              <p className="font-medium text-gray-900">{item.name}</p>
              {item.description && <p className="text-sm text-gray-500 mt-0.5 line-clamp-2">{item.description}</p>}
              <p className="font-bold text-blue-600 mt-1">₹{Number(item.basePrice).toFixed(0)}</p>
            </div>
            <button
              onClick={() => addToCart(item)}
              className="w-9 h-9 rounded-full bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center text-xl flex-shrink-0 transition-colors"
            >
              +
            </button>
          </div>
        ))}
      </div>

      {/* Cart bottom bar */}
      {cart.length > 0 && (
        <div className="fixed bottom-0 inset-x-0 p-4 bg-white border-t border-gray-200 max-w-lg mx-auto">
          <button className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors flex items-center justify-between px-5">
            <span>{cart.reduce((acc, c) => acc + c.qty, 0)} items</span>
            <span>Place Order — ₹{total.toFixed(0)}</span>
          </button>
        </div>
      )}
    </div>
  )
}

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'
import {
  ShoppingCart, Plus, Minus, Trash2, CreditCard, Banknote, Smartphone,
  Search, User, Tag, ChevronDown, UtensilsCrossed, ShoppingBag, Bike, X
} from 'lucide-react'
import { menuApi, orderApi, crmApi } from '@/shared/api'
import { useCartStore } from '@/shared/store/cartStore'
import { useAuthStore } from '@/shared/store/authStore'
import type { MenuItem, Category, Customer } from '@/types'

type OrderType = 'DINE_IN' | 'TAKEAWAY' | 'DELIVERY'

const ORDER_TYPE_ICONS: Record<OrderType, React.ReactNode> = {
  DINE_IN:  <UtensilsCrossed className="w-4 h-4" />,
  TAKEAWAY: <ShoppingBag className="w-4 h-4" />,
  DELIVERY: <Bike className="w-4 h-4" />,
}

export default function PosPage() {
  const { user } = useAuthStore()
  const outletId = user?.outletId ?? ''
  const queryClient = useQueryClient()

  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [paymentModalOpen, setPaymentModalOpen] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'UPI' | 'CARD'>('CASH')
  const [orderType, setOrderType] = useState<OrderType>('DINE_IN')
  const [tableNo, setTableNo] = useState('')
  const [discountAmt, setDiscountAmt] = useState('')
  const [customerQuery, setCustomerQuery] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)
  const [showCustomerSearch, setShowCustomerSearch] = useState(false)
  const [notes, setNotes] = useState('')

  const cart = useCartStore()

  // Fetch categories
  const { data: catData } = useQuery({
    queryKey: ['categories'],
    queryFn: () => menuApi.getCategories().then(r => r.data.data as Category[]),
  })

  // Fetch menu items
  const { data: itemData } = useQuery({
    queryKey: ['menuItems'],
    queryFn: () => menuApi.getItems(0, 200).then(r => r.data.data?.content as MenuItem[]),
  })

  // Customer search
  const { data: customers = [] } = useQuery({
    queryKey: ['customerSearch', customerQuery],
    queryFn: () => crmApi.searchCustomers(customerQuery).then(r => r.data.data as Customer[]),
    enabled: customerQuery.length >= 2,
  })

  const subtotal = cart.subtotal()
  const discount = parseFloat(discountAmt) || 0
  const estimatedTax = Math.round(subtotal * 0.05 * 100) / 100
  const total = Math.max(0, subtotal + estimatedTax - discount)

  // Place order mutation
  const placeOrderMutation = useMutation({
    mutationFn: () => orderApi.createOrder({
      outletId,
      orderType,
      tableId: orderType === 'DINE_IN' && tableNo ? tableNo : undefined,
      customerId: selectedCustomer?.id ?? undefined,
      notes: notes || undefined,
      items: cart.items.map(i => ({
        menuItemId: i.menuItemId,
        quantity: i.quantity,
        variantId: i.variantId,
      })),
      subtotal,
      taxAmount: estimatedTax,
      discountAmount: discount,
      total,
    }),
    onSuccess: (res) => {
      const order = res.data.data
      toast.success(`Order #${order.orderNumber} placed! 🎉`)
      cart.clearCart()
      setDiscountAmt('')
      setNotes('')
      setSelectedCustomer(null)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setPaymentModalOpen(false)
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Failed to place order')
    },
  })

  const filteredItems = (itemData ?? []).filter(item => {
    const matchesCategory = !selectedCategoryId || item.categoryId === selectedCategoryId
    const matchesSearch = !searchQuery || item.name.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesCategory && matchesSearch && item.available && !item.archived
  })

  const fmt = (n: number) =>
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 }).format(n)

  return (
    <div className="flex h-[calc(100vh-4rem)] overflow-hidden">
      {/* Left — Menu */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden bg-gray-50">
        {/* Search + Categories */}
        <div className="bg-white border-b border-gray-200 px-4 pt-4 pb-3 space-y-3 flex-shrink-0">
          {/* Order Type Selector */}
          <div className="flex gap-2">
            {(['DINE_IN', 'TAKEAWAY', 'DELIVERY'] as OrderType[]).map(type => (
              <button
                key={type}
                onClick={() => setOrderType(type)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                  orderType === type ? 'bg-blue-600 text-white shadow-sm' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {ORDER_TYPE_ICONS[type]}
                {type.replace('_', ' ')}
              </button>
            ))}
            {orderType === 'DINE_IN' && (
              <input
                type="text"
                placeholder="Table # (optional)"
                value={tableNo}
                onChange={e => setTableNo(e.target.value)}
                className="ml-auto px-3 py-1.5 border border-gray-300 rounded-xl text-xs w-32 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            )}
          </div>

          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search menu items..."
              className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
            <button
              onClick={() => setSelectedCategoryId(null)}
              className={`px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-colors ${
                !selectedCategoryId ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              All Items
            </button>
            {catData?.map(cat => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategoryId(cat.id)}
                className={`px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-colors ${
                  selectedCategoryId === cat.id ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>

        {/* Item grid */}
        <div className="flex-1 overflow-y-auto p-4">
          {filteredItems.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 text-gray-400">
              <Search className="w-10 h-10 mb-2 opacity-30" />
              <p className="text-sm">No items found</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 lg:grid-cols-4 gap-3">
              {filteredItems.map(item => {
                const inCart = cart.items.find(i => i.menuItemId === item.id)
                return (
                  <button
                    key={item.id}
                    onClick={() => cart.addItem({
                      menuItemId: item.id,
                      menuItemName: item.name,
                      basePrice: item.basePrice,
                      quantity: 1,
                    })}
                    className={`bg-white rounded-xl border text-left hover:shadow-md transition-all active:scale-95 relative ${
                      inCart ? 'border-blue-400 ring-2 ring-blue-200' : 'border-gray-200 hover:border-blue-300'
                    }`}
                  >
                    {inCart && (
                      <div className="absolute top-2 right-2 w-5 h-5 bg-blue-600 text-white text-xs font-bold rounded-full flex items-center justify-center">
                        {inCart.quantity}
                      </div>
                    )}
                    <div className="p-3">
                      <div className="w-full h-20 bg-gradient-to-br from-gray-50 to-gray-100 rounded-lg mb-2.5 flex items-center justify-center text-2xl">
                        {item.veg ? '🥗' : '🍗'}
                      </div>
                      <p className="text-xs font-semibold text-gray-900 line-clamp-2 leading-tight">{item.name}</p>
                      <div className="flex items-center justify-between mt-1.5">
                        <p className="text-sm font-bold text-blue-600">{fmt(item.basePrice)}</p>
                        <span className={`w-2.5 h-2.5 rounded-full ${item.veg ? 'bg-green-500' : 'bg-red-500'}`} />
                      </div>
                    </div>
                  </button>
                )
              })}
            </div>
          )}
        </div>
      </div>

      {/* Right — Cart */}
      <div className="w-80 xl:w-96 flex flex-col bg-white border-l border-gray-200 flex-shrink-0">
        {/* Cart Header */}
        <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-2">
            <ShoppingCart className="w-5 h-5 text-gray-700" />
            <h2 className="font-bold text-gray-900">Bill</h2>
            {cart.itemCount() > 0 && (
              <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center font-bold">
                {cart.itemCount()}
              </span>
            )}
          </div>
          {cart.items.length > 0 && (
            <button onClick={cart.clearCart} className="text-xs text-red-500 hover:text-red-700 font-medium">
              Clear All
            </button>
          )}
        </div>

        {/* Customer Panel */}
        <div className="px-4 py-2.5 border-b border-gray-100 flex-shrink-0">
          {selectedCustomer ? (
            <div className="flex items-center justify-between bg-blue-50 rounded-xl px-3 py-2">
              <div>
                <p className="text-xs font-semibold text-blue-800">{selectedCustomer.name}</p>
                <p className="text-xs text-blue-600">{selectedCustomer.loyaltyPoints} pts · {selectedCustomer.tier}</p>
              </div>
              <button onClick={() => setSelectedCustomer(null)} className="text-blue-400 hover:text-blue-600">
                <X className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="relative">
              <button
                onClick={() => setShowCustomerSearch(!showCustomerSearch)}
                className="w-full flex items-center gap-2 px-3 py-2 rounded-xl border border-dashed border-gray-300 hover:border-blue-400 text-sm text-gray-500 transition-colors"
              >
                <User className="w-4 h-4" />
                Add customer (for loyalty)
              </button>
              {showCustomerSearch && (
                <div className="absolute top-full left-0 right-0 z-20 mt-1 bg-white border border-gray-200 rounded-xl shadow-lg">
                  <div className="p-2">
                    <input
                      type="text"
                      autoFocus
                      placeholder="Search by name or phone..."
                      value={customerQuery}
                      onChange={e => setCustomerQuery(e.target.value)}
                      className="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  {customers.length > 0 && (
                    <div className="border-t border-gray-100 max-h-36 overflow-y-auto">
                      {customers.map((c: Customer) => (
                        <button
                          key={c.id}
                          onClick={() => { setSelectedCustomer(c); setShowCustomerSearch(false); setCustomerQuery('') }}
                          className="w-full text-left px-4 py-2.5 hover:bg-blue-50 text-sm transition-colors"
                        >
                          <p className="font-semibold text-gray-900">{c.name}</p>
                          <p className="text-xs text-gray-500">{c.phone} · {c.loyaltyPoints} pts</p>
                        </button>
                      ))}
                    </div>
                  )}
                  {customerQuery.length >= 2 && customers.length === 0 && (
                    <p className="text-xs text-gray-400 text-center px-4 py-3">No customers found</p>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Cart Items */}
        <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2">
          {cart.items.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 text-gray-400">
              <ShoppingCart className="w-10 h-10 mb-2 opacity-20" />
              <p className="text-sm">Tap items to add to bill</p>
            </div>
          ) : (
            cart.items.map((item, idx) => (
              <div key={idx} className="flex items-center gap-2.5 py-2 border-b border-gray-50 last:border-0">
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-semibold text-gray-900 truncate">{item.menuItemName}</p>
                  <p className="text-xs text-gray-500">{fmt(item.basePrice)}</p>
                </div>
                <div className="flex items-center gap-1 flex-shrink-0">
                  <button
                    onClick={() => cart.updateQty(item.menuItemId, item.quantity - 1, item.variantId)}
                    className="w-6 h-6 rounded-full border border-gray-200 flex items-center justify-center hover:bg-gray-100 transition-colors"
                  >
                    <Minus className="w-2.5 h-2.5" />
                  </button>
                  <span className="w-5 text-center text-sm font-bold text-gray-900">{item.quantity}</span>
                  <button
                    onClick={() => cart.updateQty(item.menuItemId, item.quantity + 1, item.variantId)}
                    className="w-6 h-6 rounded-full border border-gray-200 flex items-center justify-center hover:bg-gray-100 transition-colors"
                  >
                    <Plus className="w-2.5 h-2.5" />
                  </button>
                </div>
                <p className="text-xs font-bold text-gray-900 w-14 text-right flex-shrink-0">
                  {fmt(item.basePrice * item.quantity)}
                </p>
                <button
                  onClick={() => cart.removeItem(item.menuItemId, item.variantId)}
                  className="text-gray-300 hover:text-red-500 transition-colors ml-1 flex-shrink-0"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            ))
          )}
        </div>

        {/* Cart Footer */}
        {cart.items.length > 0 && (
          <div className="px-4 py-4 border-t border-gray-200 space-y-3 flex-shrink-0 bg-gray-50/60">
            {/* Discount input */}
            <div className="flex items-center gap-2">
              <Tag className="w-3.5 h-3.5 text-gray-400 flex-shrink-0" />
              <input
                type="number"
                placeholder="Discount amount (₹)"
                value={discountAmt}
                onChange={e => setDiscountAmt(e.target.value)}
                className="flex-1 px-2.5 py-1.5 text-xs border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Bill summary */}
            <div className="space-y-1.5 text-xs">
              <div className="flex justify-between text-gray-600">
                <span>Subtotal</span>
                <span className="font-medium">{fmt(subtotal)}</span>
              </div>
              <div className="flex justify-between text-gray-500">
                <span>GST (est. 5%)</span>
                <span>{fmt(estimatedTax)}</span>
              </div>
              {discount > 0 && (
                <div className="flex justify-between text-emerald-600">
                  <span>Discount</span>
                  <span>-{fmt(discount)}</span>
                </div>
              )}
            </div>

            <div className="flex justify-between font-bold text-base pt-2 border-t border-gray-200">
              <span className="text-gray-900">Total</span>
              <span className="text-blue-600">{fmt(total)}</span>
            </div>

            {/* Notes */}
            <input
              type="text"
              placeholder="Order notes (kitchen instructions...)"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              className="w-full px-3 py-2 text-xs border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />

            <button
              onClick={() => setPaymentModalOpen(true)}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl transition-colors flex items-center justify-center gap-2 shadow-sm"
            >
              <CreditCard className="w-4 h-4" />
              Collect Payment · {fmt(total)}
            </button>
          </div>
        )}
      </div>

      {/* Payment Modal */}
      {paymentModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm mx-4 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-gray-900">Collect Payment</h3>
              <button onClick={() => setPaymentModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="bg-blue-50 rounded-xl p-4 text-center">
              <p className="text-xs text-blue-600 font-semibold uppercase tracking-wider mb-1">Amount to Collect</p>
              <p className="text-4xl font-extrabold text-blue-700">{fmt(total)}</p>
              <div className="flex justify-center gap-4 mt-2 text-xs text-blue-500">
                <span>Subtotal: {fmt(subtotal)}</span>
                <span>GST: {fmt(estimatedTax)}</span>
                {discount > 0 && <span>Disc: -{fmt(discount)}</span>}
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold text-gray-600 uppercase tracking-wider mb-2">Payment Method</p>
              <div className="grid grid-cols-3 gap-2">
                {(['CASH', 'UPI', 'CARD'] as const).map(method => (
                  <button
                    key={method}
                    onClick={() => setPaymentMethod(method)}
                    className={`py-3 rounded-xl text-sm font-semibold transition-all flex flex-col items-center gap-1.5 ${
                      paymentMethod === method
                        ? 'bg-blue-600 text-white shadow-md scale-105'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    {method === 'CASH' ? <Banknote className="w-5 h-5" /> :
                     method === 'UPI'  ? <Smartphone className="w-5 h-5" /> :
                                         <CreditCard className="w-5 h-5" />}
                    {method}
                  </button>
                ))}
              </div>
            </div>

            {selectedCustomer && (
              <div className="bg-amber-50 border border-amber-200 rounded-xl px-3 py-2 text-xs">
                <p className="font-semibold text-amber-800">Loyalty: {selectedCustomer.name}</p>
                <p className="text-amber-600">Will earn ~{Math.floor(total / 10)} points for this purchase</p>
              </div>
            )}

            <div className="flex gap-3">
              <button
                onClick={() => setPaymentModalOpen(false)}
                className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-semibold transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => placeOrderMutation.mutate()}
                disabled={placeOrderMutation.isPending}
                className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-400 text-white rounded-xl font-semibold transition-colors"
              >
                {placeOrderMutation.isPending ? 'Processing...' : '✓ Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

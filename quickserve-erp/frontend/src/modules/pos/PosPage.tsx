import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'
import { ShoppingCart, Plus, Minus, Trash2, CreditCard, Banknote, Smartphone, Search, User } from 'lucide-react'
import { menuApi, orderApi, crmApi } from '@/shared/api'
import { useCartStore } from '@/shared/store/cartStore'
import { useAuthStore } from '@/shared/store/authStore'
import type { MenuItem, Category, Customer } from '@/types'

export default function PosPage() {
  const { user } = useAuthStore()
  const outletId = user?.outletId ?? ''
  const queryClient = useQueryClient()

  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [paymentModalOpen, setPaymentModalOpen] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'UPI' | 'CARD'>('CASH')
  const [customerSearchOpen, setCustomerSearchOpen] = useState(false)
  const [customerQuery, setCustomerQuery] = useState('')

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
  const { data: customers } = useQuery({
    queryKey: ['customerSearch', customerQuery],
    queryFn: () => crmApi.searchCustomers(customerQuery).then(r => r.data.data as Customer[]),
    enabled: customerQuery.length >= 3,
  })

  // Place order mutation
  const placeOrderMutation = useMutation({
    mutationFn: () => orderApi.createOrder({
      outletId,
      orderType: 'DINE_IN',
      tableId: cart.tableId ?? undefined,
      customerId: cart.customerId ?? undefined,
      notes: cart.notes,
      items: cart.items.map(i => ({
        menuItemId: i.menuItemId,
        quantity: i.quantity,
        variantId: i.variantId,
      })),
    }),
    onSuccess: (res) => {
      const order = res.data.data
      toast.success(`Order #${order.orderNumber} placed!`)
      cart.clearCart()
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setPaymentModalOpen(false)
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Failed to place order')
    },
  })

  const filteredItems = (itemData ?? []).filter(item => {
    const matchesCategory = !selectedCategoryId || item.categoryId === selectedCategoryId
    const matchesSearch   = !searchQuery || item.name.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesCategory && matchesSearch && item.available && !item.archived
  })

  const formatCurrency = (n: number) =>
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 }).format(n)

  return (
    <div className="flex h-[calc(100vh-4rem)] overflow-hidden">
      {/* Left — Categories + Items */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Search + Categories */}
        <div className="bg-white border-b border-gray-200 px-4 pt-4 pb-3 space-y-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search menu items..."
              className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
            <button
              onClick={() => setSelectedCategoryId(null)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                !selectedCategoryId ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              All
            </button>
            {catData?.map(cat => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategoryId(cat.id)}
                className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                  selectedCategoryId === cat.id ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>

        {/* Item grid */}
        <div className="flex-1 overflow-y-auto p-4">
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {filteredItems.map(item => (
              <button
                key={item.id}
                onClick={() => cart.addItem({
                  menuItemId: item.id,
                  menuItemName: item.name,
                  basePrice: item.basePrice,
                  quantity: 1,
                })}
                className="bg-white rounded-xl border border-gray-200 p-3 text-left hover:border-blue-400 hover:shadow-md transition-all active:scale-95"
              >
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name}
                    className="w-full h-24 object-cover rounded-lg mb-2" />
                ) : (
                  <div className="w-full h-24 bg-gradient-to-br from-blue-50 to-indigo-100 rounded-lg mb-2 flex items-center justify-center">
                    <span className="text-3xl">{item.veg ? '🥗' : '🍗'}</span>
                  </div>
                )}
                <p className="text-sm font-medium text-gray-900 line-clamp-2">{item.name}</p>
                <p className="text-base font-bold text-blue-600 mt-1">{formatCurrency(item.basePrice)}</p>
                <span className={`inline-block w-3 h-3 rounded-full mt-1 ${item.veg ? 'bg-green-500' : 'bg-red-500'}`} />
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Right — Cart */}
      <div className="w-80 xl:w-96 flex flex-col bg-white border-l border-gray-200">
        {/* Cart header */}
        <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShoppingCart className="w-5 h-5 text-gray-700" />
            <h2 className="font-semibold text-gray-900">Cart</h2>
            {cart.itemCount() > 0 && (
              <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center">
                {cart.itemCount()}
              </span>
            )}
          </div>
          {cart.items.length > 0 && (
            <button onClick={cart.clearCart} className="text-xs text-red-600 hover:text-red-800">
              Clear
            </button>
          )}
        </div>

        {/* Customer select */}
        <div className="px-4 py-2 border-b border-gray-100">
          <button
            onClick={() => setCustomerSearchOpen(true)}
            className="w-full flex items-center gap-2 px-3 py-2 rounded-lg border border-dashed border-gray-300 hover:border-blue-400 text-sm text-gray-500"
          >
            <User className="w-4 h-4" />
            {cart.customerId ? 'Customer selected' : 'Add customer (optional)'}
          </button>
        </div>

        {/* Cart items */}
        <div className="flex-1 overflow-y-auto px-4 py-2 space-y-3">
          {cart.items.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-40 text-gray-400">
              <ShoppingCart className="w-10 h-10 mb-2 opacity-30" />
              <p className="text-sm">Tap items to add to cart</p>
            </div>
          ) : (
            cart.items.map((item, idx) => (
              <div key={idx} className="flex items-center gap-3 py-2">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">{item.menuItemName}</p>
                  <p className="text-sm text-gray-500">{formatCurrency(item.basePrice)}</p>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => cart.updateQty(item.menuItemId, item.quantity - 1, item.variantId)}
                    className="w-7 h-7 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100"
                  >
                    <Minus className="w-3 h-3" />
                  </button>
                  <span className="w-6 text-center text-sm font-medium">{item.quantity}</span>
                  <button
                    onClick={() => cart.updateQty(item.menuItemId, item.quantity + 1, item.variantId)}
                    className="w-7 h-7 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100"
                  >
                    <Plus className="w-3 h-3" />
                  </button>
                </div>
                <p className="text-sm font-medium w-16 text-right">
                  {formatCurrency(item.basePrice * item.quantity)}
                </p>
                <button
                  onClick={() => cart.removeItem(item.menuItemId, item.variantId)}
                  className="text-gray-400 hover:text-red-500"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))
          )}
        </div>

        {/* Cart footer */}
        {cart.items.length > 0 && (
          <div className="px-4 py-4 border-t border-gray-200 space-y-3">
            <div className="flex justify-between text-sm text-gray-600">
              <span>Subtotal</span>
              <span>{formatCurrency(cart.subtotal())}</span>
            </div>
            <div className="flex justify-between text-sm text-gray-500">
              <span>GST (estimated)</span>
              <span className="text-gray-400">calculated at checkout</span>
            </div>
            <div className="flex justify-between font-bold text-gray-900 text-base pt-2 border-t">
              <span>Total</span>
              <span className="text-blue-600">{formatCurrency(cart.subtotal())}</span>
            </div>

            <button
              onClick={() => setPaymentModalOpen(true)}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl transition-colors flex items-center justify-center gap-2"
            >
              <CreditCard className="w-4 h-4" />
              Proceed to Payment
            </button>
          </div>
        )}
      </div>

      {/* Payment Modal */}
      {paymentModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm mx-4 space-y-4">
            <h3 className="text-lg font-bold text-gray-900">Collect Payment</h3>

            <div className="bg-gray-50 rounded-xl p-4 text-center">
              <p className="text-sm text-gray-500">Total Amount</p>
              <p className="text-3xl font-bold text-blue-600">{formatCurrency(cart.subtotal())}</p>
            </div>

            <div>
              <p className="text-sm font-medium text-gray-700 mb-2">Payment Method</p>
              <div className="grid grid-cols-3 gap-2">
                {(['CASH', 'UPI', 'CARD'] as const).map(method => (
                  <button
                    key={method}
                    onClick={() => setPaymentMethod(method)}
                    className={`py-3 rounded-xl text-sm font-medium transition-colors flex flex-col items-center gap-1 ${
                      paymentMethod === method
                        ? 'bg-blue-600 text-white'
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

            <div className="flex gap-3">
              <button
                onClick={() => setPaymentModalOpen(false)}
                className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-medium transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => placeOrderMutation.mutate()}
                disabled={placeOrderMutation.isPending}
                className="flex-1 py-3 bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white rounded-xl font-medium transition-colors"
              >
                {placeOrderMutation.isPending ? 'Processing...' : 'Confirm Payment'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

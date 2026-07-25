import { create } from 'zustand'
import type { CartItem } from '@/types'

interface CartState {
  items: CartItem[]
  customerId: string | null
  tableId: string | null
  outletId: string | null
  orderType: 'DINE_IN' | 'TAKEAWAY' | 'DELIVERY' | 'QR_SELF'
  notes: string

  addItem:    (item: CartItem) => void
  removeItem: (menuItemId: string, variantId?: string) => void
  updateQty:  (menuItemId: string, quantity: number, variantId?: string) => void
  clearCart:  () => void
  setCustomer:(id: string | null) => void
  setTable:   (id: string | null) => void
  setOutlet:  (id: string) => void
  setNotes:   (notes: string) => void

  total:         () => number
  subtotal:      () => number
  itemCount:     () => number
}

export const useCartStore = create<CartState>((set, get) => ({
  items:     [],
  customerId: null,
  tableId:   null,
  outletId:  null,
  orderType: 'DINE_IN',
  notes:     '',

  addItem: (item) => {
    const { items } = get()
    const existing = items.find(
      (i) => i.menuItemId === item.menuItemId && i.variantId === item.variantId
    )
    if (existing) {
      set({ items: items.map((i) =>
        i.menuItemId === item.menuItemId && i.variantId === item.variantId
          ? { ...i, quantity: i.quantity + item.quantity }
          : i
      )})
    } else {
      set({ items: [...items, item] })
    }
  },

  removeItem: (menuItemId, variantId) =>
    set({ items: get().items.filter(
      (i) => !(i.menuItemId === menuItemId && i.variantId === variantId)
    )}),

  updateQty: (menuItemId, quantity, variantId) => {
    if (quantity <= 0) {
      get().removeItem(menuItemId, variantId)
      return
    }
    set({ items: get().items.map((i) =>
      i.menuItemId === menuItemId && i.variantId === variantId
        ? { ...i, quantity }
        : i
    )})
  },

  clearCart: () => set({ items: [], customerId: null, tableId: null, notes: '' }),

  setCustomer: (id) => set({ customerId: id }),
  setTable:    (id) => set({ tableId: id }),
  setOutlet:   (id) => set({ outletId: id }),
  setNotes:    (notes) => set({ notes }),

  subtotal: () =>
    get().items.reduce((acc, i) => acc + i.basePrice * i.quantity, 0),

  total: () => get().subtotal(), // Tax added server-side

  itemCount: () =>
    get().items.reduce((acc, i) => acc + i.quantity, 0),
}))

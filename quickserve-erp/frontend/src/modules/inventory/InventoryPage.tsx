import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/shared/api'
import type { ApiResponse, PagedResponse } from '@/types'

interface InvItem {
  id: string
  name: string
  unit: string
  currentStock: number
  reorderLevel: number
  costPerUnit?: number
  active: boolean
  lowStock: boolean
}

interface UpdateStockReq {
  movementType: 'PURCHASE' | 'ADJUSTMENT' | 'CONSUMPTION' | 'WASTE'
  quantity: number
  notes?: string
}

export default function InventoryPage() {
  const qc = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [showAdj, setShowAdj] = useState<InvItem | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['inventory'],
    queryFn: () => api.get<ApiResponse<PagedResponse<InvItem>>>('/inventory/items').then(r => r.data.data),
  })

  const addMutation = useMutation({
    mutationFn: (body: object) => api.post('/inventory/items', body),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['inventory'] }); setShowAdd(false); toast.success('Item added') },
    onError: () => toast.error('Failed to add item'),
  })

  const adjMutation = useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateStockReq }) =>
      api.post(`/inventory/items/${id}/stock`, req),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['inventory'] }); setShowAdj(null); toast.success('Stock updated') },
    onError: () => toast.error('Failed to update stock'),
  })

  const items = data?.content ?? []
  const lowStockCount = items.filter(i => i.lowStock).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Inventory</h1>
          <p className="text-sm text-gray-500 mt-1">
            {items.length} items tracked
            {lowStockCount > 0 && (
              <span className="ml-2 px-2 py-0.5 bg-red-100 text-red-700 rounded-full text-xs font-medium">
                {lowStockCount} low stock
              </span>
            )}
          </p>
        </div>
        <button
          onClick={() => setShowAdd(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700"
        >
          + Add Item
        </button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {['Item Name', 'Unit', 'Current Stock', 'Reorder Level', 'Cost/Unit', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-4 py-3 font-medium text-gray-600">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {items.map(item => (
                <tr key={item.id} className={`hover:bg-gray-50 transition-colors ${item.lowStock ? 'bg-red-50' : ''}`}>
                  <td className="px-4 py-3 font-medium text-gray-900">{item.name}</td>
                  <td className="px-4 py-3 text-gray-600">{item.unit}</td>
                  <td className="px-4 py-3">
                    <span className={`font-semibold ${item.lowStock ? 'text-red-600' : 'text-gray-900'}`}>
                      {Number(item.currentStock).toFixed(2)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600">{Number(item.reorderLevel).toFixed(2)}</td>
                  <td className="px-4 py-3 text-gray-600">
                    {item.costPerUnit ? `₹${Number(item.costPerUnit).toFixed(2)}` : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      item.lowStock ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                    }`}>
                      {item.lowStock ? 'Low Stock' : 'OK'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setShowAdj(item)}
                      className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                    >
                      Adjust Stock
                    </button>
                  </td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-gray-400">
                    No inventory items yet. Add your first item.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Add Item Modal */}
      {showAdd && (
        <AddItemModal
          onClose={() => setShowAdd(false)}
          onSubmit={data => addMutation.mutate(data)}
          loading={addMutation.isPending}
        />
      )}

      {/* Adjust Stock Modal */}
      {showAdj && (
        <AdjustStockModal
          item={showAdj}
          onClose={() => setShowAdj(null)}
          onSubmit={req => adjMutation.mutate({ id: showAdj!.id, req })}
          loading={adjMutation.isPending}
        />
      )}
    </div>
  )
}

// ── Add Item Modal ──────────────────────────────────────────────────────────

function AddItemModal({ onClose, onSubmit, loading }: {
  onClose: () => void
  onSubmit: (data: object) => void
  loading: boolean
}) {
  const [form, setForm] = useState({ name: '', unit: 'KG', currentStock: '', reorderLevel: '', costPerUnit: '' })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit({
      name: form.name,
      unit: form.unit,
      currentStock: parseFloat(form.currentStock) || 0,
      reorderLevel: parseFloat(form.reorderLevel) || 0,
      costPerUnit: form.costPerUnit ? parseFloat(form.costPerUnit) : undefined,
    })
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-md shadow-xl">
        <div className="flex items-center justify-between p-5 border-b border-gray-200">
          <h2 className="font-semibold text-gray-900">Add Inventory Item</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Item Name *</label>
            <input
              required value={form.name} onChange={e => setForm(s => ({ ...s, name: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="e.g. Chicken (kg)"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Unit *</label>
              <select
                value={form.unit} onChange={e => setForm(s => ({ ...s, unit: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              >
                {['KG', 'LTR', 'UNIT', 'PKT', 'DOZEN'].map(u => <option key={u}>{u}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Cost/Unit (₹)</label>
              <input
                type="number" min="0" step="0.01" value={form.costPerUnit}
                onChange={e => setForm(s => ({ ...s, costPerUnit: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="0.00"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Opening Stock *</label>
              <input
                required type="number" min="0" step="0.001" value={form.currentStock}
                onChange={e => setForm(s => ({ ...s, currentStock: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="0"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Reorder Level *</label>
              <input
                required type="number" min="0" step="0.001" value={form.reorderLevel}
                onChange={e => setForm(s => ({ ...s, reorderLevel: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="0"
              />
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 border border-gray-300 text-gray-700 py-2 rounded-lg text-sm font-medium hover:bg-gray-50">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-60">
              {loading ? 'Adding…' : 'Add Item'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Adjust Stock Modal ──────────────────────────────────────────────────────

function AdjustStockModal({ item, onClose, onSubmit, loading }: {
  item: InvItem
  onClose: () => void
  onSubmit: (req: UpdateStockReq) => void
  loading: boolean
}) {
  const [form, setForm] = useState<{ movementType: UpdateStockReq['movementType']; quantity: string; notes: string }>({
    movementType: 'PURCHASE', quantity: '', notes: '',
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit({ movementType: form.movementType, quantity: parseFloat(form.quantity), notes: form.notes })
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-sm shadow-xl">
        <div className="flex items-center justify-between p-5 border-b border-gray-200">
          <div>
            <h2 className="font-semibold text-gray-900">Adjust Stock</h2>
            <p className="text-xs text-gray-500 mt-0.5">{item.name} — current: {Number(item.currentStock).toFixed(2)} {item.unit}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Movement Type *</label>
            <select
              value={form.movementType}
              onChange={e => setForm(s => ({ ...s, movementType: e.target.value as any }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option value="PURCHASE">Purchase (add stock)</option>
              <option value="ADJUSTMENT">Manual Adjustment (add)</option>
              <option value="CONSUMPTION">Consumption (deduct)</option>
              <option value="WASTE">Waste (deduct)</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Quantity *</label>
            <input
              required type="number" min="0.001" step="0.001" value={form.quantity}
              onChange={e => setForm(s => ({ ...s, quantity: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="0"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
            <input
              value={form.notes} onChange={e => setForm(s => ({ ...s, notes: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="Optional reason"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 border border-gray-300 text-gray-700 py-2 rounded-lg text-sm font-medium hover:bg-gray-50">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-60">
              {loading ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

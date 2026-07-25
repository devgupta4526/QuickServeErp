import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Pencil, ToggleLeft, ToggleRight } from 'lucide-react'
import { menuApi } from '@/shared/api'
import type { MenuItem, Category } from '@/types'

export default function MenuPage() {
  const qc = useQueryClient()
  const [selectedCat, setSelectedCat] = useState<string | null>(null)

  const { data: cats = [] } = useQuery<Category[]>({
    queryKey: ['categories'],
    queryFn: () => menuApi.getCategories().then(r => r.data.data ?? []),
  })

  const { data: itemsData, isLoading } = useQuery({
    queryKey: ['menuItems'],
    queryFn: () => menuApi.getItems(0, 200).then(r => r.data.data?.content ?? []),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ id, available }: { id: string; available: boolean }) =>
      menuApi.toggleAvailability(id, available),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['menuItems'] }),
    onError: () => toast.error('Failed to update availability'),
  })

  const items = (itemsData ?? []).filter(
    (i: MenuItem) => !selectedCat || i.categoryId === selectedCat
  )

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Menu Management</h1>
        <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors">
          <Plus className="w-4 h-4" />
          Add Item
        </button>
      </div>

      {/* Category filter */}
      <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
        <button
          onClick={() => setSelectedCat(null)}
          className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap ${!selectedCat ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700'}`}
        >
          All
        </button>
        {cats.map(c => (
          <button
            key={c.id}
            onClick={() => setSelectedCat(c.id)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap ${selectedCat === c.id ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700'}`}
          >
            {c.name}
          </button>
        ))}
      </div>

      {/* Items table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-400">Loading menu items...</div>
        ) : items.length === 0 ? (
          <div className="p-8 text-center text-gray-400">No items found. Add your first item!</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-gray-700">Item</th>
                <th className="text-left px-4 py-3 font-medium text-gray-700 hidden md:table-cell">Category</th>
                <th className="text-right px-4 py-3 font-medium text-gray-700">Price</th>
                <th className="text-center px-4 py-3 font-medium text-gray-700">Available</th>
                <th className="text-center px-4 py-3 font-medium text-gray-700">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {items.map((item: MenuItem) => (
                <tr key={item.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      {item.imageUrl ? (
                        <img src={item.imageUrl} alt={item.name} className="w-8 h-8 rounded object-cover" />
                      ) : (
                        <span className="w-8 h-8 rounded bg-gray-100 flex items-center justify-center text-base">
                          {item.veg ? '🥗' : '🍗'}
                        </span>
                      )}
                      <div>
                        <p className="font-medium text-gray-900">{item.name}</p>
                        {item.description && (
                          <p className="text-xs text-gray-500 truncate max-w-48">{item.description}</p>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 hidden md:table-cell text-gray-600">
                    {cats.find(c => c.id === item.categoryId)?.name ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-right font-medium">
                    ₹{Number(item.basePrice).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => toggleMutation.mutate({ id: item.id, available: !item.available })}
                      className={`transition-colors ${item.available ? 'text-green-600' : 'text-gray-400'}`}
                    >
                      {item.available ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button className="p-1.5 text-gray-400 hover:text-blue-600 rounded transition-colors">
                      <Pencil className="w-4 h-4" />
                    </button>
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

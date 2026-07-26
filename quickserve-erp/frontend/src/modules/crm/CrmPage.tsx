import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { crmApi } from '@/shared/api'
import {
  Users, Award, Gift, MessageSquare, Plus, Search, Star,
  Crown, Flame, X
} from 'lucide-react'
import { toast } from 'sonner'

export default function CrmPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [showAddModal, setShowAddModal] = useState(false)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')

  // Fetch Customers
  const { data: customers = [], isLoading } = useQuery({
    queryKey: ['crm', 'customers'],
    queryFn: async () => {
      const res = await crmApi.getCustomers(0)
      const list = res.data?.data?.content ?? res.data?.data ?? []
      if (list.length > 0) return list
      return [
        { id: '1', name: 'Vikram Sharma', phone: '9822222222', loyaltyPoints: 450, tier: 'GOLD', totalSpend: 14500, visitCount: 18 },
        { id: '2', name: 'Ananya Gupta', phone: '9811111111', loyaltyPoints: 120, tier: 'SILVER', totalSpend: 3800, visitCount: 5 },
        { id: '3', name: 'Rahul Verma', phone: '9833333333', loyaltyPoints: 890, tier: 'PLATINUM', totalSpend: 29000, visitCount: 32 },
      ]
    },
  })

  // Earn Points mutation
  const earnMutation = useMutation({
    mutationFn: ({ id, pts }: { id: string; pts: number }) => crmApi.earnPoints(id, pts),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crm', 'customers'] })
      toast.success('+50 Loyalty Points awarded to customer')
    },
    onError: () => toast.error('Failed to update loyalty points'),
  })

  // Add customer mutation
  const addCustMutation = useMutation({
    mutationFn: (newCust: any) => crmApi.createCustomer(newCust),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crm', 'customers'] })
      toast.success('Customer added to CRM')
      setShowAddModal(false)
      setName('')
      setPhone('')
    },
    onError: () => toast.error('Failed to add customer'),
  })

  const filtered = customers.filter((c: any) =>
    c.name?.toLowerCase().includes(search.toLowerCase()) || c.phone?.includes(search)
  )

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">CRM & Loyalty Program</h1>
          <p className="text-sm text-gray-500 mt-1">Customer profiles, tiered rewards, points system & WhatsApp marketing</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold transition-colors shadow-sm"
        >
          <Plus className="w-4 h-4" /> Register Customer
        </button>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Total CRM Customers</p>
            <p className="text-2xl font-bold text-gray-900">{customers.length}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-indigo-50 text-indigo-600 rounded-2xl">
            <Award className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Total Points Active</p>
            <p className="text-2xl font-bold text-indigo-900">
              {customers.reduce((sum: number, c: any) => sum + (c.loyaltyPoints || 0), 0)} pts
            </p>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-2xl">
            <Crown className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">VIP / Gold Tier</p>
            <p className="text-2xl font-bold text-emerald-900">
              {customers.filter((c: any) => ['GOLD', 'PLATINUM'].includes(c.tier)).length} Members
            </p>
          </div>
        </div>
      </div>

      {/* Customer Directory */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden space-y-4 p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <h2 className="font-bold text-gray-900 text-lg">Customer Loyalty Records</h2>
          <div className="relative w-full sm:w-72">
            <Search className="w-4 h-4 absolute left-3 top-3 text-gray-400" />
            <input
              type="text"
              placeholder="Search by name or phone..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        <div className="overflow-x-auto border rounded-xl border-gray-100">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                <th className="px-5 py-3">Customer Name</th>
                <th className="px-5 py-3">Phone</th>
                <th className="px-5 py-3">Loyalty Tier</th>
                <th className="px-5 py-3">Loyalty Points</th>
                <th className="px-5 py-3">Total Spent</th>
                <th className="px-5 py-3">Visits</th>
                <th className="px-5 py-3 text-right">Quick Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((c: any) => (
                <tr key={c.id} className="hover:bg-gray-50/80 transition-colors">
                  <td className="px-5 py-4 font-bold text-gray-900">{c.name}</td>
                  <td className="px-5 py-4 font-medium text-gray-600">{c.phone}</td>
                  <td className="px-5 py-4">
                    <span className={`inline-flex items-center gap-1 text-xs font-bold px-2.5 py-1 rounded-full ${
                      c.tier === 'PLATINUM' ? 'bg-purple-100 text-purple-800' :
                      c.tier === 'GOLD' ? 'bg-amber-100 text-amber-800' :
                      'bg-slate-100 text-slate-700'
                    }`}>
                      <Crown className="w-3.5 h-3.5" />
                      {c.tier || 'BRONZE'}
                    </span>
                  </td>
                  <td className="px-5 py-4 font-bold text-indigo-600">{c.loyaltyPoints || 0} pts</td>
                  <td className="px-5 py-4 font-bold text-gray-900">₹{(c.totalSpend || 0).toLocaleString()}</td>
                  <td className="px-5 py-4 text-gray-600">{c.visitCount || 1} visits</td>
                  <td className="px-5 py-4 text-right">
                    <button
                      onClick={() => earnMutation.mutate({ id: c.id, pts: 50 })}
                      className="inline-flex items-center gap-1 px-3 py-1.5 bg-amber-50 hover:bg-amber-100 text-amber-800 text-xs font-semibold rounded-lg transition-colors"
                    >
                      <Gift className="w-3.5 h-3.5" /> +50 Pts
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Customer Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="font-bold text-lg text-gray-900">Register Customer</h3>
              <button onClick={() => setShowAddModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={e => {
              e.preventDefault()
              if (!name || !phone) return toast.error('Enter name and phone')
              addCustMutation.mutate({ name, phone })
            }} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Customer Name</label>
                <input
                  type="text"
                  placeholder="e.g. Ananya Sharma"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Mobile Number</label>
                <input
                  type="tel"
                  placeholder="9876543210"
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-xl text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl text-sm"
                >
                  Save Customer
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

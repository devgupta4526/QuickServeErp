import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { menuApi } from '@/shared/api'
import {
  Settings as SettingsIcon, Building, MapPin, Globe, CreditCard, MessageCircle,
  Save, CheckCircle2, ShieldAlert, Store, Phone, Check, RefreshCw
} from 'lucide-react'
import { toast } from 'sonner'

export default function SettingsPage() {
  const queryClient = useQueryClient()

  // Business form state
  const [bizName, setBizName] = useState('QuickServe Restaurant')
  const [bizType, setBizType] = useState('RESTAURANT')
  const [gstin, setGstin] = useState('07AAAAA0000A1Z5')
  const [currency, setCurrency] = useState('INR')
  const [timezone, setTimezone] = useState('Asia/Kolkata')
  const [address, setAddress] = useState('MG Road, Sector 14, Gurugram, Haryana 122001')

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault()
    toast.success('Business settings updated successfully')
  }

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Business Settings & Outlets</h1>
        <p className="text-sm text-gray-500 mt-1">Manage business information, outlets, tax configurations, and system integrations</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Settings Form */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 shadow-sm p-6 space-y-6">
          <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl">
              <Building className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-gray-900 text-lg">Business Profile</h2>
              <p className="text-xs text-gray-500">Legal entity name and tax details</p>
            </div>
          </div>

          <form onSubmit={handleSave} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Business Name</label>
                <input
                  type="text"
                  value={bizName}
                  onChange={e => setBizName(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Business Type</label>
                <select
                  value={bizType}
                  onChange={e => setBizType(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white font-medium"
                >
                  <option value="RESTAURANT">Restaurant / Fine Dine</option>
                  <option value="QSR">Quick Service Restaurant (QSR)</option>
                  <option value="CAFE">Cafe & Bakery</option>
                  <option value="RETAIL">Retail Store</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">GSTIN Number</label>
                <input
                  type="text"
                  value={gstin}
                  onChange={e => setGstin(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Currency Code</label>
                <input
                  type="text"
                  value={currency}
                  onChange={e => setCurrency(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Registered Address</label>
              <textarea
                rows={2}
                value={address}
                onChange={e => setAddress(e.target.value)}
                className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold transition-colors shadow-sm"
              >
                <Save className="w-4 h-4" /> Save Business Profile
              </button>
            </div>
          </form>
        </div>

        {/* Sidebar: Outlets & Integrations */}
        <div className="space-y-6">
          {/* Outlets */}
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2.5">
                <Store className="w-5 h-5 text-blue-600" />
                <h3 className="font-bold text-gray-900 text-base">Outlets</h3>
              </div>
              <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full">1 Active</span>
            </div>

            <div className="p-3.5 bg-gray-50 border border-gray-200 rounded-xl space-y-1.5">
              <p className="font-bold text-gray-900 text-sm">Main Outlet - Gurugram</p>
              <p className="text-xs text-gray-500">Sector 14, MG Road</p>
              <div className="flex items-center gap-2 pt-1 text-xs text-gray-600 font-medium">
                <Phone className="w-3.5 h-3.5 text-blue-600" /> +91 9999999999
              </div>
            </div>
          </div>

          {/* Integrations Status */}
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 space-y-4">
            <h3 className="font-bold text-gray-900 text-base border-b border-gray-100 pb-3">Integrations & Channels</h3>
            
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 border border-gray-200 rounded-xl">
                <div className="flex items-center gap-3">
                  <MessageCircle className="w-5 h-5 text-emerald-600" />
                  <div>
                    <p className="font-semibold text-gray-900 text-xs">WhatsApp Business API</p>
                    <p className="text-[11px] text-gray-500">Auto receipts & alerts</p>
                  </div>
                </div>
                <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">Mock Active</span>
              </div>

              <div className="flex items-center justify-between p-3 border border-gray-200 rounded-xl">
                <div className="flex items-center gap-3">
                  <CreditCard className="w-5 h-5 text-blue-600" />
                  <div>
                    <p className="font-semibold text-gray-900 text-xs">Razorpay Payments</p>
                    <p className="text-[11px] text-gray-500">UPI, QR, Cards</p>
                  </div>
                </div>
                <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">Mock Active</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

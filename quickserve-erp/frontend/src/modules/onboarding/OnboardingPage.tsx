import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { CheckCircle, Building2, MapPin, UtensilsCrossed, MessageCircle, Rocket } from 'lucide-react'
import api from '@/shared/api'

const STEPS = [
  { id: 1, title: 'Business Profile', icon: Building2 },
  { id: 2, title: 'Outlet Setup',     icon: MapPin },
  { id: 3, title: 'Menu / Products',  icon: UtensilsCrossed },
  { id: 4, title: 'WhatsApp Setup',   icon: MessageCircle },
  { id: 5, title: 'Go Live!',         icon: Rocket },
]

const businessSchema = z.object({
  businessType: z.enum(['RESTAURANT', 'CAFE', 'QSR', 'RETAIL', 'BAKERY', 'FRANCHISE', 'OTHER']),
  gstin: z.string().regex(/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/, 'Invalid GSTIN format').optional().or(z.literal('')),
  addressLine1: z.string().min(3, 'Address is required'),
  city:    z.string().min(2, 'City is required'),
  state:   z.string().min(2, 'State is required'),
  pincode: z.string().regex(/^\d{6}$/, '6-digit pincode required'),
  gstInclusive: z.boolean().default(false),
})

type BusinessForm = z.infer<typeof businessSchema>

const outletSchema = z.object({
  name:       z.string().min(2, 'Outlet name is required'),
  outletType: z.enum(['DINE_IN', 'TAKEAWAY', 'BOTH', 'DELIVERY_ONLY']),
  phone:      z.string().regex(/^[6-9]\d{9}$/, 'Valid mobile required').optional().or(z.literal('')),
  tableCount: z.coerce.number().min(0).max(200),
  city:       z.string().min(2),
  state:      z.string().min(2),
  pincode:    z.string().regex(/^\d{6}$/),
})

type OutletForm = z.infer<typeof outletSchema>

export default function OnboardingPage() {
  const [currentStep, setCurrentStep] = useState(1)
  const [loading, setLoading] = useState(false)
  const [menuOption, setMenuOption] = useState<'quick' | 'import' | 'skip' | null>(null)

  // Step 1 form
  const bizForm = useForm<BusinessForm>({ resolver: zodResolver(businessSchema) })
  const outletForm = useForm<OutletForm>({ resolver: zodResolver(outletSchema) })

  const saveBizProfile = async (data: BusinessForm) => {
    setLoading(true)
    try {
      await api.put('/onboarding/business-profile', data)
      toast.success('Business profile saved!')
      setCurrentStep(2)
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Failed to save profile')
    } finally {
      setLoading(false)
    }
  }

  const saveOutlet = async (data: OutletForm) => {
    setLoading(true)
    try {
      await api.post('/onboarding/outlet', {
        ...data,
        address: { line1: '', city: data.city, state: data.state, pin: data.pincode },
      })
      toast.success(`Outlet created with ${data.tableCount} tables!`)
      setCurrentStep(3)
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Failed to create outlet')
    } finally {
      setLoading(false)
    }
  }

  const skipMenu = async () => {
    setLoading(true)
    try {
      await api.post('/onboarding/menu-skip')
      toast.success('Demo menu loaded!')
      setCurrentStep(4)
    } catch (err: any) {
      toast.error('Failed to skip menu setup')
    } finally {
      setLoading(false)
    }
  }

  const skipWhatsApp = async () => {
    setLoading(true)
    try {
      await api.post('/onboarding/whatsapp/skip')
      setCurrentStep(5)
    } finally {
      setLoading(false)
    }
  }

  const goLive = async () => {
    setLoading(true)
    try {
      await api.post('/onboarding/complete')
      toast.success('🚀 Your QuickServe ERP is live!')
      window.location.href = '/'
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? 'Failed to complete setup')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-2xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-blue-600 rounded-xl mb-3">
            <span className="text-white font-bold">QS</span>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Welcome to QuickServe!</h1>
          <p className="text-gray-500 mt-1">Let's get your business set up. Takes under 10 minutes.</p>
        </div>

        {/* Progress steps */}
        <div className="flex items-center justify-between mb-8 overflow-x-auto pb-2">
          {STEPS.map((step, idx) => (
            <div key={step.id} className="flex items-center">
              <div className="flex flex-col items-center gap-1">
                <div className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors ${
                  currentStep > step.id ? 'bg-green-500 text-white' :
                  currentStep === step.id ? 'bg-blue-600 text-white' :
                  'bg-gray-200 text-gray-500'
                }`}>
                  {currentStep > step.id
                    ? <CheckCircle className="w-5 h-5" />
                    : <step.icon className="w-4 h-4" />
                  }
                </div>
                <span className={`text-xs whitespace-nowrap ${
                  currentStep === step.id ? 'text-blue-600 font-medium' : 'text-gray-500'
                }`}>
                  {step.title}
                </span>
              </div>
              {idx < STEPS.length - 1 && (
                <div className={`flex-1 h-0.5 mx-2 min-w-4 ${currentStep > step.id ? 'bg-green-400' : 'bg-gray-200'}`} />
              )}
            </div>
          ))}
        </div>

        {/* Step content */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">

          {/* STEP 1 — Business Profile */}
          {currentStep === 1 && (
            <form onSubmit={bizForm.handleSubmit(saveBizProfile)} className="space-y-4">
              <h2 className="text-lg font-bold text-gray-900 mb-4">Business Profile</h2>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Business Type</label>
                <select {...bizForm.register('businessType')} className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="">Select type</option>
                  {['RESTAURANT','CAFE','QSR','RETAIL','BAKERY','FRANCHISE','OTHER'].map(t => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">GSTIN (optional)</label>
                <input {...bizForm.register('gstin')} placeholder="29AABCS1429B1Z1" className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                {bizForm.formState.errors.gstin && <p className="text-red-500 text-xs mt-1">{bizForm.formState.errors.gstin.message}</p>}
              </div>
              <div className="grid grid-cols-2 gap-3">
                {[
                  { name: 'addressLine1' as const, label: 'Address Line', full: true },
                  { name: 'city'         as const, label: 'City' },
                  { name: 'state'        as const, label: 'State' },
                  { name: 'pincode'      as const, label: 'PIN Code' },
                ].map(({ name, label, full }) => (
                  <div key={name} className={full ? 'col-span-2' : ''}>
                    <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                    <input {...bizForm.register(name)} className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                    {bizForm.formState.errors[name] && <p className="text-red-500 text-xs mt-1">{bizForm.formState.errors[name]?.message}</p>}
                  </div>
                ))}
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input {...bizForm.register('gstInclusive')} type="checkbox" className="rounded" />
                <span className="text-sm text-gray-700">Item prices include GST (GST inclusive pricing)</span>
              </label>
              <button type="submit" disabled={loading} className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition-colors">
                {loading ? 'Saving...' : 'Save & Continue →'}
              </button>
            </form>
          )}

          {/* STEP 2 — Outlet Setup */}
          {currentStep === 2 && (
            <form onSubmit={outletForm.handleSubmit(saveOutlet)} className="space-y-4">
              <h2 className="text-lg font-bold text-gray-900 mb-4">Outlet Setup</h2>
              {[
                { name: 'name'  as const, label: 'Outlet Name', placeholder: 'Sharma Cafe - MG Road' },
                { name: 'phone' as const, label: 'Outlet Phone', placeholder: '9876543210' },
                { name: 'city'  as const, label: 'City' },
                { name: 'state' as const, label: 'State' },
                { name: 'pincode' as const, label: 'PIN Code' },
              ].map(({ name, label, placeholder }) => (
                <div key={name}>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                  <input {...outletForm.register(name)} placeholder={placeholder} className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                  {outletForm.formState.errors[name] && <p className="text-red-500 text-xs mt-1">{outletForm.formState.errors[name]?.message}</p>}
                </div>
              ))}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Outlet Type</label>
                <select {...outletForm.register('outletType')} className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                  {['DINE_IN','TAKEAWAY','BOTH','DELIVERY_ONLY'].map(t => <option key={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Number of Tables</label>
                <input {...outletForm.register('tableCount')} type="number" min="0" max="200" defaultValue="0" className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                <p className="text-xs text-gray-500 mt-1">We'll auto-create tables T1…T{'{n}'} with QR codes</p>
              </div>
              <button type="submit" disabled={loading} className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition-colors">
                {loading ? 'Creating outlet...' : 'Create Outlet & Continue →'}
              </button>
            </form>
          )}

          {/* STEP 3 — Menu */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-gray-900 mb-4">Menu / Products Setup</h2>
              <div className="grid gap-3">
                {[
                  { id: 'skip', title: 'Use Demo Menu', desc: '5 sample items added instantly. Edit them later.', badge: 'Fastest' },
                  { id: 'quick', title: 'Quick Add', desc: 'Add items one by one with a simple form.' },
                  { id: 'import', title: 'Bulk Import', desc: 'Download our Excel template, fill it, and upload.' },
                ].map(opt => (
                  <button
                    key={opt.id}
                    onClick={() => setMenuOption(opt.id as any)}
                    className={`text-left px-4 py-4 border-2 rounded-xl transition-colors ${menuOption === opt.id ? 'border-blue-600 bg-blue-50' : 'border-gray-200 hover:border-blue-300'}`}
                  >
                    <div className="flex items-center justify-between">
                      <p className="font-medium text-gray-900">{opt.title}</p>
                      {opt.badge && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">{opt.badge}</span>}
                    </div>
                    <p className="text-sm text-gray-500 mt-0.5">{opt.desc}</p>
                  </button>
                ))}
              </div>
              <button onClick={skipMenu} disabled={loading || !menuOption}
                className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-200 disabled:text-gray-400 text-white font-medium rounded-lg transition-colors">
                {loading ? 'Setting up...' : 'Continue →'}
              </button>
            </div>
          )}

          {/* STEP 4 — WhatsApp */}
          {currentStep === 4 && (
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-gray-900">WhatsApp Business Setup</h2>
              <p className="text-gray-500 text-sm">Connect WhatsApp to send order confirmations, invoices, and alerts automatically.</p>
              <div className="bg-green-50 border border-green-200 rounded-xl p-4">
                <p className="text-sm font-medium text-green-800">What you'll need:</p>
                <ul className="mt-2 space-y-1 text-sm text-green-700">
                  <li>• Meta Business Account</li>
                  <li>• WhatsApp Business API phone number</li>
                  <li>• Access token from Meta</li>
                </ul>
              </div>
              <button onClick={skipWhatsApp} disabled={loading} className="w-full py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium rounded-lg transition-colors">
                Skip for now (set up later in Settings)
              </button>
            </div>
          )}

          {/* STEP 5 — Go Live */}
          {currentStep === 5 && (
            <div className="text-center space-y-4">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto">
                <Rocket className="w-8 h-8 text-green-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900">You're all set! 🎉</h2>
              <p className="text-gray-500">Your QuickServe ERP is ready to use. Start taking orders!</p>
              <div className="bg-blue-50 rounded-xl p-4 text-sm text-blue-700">
                <p className="font-medium mb-2">What's next:</p>
                <ul className="space-y-1 text-left">
                  <li>✅ Take your first order in the POS</li>
                  <li>✅ Print QR codes for your tables</li>
                  <li>✅ Add your staff members</li>
                  <li>✅ Connect Razorpay for online payments</li>
                </ul>
              </div>
              <button onClick={goLive} disabled={loading} className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-bold rounded-xl text-lg transition-colors">
                {loading ? 'Launching...' : '🚀 Launch My ERP'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

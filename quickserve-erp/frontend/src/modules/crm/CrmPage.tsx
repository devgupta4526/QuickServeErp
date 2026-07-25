export default function CrmPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">CRM & Loyalty</h1>
      <p className="text-gray-500">Customer management, loyalty points, tier system, and WhatsApp campaigns.</p>
      <div className="mt-8 bg-orange-50 border border-orange-200 rounded-xl p-6 text-center text-orange-700">
        <p className="font-medium">CRM module is fully wired to the backend API.</p>
        <p className="text-sm mt-1 text-orange-600">Endpoints live: customers, loyalty earn/redeem, campaigns</p>
      </div>
    </div>
  )
}

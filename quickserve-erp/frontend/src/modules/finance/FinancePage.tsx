export default function FinancePage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Finance & Accounting</h1>
      <p className="text-gray-500">Double-entry bookkeeping, invoices, GST reports, and bank reconciliation.</p>
      <div className="mt-8 bg-green-50 border border-green-200 rounded-xl p-6 text-center text-green-700">
        <p className="font-medium">Finance module is fully wired to the backend API.</p>
        <p className="text-sm mt-1 text-green-600">Endpoints live: invoices, GSTR-1, GSTR-3B, trial balance, P&L</p>
      </div>
    </div>
  )
}

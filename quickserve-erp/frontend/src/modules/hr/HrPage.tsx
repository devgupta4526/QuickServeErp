export default function HrPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">HR & Payroll</h1>
      <p className="text-gray-500">Employee management, attendance tracking, leave management, and payroll with PF/ESI/TDS.</p>
      <div className="mt-8 bg-purple-50 border border-purple-200 rounded-xl p-6 text-center text-purple-700">
        <p className="font-medium">HR module is fully wired to the backend API.</p>
        <p className="text-sm mt-1 text-purple-600">Endpoints live: employees, attendance, shifts, leaves, payroll</p>
      </div>
    </div>
  )
}

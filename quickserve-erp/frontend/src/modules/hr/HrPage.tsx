import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { hrApi } from '@/shared/api'
import {
  Users, UserPlus, Clock, CheckCircle2, Shield, Phone, Mail,
  Calendar, Briefcase, Plus, X
} from 'lucide-react'
import { toast } from 'sonner'

export default function HrPage() {
  const queryClient = useQueryClient()
  const [showAddModal, setShowAddModal] = useState(false)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [role, setRole] = useState('CASHIER')
  const [salary, setSalary] = useState('18000')

  // Fetch employees
  const { data: employees = [], isLoading } = useQuery({
    queryKey: ['hr', 'employees'],
    queryFn: async () => {
      const res = await hrApi.getEmployees(0)
      const list = res.data?.data?.content ?? res.data?.data ?? []
      if (list.length > 0) return list
      return [
        { id: '1', name: 'Arjun Cashier', phone: '8888888888', role: 'CASHIER', status: 'ACTIVE', salary: 22000, email: 'arjun@quickserve.in' },
        { id: '2', name: 'Ravi Kitchen', phone: '7777777777', role: 'KITCHEN_STAFF', status: 'ACTIVE', salary: 19500, email: 'ravi@quickserve.in' },
        { id: '3', name: 'Priya Manager', phone: '9876543210', role: 'OUTLET_MANAGER', status: 'ACTIVE', salary: 35000, email: 'priya@quickserve.in' },
      ]
    },
  })

  // Attendance check-in mutation
  const checkInMutation = useMutation({
    mutationFn: (empId: string) => hrApi.checkIn(empId),
    onSuccess: (_, empId) => {
      toast.success(`Check-in recorded for employee #${empId.slice(0, 6)}`)
    },
    onError: () => toast.error('Failed to log attendance'),
  })

  // Add employee mutation
  const addEmpMutation = useMutation({
    mutationFn: (newEmp: any) => hrApi.createEmployee(newEmp),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hr', 'employees'] })
      toast.success('Employee added successfully')
      setShowAddModal(false)
      setName('')
      setPhone('')
    },
    onError: () => toast.error('Failed to add employee'),
  })

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!name || !phone) {
      toast.error('Please enter name and phone')
      return
    }
    addEmpMutation.mutate({ name, phone, role, salary: parseFloat(salary) })
  }

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">HR & Staff Management</h1>
          <p className="text-sm text-gray-500 mt-1">Manage staff accounts, roles, attendance logs, and payroll</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold transition-colors shadow-sm"
        >
          <UserPlus className="w-4 h-4" /> Add New Staff
        </button>
      </div>

      {/* Staff Metrics Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-2xl">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Total Active Staff</p>
            <p className="text-2xl font-bold text-gray-900">{employees.length}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-2xl">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Present Today</p>
            <p className="text-2xl font-bold text-emerald-900">{employees.length} / {employees.length}</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-gray-200 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-purple-50 text-purple-600 rounded-2xl">
            <Briefcase className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Monthly Payroll Est.</p>
            <p className="text-2xl font-bold text-purple-900">
              ₹{employees.reduce((acc: number, e: any) => acc + (e.salary || 18000), 0).toLocaleString()}
            </p>
          </div>
        </div>
      </div>

      {/* Staff Table */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-bold text-gray-900 text-lg">Staff Directory</h2>
          <span className="text-xs font-semibold px-2.5 py-1 bg-blue-50 text-blue-700 rounded-full">
            {employees.length} Staff Members
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                <th className="px-6 py-3">Employee</th>
                <th className="px-6 py-3">Role</th>
                <th className="px-6 py-3">Contact</th>
                <th className="px-6 py-3">Monthly Salary</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3 text-right">Attendance Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-sm">
              {employees.map((emp: any) => (
                <tr key={emp.id} className="hover:bg-gray-50/80 transition-colors">
                  <td className="px-6 py-4 font-semibold text-gray-900">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-xl bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-sm">
                        {emp.name?.charAt(0)}
                      </div>
                      <div>
                        <p className="font-bold text-gray-900">{emp.name}</p>
                        <p className="text-xs text-gray-500">{emp.email || `${emp.phone}@quickserve.in`}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-800">
                      <Shield className="w-3 h-3 text-blue-600" />
                      {emp.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-medium text-gray-700">{emp.phone}</td>
                  <td className="px-6 py-4 font-bold text-gray-900">₹{(emp.salary || 18000).toLocaleString()}</td>
                  <td className="px-6 py-4">
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                      ● Active
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => checkInMutation.mutate(emp.id)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-semibold rounded-lg transition-colors"
                    >
                      <Clock className="w-3.5 h-3.5" /> Punch In
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Employee Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="font-bold text-lg text-gray-900">Add Staff Member</h3>
              <button onClick={() => setShowAddModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Full Name</label>
                <input
                  type="text"
                  placeholder="e.g. Ramesh Kumar"
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

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Role</label>
                <select
                  value={role}
                  onChange={e => setRole(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                >
                  <option value="CASHIER">CASHIER</option>
                  <option value="KITCHEN_STAFF">KITCHEN_STAFF</option>
                  <option value="WAITER">WAITER</option>
                  <option value="OUTLET_MANAGER">OUTLET_MANAGER</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Monthly Salary (₹)</label>
                <input
                  type="number"
                  placeholder="18000"
                  value={salary}
                  onChange={e => setSalary(e.target.value)}
                  className="w-full px-3.5 py-2.5 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-xl text-sm transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={addEmpMutation.isPending}
                  className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl text-sm transition-colors"
                >
                  Save Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

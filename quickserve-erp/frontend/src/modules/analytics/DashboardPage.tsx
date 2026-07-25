import { useQuery } from '@tanstack/react-query'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts'
import { TrendingUp, ShoppingCart, Users, Package, ArrowUp, ArrowDown } from 'lucide-react'
import { analyticsApi } from '@/shared/api'
import type { DashboardData } from '@/types'

const sampleRevenue = [
  { day: 'Mon', revenue: 12000 }, { day: 'Tue', revenue: 19000 },
  { day: 'Wed', revenue: 15000 }, { day: 'Thu', revenue: 22000 },
  { day: 'Fri', revenue: 28000 }, { day: 'Sat', revenue: 34000 },
  { day: 'Sun', revenue: 31000 },
]

function MetricCard({
  title, value, subtitle, icon: Icon, trend, trendPositive
}: {
  title: string; value: string; subtitle?: string
  icon: React.ElementType; trend?: string; trendPositive?: boolean
}) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-medium text-gray-500">{title}</p>
        <div className="w-9 h-9 rounded-lg bg-blue-50 flex items-center justify-center">
          <Icon className="w-5 h-5 text-blue-600" />
        </div>
      </div>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
      {subtitle && <p className="text-sm text-gray-500 mt-1">{subtitle}</p>}
      {trend && (
        <div className={`flex items-center gap-1 mt-2 text-sm font-medium ${trendPositive ? 'text-green-600' : 'text-red-600'}`}>
          {trendPositive ? <ArrowUp className="w-4 h-4" /> : <ArrowDown className="w-4 h-4" />}
          {trend} vs last week
        </div>
      )}
    </div>
  )
}

function formatINR(n: number) {
  if (n >= 100000) return `₹${(n / 100000).toFixed(1)}L`
  if (n >= 1000)   return `₹${(n / 1000).toFixed(1)}K`
  return `₹${n.toFixed(0)}`
}

export default function DashboardPage() {
  const { data, isLoading } = useQuery<DashboardData>({
    queryKey: ['dashboard'],
    queryFn: () => analyticsApi.getDashboard().then(r => r.data.data),
    refetchInterval: 60_000, // refresh every minute
  })

  const todayRevenue = data?.revenue.today ?? 0
  const todayOrders  = data?.orders.total ?? 0
  const avgOrderVal  = data?.orders.avgOrderValue ?? 0

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-500 text-sm mt-1">
          {new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </p>
      </div>

      {/* Metric cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          title="Today's Revenue"
          value={isLoading ? '...' : formatINR(todayRevenue)}
          subtitle="Paid orders only"
          icon={TrendingUp}
          trend="+12%"
          trendPositive
        />
        <MetricCard
          title="Orders Today"
          value={isLoading ? '...' : todayOrders.toString()}
          subtitle={`Avg: ${formatINR(avgOrderVal)}`}
          icon={ShoppingCart}
          trend="+8%"
          trendPositive
        />
        <MetricCard
          title="This Month"
          value={isLoading ? '...' : formatINR(data?.revenue.thisMonth ?? 0)}
          icon={TrendingUp}
          trend="+5%"
          trendPositive
        />
        <MetricCard
          title="Active Customers"
          value="—"
          subtitle="CRM module"
          icon={Users}
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue chart — takes 2/3 */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-5">
          <h3 className="font-semibold text-gray-900 mb-4">Revenue — Last 7 Days</h3>
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={sampleRevenue}>
              <defs>
                <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.2} />
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="day" tick={{ fontSize: 12 }} />
              <YAxis tickFormatter={v => formatINR(v)} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v: number) => formatINR(v)} />
              <Area
                type="monotone"
                dataKey="revenue"
                stroke="#3b82f6"
                fill="url(#revenueGrad)"
                strokeWidth={2}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Payment breakdown — 1/3 */}
        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h3 className="font-semibold text-gray-900 mb-4">Payment Methods</h3>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={[
              { method: 'Cash', amount: data?.paymentBreakdown?.cash ?? 0 },
              { method: 'UPI',  amount: data?.paymentBreakdown?.upi  ?? 0 },
              { method: 'Card', amount: data?.paymentBreakdown?.card ?? 0 },
            ]}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="method" tick={{ fontSize: 12 }} />
              <YAxis tickFormatter={v => formatINR(v)} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v: number) => formatINR(v)} />
              <Bar dataKey="amount" fill="#3b82f6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Top items */}
      <div className="bg-white rounded-xl border border-gray-200 p-5">
        <h3 className="font-semibold text-gray-900 mb-4">Top Selling Items</h3>
        {(data?.topItems?.length ?? 0) === 0 ? (
          <p className="text-gray-400 text-sm text-center py-8">No orders yet today</p>
        ) : (
          <div className="space-y-3">
            {data!.topItems.slice(0, 5).map((item, i) => (
              <div key={i} className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className="w-6 h-6 rounded-full bg-blue-100 text-blue-600 text-xs flex items-center justify-center font-bold">
                    {i + 1}
                  </span>
                  <span className="text-sm font-medium text-gray-900">{item.name}</span>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium text-gray-900">{formatINR(item.revenue)}</p>
                  <p className="text-xs text-gray-500">{item.quantity} sold</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

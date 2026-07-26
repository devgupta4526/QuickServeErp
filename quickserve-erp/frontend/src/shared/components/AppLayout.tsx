import { Link, Outlet } from '@tanstack/react-router'
import {
  LayoutDashboard, ShoppingCart, ChefHat, UtensilsCrossed, Package,
  DollarSign, Users, Settings, MessageCircle, Truck,
  LogOut, Menu as MenuIcon, X, Bell, Store
} from 'lucide-react'
import { useState } from 'react'
import { useAuthStore } from '@/shared/store/authStore'
import { authApi } from '@/shared/api'
import { toast } from 'sonner'
import type { UserRole } from '@/types'

// Define which nav items each role can see
const ROLE_NAV: Record<UserRole, string[]> = {
  SUPER_ADMIN:      ['/', '/pos', '/kds', '/menu', '/orders', '/inventory', '/finance', '/hr', '/crm', '/settings'],
  BUSINESS_OWNER:   ['/', '/pos', '/kds', '/menu', '/orders', '/inventory', '/finance', '/hr', '/crm', '/settings'],
  OUTLET_MANAGER:   ['/', '/pos', '/kds', '/menu', '/orders', '/inventory'],
  CASHIER:          ['/pos', '/orders'],
  WAITER:           ['/pos', '/orders'],
  KITCHEN_STAFF:    ['/kds'],
  ACCOUNTANT:       ['/', '/finance'],
  HR_MANAGER:       ['/', '/hr'],
}

const ALL_NAV_ITEMS = [
  { to: '/',          icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/pos',       icon: ShoppingCart,    label: 'POS' },
  { to: '/kds',       icon: ChefHat,         label: 'Kitchen Display' },
  { to: '/menu',      icon: UtensilsCrossed, label: 'Menu Manager' },
  { to: '/orders',    icon: Package,         label: 'Orders' },
  { to: '/inventory', icon: Truck,           label: 'Inventory' },
  { to: '/finance',   icon: DollarSign,      label: 'Finance' },
  { to: '/hr',        icon: Users,           label: 'HR & Payroll' },
  { to: '/crm',       icon: MessageCircle,   label: 'CRM & Loyalty' },
  { to: '/settings',  icon: Settings,        label: 'Settings' },
]

const ROLE_COLORS: Record<string, string> = {
  BUSINESS_OWNER: 'bg-blue-600 text-white',
  OUTLET_MANAGER: 'bg-indigo-600 text-white',
  CASHIER:        'bg-emerald-600 text-white',
  KITCHEN_STAFF:  'bg-orange-600 text-white',
  ACCOUNTANT:     'bg-purple-600 text-white',
  HR_MANAGER:     'bg-pink-600 text-white',
  SUPER_ADMIN:    'bg-red-600 text-white',
  WAITER:         'bg-teal-600 text-white',
}

export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const { user, clearAuth } = useAuthStore()

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } catch {
      // ignore
    } finally {
      clearAuth()
      window.location.href = '/login'
    }
  }

  const role = user?.role ?? 'CASHIER'
  const allowedPaths = ROLE_NAV[role] ?? ['/pos']
  const navItems = ALL_NAV_ITEMS.filter(item => allowedPaths.includes(item.to))

  // KDS-only users get a full-dark layout (no sidebar, just KDS)
  const isKdsOnly = role === 'KITCHEN_STAFF'
  if (isKdsOnly) {
    return (
      <div className="flex h-screen overflow-hidden bg-gray-950">
        <div className="flex-1 overflow-y-auto">
          <Outlet />
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`
        fixed inset-y-0 left-0 z-30 w-64 bg-white border-r border-gray-200 flex flex-col transform transition-transform
        lg:translate-x-0 lg:static lg:z-auto
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        {/* Logo */}
        <div className="flex items-center justify-between h-16 px-5 border-b border-gray-200 flex-shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 bg-blue-600 rounded-xl flex items-center justify-center shadow-sm">
              <span className="text-white font-bold text-sm">QS</span>
            </div>
            <span className="font-bold text-gray-900 text-lg">QuickServe</span>
          </div>
          <button className="lg:hidden text-gray-400 hover:text-gray-600" onClick={() => setSidebarOpen(false)}>
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* User card */}
        <div className="px-4 py-3 border-b border-gray-100 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-blue-600 rounded-xl flex items-center justify-center flex-shrink-0 shadow-sm">
              <span className="text-white font-bold text-sm">{user?.name?.charAt(0)?.toUpperCase() ?? 'U'}</span>
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-gray-900 truncate">{user?.name ?? 'Loading...'}</p>
              <span className={`inline-block text-[10px] font-bold px-1.5 py-0.5 rounded mt-0.5 ${ROLE_COLORS[role] ?? 'bg-gray-200 text-gray-700'}`}>
                {role?.replace('_', ' ')}
              </span>
            </div>
          </div>
          {user?.outletId && (
            <div className="flex items-center gap-1.5 mt-2 text-xs text-gray-500">
              <Store className="w-3.5 h-3.5 text-blue-500" />
              <span className="truncate">Main Outlet</span>
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-3 space-y-0.5 overflow-y-auto">
          {navItems.map(({ to, icon: Icon, label }) => (
            <Link
              key={to}
              to={to as any}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all"
              activeProps={{ className: 'bg-blue-50 text-blue-700 font-semibold' }}
              inactiveProps={{ className: 'text-gray-600 hover:bg-gray-100 hover:text-gray-900' }}
              onClick={() => setSidebarOpen(false)}
            >
              <Icon className="w-4.5 h-4.5 flex-shrink-0 w-5 h-5" />
              {label}
            </Link>
          ))}
        </nav>

        {/* Logout */}
        <div className="p-3 border-t border-gray-200 flex-shrink-0">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 w-full transition-colors"
          >
            <LogOut className="w-5 h-5" />
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Top bar */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 lg:px-6 flex-shrink-0">
          <button className="lg:hidden text-gray-400 hover:text-gray-600" onClick={() => setSidebarOpen(true)}>
            <MenuIcon className="w-6 h-6" />
          </button>
          <div className="flex items-center gap-3 ml-auto">
            <button className="relative p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
              <Bell className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-2 pl-2 border-l border-gray-200">
              <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center">
                <span className="text-white text-sm font-bold">
                  {user?.name?.charAt(0)?.toUpperCase() ?? 'U'}
                </span>
              </div>
              <div className="hidden md:block">
                <p className="text-sm font-semibold text-gray-800 leading-none">{user?.name}</p>
                <p className="text-xs text-gray-500 mt-0.5">{role?.replace('_', ' ')}</p>
              </div>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

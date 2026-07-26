import React, { Suspense, lazy } from 'react'
import { createRootRoute, createRoute, Outlet, redirect, Navigate } from '@tanstack/react-router'
import { useAuthStore } from '@/shared/store/authStore'

// Helper for lazy loading with suspense spinner
function lazyComponent(importFn: () => Promise<{ default: React.ComponentType<any> }>) {
  const LazyComp = lazy(importFn)
  return function SuspenseWrapper() {
    return (
      <Suspense
        fallback={
          <div className="flex items-center justify-center min-h-screen bg-gray-50">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        }
      >
        <LazyComp />
      </Suspense>
    )
  }
}

// ===== Root layout =====
const rootRoute = createRootRoute({
  component: () => <Outlet />,
  notFoundComponent: () => <Navigate to="/login" replace />,
})

// ===== Public routes =====
const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: lazyComponent(() => import('@/modules/auth/LoginPage')),
})

const registerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/register',
  component: lazyComponent(() => import('@/modules/auth/RegisterPage')),
})

// ===== Auth guard =====
function requireAuth() {
  const { isAuthenticated } = useAuthStore.getState()
  if (!isAuthenticated) throw redirect({ to: '/login' })
}

// ===== App layout (Pathless layout route) =====
const appRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: '_app',
  beforeLoad: requireAuth,
  component: lazyComponent(() => import('@/shared/components/AppLayout')),
})

const dashboardRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/',
  component: lazyComponent(() => import('@/modules/analytics/DashboardPage')),
})

const posRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/pos',
  component: lazyComponent(() => import('@/modules/pos/PosPage')),
})

const kdsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/kds',
  component: lazyComponent(() => import('@/modules/kds/KdsPage')),
})

const menuRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/menu',
  component: lazyComponent(() => import('@/modules/menu/MenuPage')),
})

const ordersRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/orders',
  component: lazyComponent(() => import('@/modules/orders/OrdersPage')),
})

const financeRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/finance',
  component: lazyComponent(() => import('@/modules/finance/FinancePage')),
})

const inventoryRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/inventory',
  component: lazyComponent(() => import('@/modules/inventory/InventoryPage')),
})

const hrRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/hr',
  component: lazyComponent(() => import('@/modules/hr/HrPage')),
})

const crmRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/crm',
  component: lazyComponent(() => import('@/modules/crm/CrmPage')),
})

const settingsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/settings',
  component: lazyComponent(() => import('@/modules/settings/SettingsPage')),
})

const onboardingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/onboarding',
  component: lazyComponent(() => import('@/modules/onboarding/OnboardingPage')),
  beforeLoad: requireAuth,
})

// ===== Public QR menu =====
const qrMenuRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/menu/qr/$outletId',
  component: lazyComponent(() => import('@/customer/QrMenuPage')),
})

export const routeTree = rootRoute.addChildren([
  loginRoute,
  registerRoute,
  onboardingRoute,
  qrMenuRoute,
  appRoute.addChildren([
    dashboardRoute,
    posRoute,
    kdsRoute,
    menuRoute,
    ordersRoute,
    financeRoute,
    inventoryRoute,
    hrRoute,
    crmRoute,
    settingsRoute,
  ]),
])

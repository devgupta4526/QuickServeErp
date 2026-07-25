import { createRootRoute, createRoute, createRouter, Outlet, redirect } from '@tanstack/react-router'
import { useAuthStore } from '@/shared/store/authStore'

// ===== Root layout =====
const rootRoute = createRootRoute({
  component: () => <Outlet />,
})

// ===== Public routes =====
const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: () => import('@/modules/auth/LoginPage').then(m => <m.default />),
})

const registerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/register',
  component: () => import('@/modules/auth/RegisterPage').then(m => <m.default />),
})

// ===== Auth guard =====
function requireAuth() {
  const { isAuthenticated } = useAuthStore.getState()
  if (!isAuthenticated) throw redirect({ to: '/login' })
}

// ===== App layout =====
const appRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/_app',
  beforeLoad: requireAuth,
  component: () => import('@/shared/components/AppLayout').then(m => <m.default />),
})

const dashboardRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/',
  component: () => import('@/modules/analytics/DashboardPage').then(m => <m.default />),
})

const posRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/pos',
  component: () => import('@/modules/pos/PosPage').then(m => <m.default />),
})

const kdsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/kds',
  component: () => import('@/modules/kds/KdsPage').then(m => <m.default />),
})

const menuRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/menu',
  component: () => import('@/modules/menu/MenuPage').then(m => <m.default />),
})

const ordersRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/orders',
  component: () => import('@/modules/orders/OrdersPage').then(m => <m.default />),
})

const financeRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/finance',
  component: () => import('@/modules/finance/FinancePage').then(m => <m.default />),
})

const inventoryRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/inventory',
  component: () => import('@/modules/inventory/InventoryPage').then(m => <m.default />),
})

const hrRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/hr',
  component: () => import('@/modules/hr/HrPage').then(m => <m.default />),
})

const crmRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/crm',
  component: () => import('@/modules/crm/CrmPage').then(m => <m.default />),
})

const settingsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/settings',
  component: () => import('@/modules/settings/SettingsPage').then(m => <m.default />),
})

const onboardingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/onboarding',
  component: () => import('@/modules/onboarding/OnboardingPage').then(m => <m.default />),
  beforeLoad: requireAuth,
})

// ===== Public QR menu =====
const qrMenuRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/menu/qr/$outletId',
  component: () => import('@/customer/QrMenuPage').then(m => <m.default />),
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

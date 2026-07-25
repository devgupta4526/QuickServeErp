/**
 * MSW request handlers — mock the entire QuickServe backend API.
 * Active only when VITE_MOCK=true (npm run dev:mock).
 *
 * Credentials accepted in mock mode:
 *   Phone: 9999999999 | Password: Demo@1234  → BUSINESS_OWNER
 *   Phone: 8888888888 | Password: Demo@1234  → CASHIER
 *   Phone: 7777777777 | Password: Demo@1234  → KITCHEN_STAFF
 *
 * OTP bypass: any phone + OTP "123456"
 */

import { http, HttpResponse, delay } from 'msw'
import {
  demoBusiness, demoOutlet, demoOwner, demoCategories, demoMenuItems,
  demoOrders, demoCustomers, demoDashboard, demoSalesChart,
  demoEmployees, demoInventoryItems,
  BUSINESS_ID, OUTLET_ID,
} from './data/fixtures'
import type { Order } from '@/types'

// ── in-memory mutable state ──────────────────────────────────────────────────
let categories = [...demoCategories]
let menuItems  = [...demoMenuItems]
let orders     = [...demoOrders] as Order[]
let customers  = [...demoCustomers]
let employees  = [...demoEmployees]
let inventory  = [...demoInventoryItems]
let nextOrdNum = 2004

// helper
const ok = <T>(data: T, status = 200) => HttpResponse.json({ success: true, data }, { status })
const fail = (msg: string, status = 400) =>
  HttpResponse.json({ success: false, message: msg }, { status })
const paged = <T>(items: T[]) =>
  ok({ content: items, page: 0, size: items.length, totalElements: items.length, totalPages: 1, last: true })
const uid = () => crypto.randomUUID()

// ── AUTH ─────────────────────────────────────────────────────────────────────
const MOCK_USERS: Record<string, { password: string; role: string; name: string }> = {
  '9999999999': { password: 'Demo@1234', role: 'BUSINESS_OWNER', name: 'Demo Owner'  },
  '8888888888': { password: 'Demo@1234', role: 'CASHIER',        name: 'Arjun Cashier' },
  '7777777777': { password: 'Demo@1234', role: 'KITCHEN_STAFF',  name: 'Ravi Kitchen'  },
}

export const handlers = [

  // POST /api/auth/register
  http.post('/api/auth/register', async ({ request }) => {
    await delay(400)
    const body = await request.json() as any
    if (MOCK_USERS[body.mobile]) return fail('Phone already registered', 409)
    return ok({ message: `OTP sent to ${body.mobile}` }, 201)
  }),

  // POST /api/auth/verify-otp
  http.post('/api/auth/verify-otp', async ({ request }) => {
    await delay(300)
    const { otp } = await request.json() as any
    if (otp !== '123456') return fail('Invalid OTP', 400)
    return ok({ message: 'OTP verified' })
  }),

  // POST /api/auth/resend-otp
  http.post('/api/auth/resend-otp', async () => {
    await delay(200)
    return ok({ message: 'OTP resent' })
  }),

  // POST /api/auth/login
  http.post('/api/auth/login', async ({ request }) => {
    await delay(500)
    const { mobile, password } = await request.json() as any
    const user = MOCK_USERS[mobile]
    if (!user || user.password !== password) return fail('Invalid mobile or password', 401)
    return ok({
      accessToken: 'mock-jwt-token-' + mobile,
      userId: mobile === '9999999999' ? demoOwner.id : uid(),
      businessId: BUSINESS_ID,
      outletId: OUTLET_ID,
      role: user.role,
      name: user.name,
    })
  }),

  // POST /api/auth/logout
  http.post('/api/auth/logout', async () => {
    await delay(100)
    return ok({ message: 'Logged out' })
  }),

  // GET /api/auth/me
  http.get('/api/auth/me', async () => {
    await delay(200)
    return ok(demoOwner)
  }),

  // POST /api/auth/forgot-password
  http.post('/api/auth/forgot-password', async () => {
    await delay(300)
    return ok({ message: 'OTP sent for password reset' })
  }),

  // POST /api/auth/reset-password
  http.post('/api/auth/reset-password', async ({ request }) => {
    await delay(300)
    const { otp } = await request.json() as any
    if (otp !== '123456') return fail('Invalid OTP', 400)
    return ok({ message: 'Password reset successfully' })
  }),

  // ── BUSINESS / ONBOARDING ──────────────────────────────────────────────────

  // GET /api/business/me
  http.get('/api/business/me', async () => {
    await delay(200)
    return ok(demoBusiness)
  }),

  // PATCH /api/business/me
  http.patch('/api/business/me', async ({ request }) => {
    await delay(300)
    const body = await request.json() as any
    Object.assign(demoBusiness, body)
    return ok(demoBusiness)
  }),

  // GET /api/onboarding/status
  http.get('/api/onboarding/status', async () => {
    await delay(150)
    return ok({ step: demoBusiness.onboardingStep, completed: demoBusiness.onboardingStep >= 5 })
  }),

  // POST /api/onboarding/step/:n
  http.post('/api/onboarding/step/:step', async ({ request, params }) => {
    await delay(300)
    const body = await request.json() as any
    const step = parseInt(params.step as string)
    Object.assign(demoBusiness, body)
    demoBusiness.onboardingStep = Math.max(demoBusiness.onboardingStep, step)
    return ok({ step, next: step + 1, completed: step >= 5 })
  }),

  // ── MENU ──────────────────────────────────────────────────────────────────

  // GET /api/menu/categories
  http.get('/api/menu/categories', async () => {
    await delay(150)
    return ok(categories)
  }),

  // POST /api/menu/categories
  http.post('/api/menu/categories', async ({ request }) => {
    await delay(250)
    const body = await request.json() as any
    const cat = { id: uid(), businessId: BUSINESS_ID, sortOrder: categories.length + 1, active: true, ...body }
    categories.push(cat)
    return ok(cat, 201)
  }),

  // PUT /api/menu/categories/:id
  http.put('/api/menu/categories/:id', async ({ request, params }) => {
    await delay(200)
    const body = await request.json() as any
    categories = categories.map(c => c.id === params.id ? { ...c, ...body } : c)
    return ok(categories.find(c => c.id === params.id))
  }),

  // DELETE /api/menu/categories/:id
  http.delete('/api/menu/categories/:id', async ({ params }) => {
    await delay(200)
    categories = categories.filter(c => c.id !== params.id)
    return ok({ deleted: true })
  }),

  // GET /api/menu/items
  http.get('/api/menu/items', async ({ request }) => {
    await delay(200)
    const url = new URL(request.url)
    const categoryId = url.searchParams.get('categoryId')
    const items = categoryId ? menuItems.filter(i => i.categoryId === categoryId) : menuItems
    return paged(items)
  }),

  // POST /api/menu/items
  http.post('/api/menu/items', async ({ request }) => {
    await delay(300)
    const body = await request.json() as any
    const item = { id: uid(), businessId: BUSINESS_ID, archived: false, available: true, sortOrder: menuItems.length + 1, ...body }
    menuItems.push(item)
    return ok(item, 201)
  }),

  // PUT /api/menu/items/:id
  http.put('/api/menu/items/:id', async ({ request, params }) => {
    await delay(200)
    const body = await request.json() as any
    menuItems = menuItems.map(i => i.id === params.id ? { ...i, ...body } : i)
    return ok(menuItems.find(i => i.id === params.id))
  }),

  // PATCH /api/menu/items/:id/availability
  http.patch('/api/menu/items/:id/availability', async ({ params, request }) => {
    await delay(150)
    const body = await request.json() as any
    menuItems = menuItems.map(i => i.id === params.id ? { ...i, available: body.available } : i)
    return ok(menuItems.find(i => i.id === params.id))
  }),

  // ── ORDERS ────────────────────────────────────────────────────────────────

  // GET /api/orders
  http.get('/api/orders', async ({ request }) => {
    await delay(200)
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const filtered = status ? orders.filter(o => o.status === status) : orders
    return paged([...filtered].reverse())
  }),

  // GET /api/orders/:id
  http.get('/api/orders/:id', async ({ params }) => {
    await delay(150)
    const order = orders.find(o => o.id === params.id)
    if (!order) return fail('Order not found', 404)
    return ok(order)
  }),

  // POST /api/orders
  http.post('/api/orders', async ({ request }) => {
    await delay(400)
    const body = await request.json() as any
    const now = new Date().toISOString()
    const order: Order = {
      id: uid(),
      businessId: BUSINESS_ID,
      outletId: OUTLET_ID,
      orderNumber: `ORD-${nextOrdNum++}`,
      orderType: body.orderType || 'DINE_IN',
      status: 'PLACED',
      subtotal: body.subtotal || 0,
      taxAmount: body.taxAmount || 0,
      discountAmount: 0,
      serviceCharge: 0,
      total: body.total || 0,
      paymentStatus: 'PENDING',
      items: body.items || [],
      createdAt: now,
      updatedAt: now,
    }
    orders.push(order)
    return ok(order, 201)
  }),

  // PATCH /api/orders/:id/status
  http.patch('/api/orders/:id/status', async ({ params, request }) => {
    await delay(200)
    const { status } = await request.json() as any
    orders = orders.map(o => o.id === params.id ? { ...o, status, updatedAt: new Date().toISOString() } : o)
    return ok(orders.find(o => o.id === params.id))
  }),

  // POST /api/orders/:id/payment
  http.post('/api/orders/:id/payment', async ({ params, request }) => {
    await delay(300)
    const body = await request.json() as any
    orders = orders.map(o =>
      o.id === params.id
        ? { ...o, paymentStatus: 'PAID', status: 'DELIVERED', updatedAt: new Date().toISOString() }
        : o
    )
    return ok({ id: uid(), orderId: params.id, amount: body.amount, method: body.method, status: 'SUCCESS', paidAt: new Date().toISOString() }, 201)
  }),

  // ── KDS ────────────────────────────────────────────────────────────────────

  // GET /api/kds/active
  http.get('/api/kds/active', async () => {
    await delay(200)
    const active = orders.filter(o => ['PLACED', 'PREPARING'].includes(o.status))
    return ok(active)
  }),

  // PATCH /api/kds/items/:itemId/status
  http.patch('/api/kds/items/:itemId/status', async ({ params, request }) => {
    await delay(150)
    const { status } = await request.json() as any
    orders = orders.map(o => ({
      ...o,
      items: o.items.map(i => i.id === params.itemId ? { ...i, kdsStatus: status } : i),
    }))
    return ok({ updated: true })
  }),

  // ── ANALYTICS ─────────────────────────────────────────────────────────────

  // GET /api/analytics/dashboard
  http.get('/api/analytics/dashboard', async () => {
    await delay(300)
    return ok(demoDashboard)
  }),

  // GET /api/analytics/sales-chart
  http.get('/api/analytics/sales-chart', async () => {
    await delay(250)
    return ok(demoSalesChart)
  }),

  // GET /api/analytics/top-items
  http.get('/api/analytics/top-items', async () => {
    await delay(200)
    return ok(demoDashboard.topItems)
  }),

  // ── CRM / CUSTOMERS ───────────────────────────────────────────────────────

  // GET /api/crm/customers
  http.get('/api/crm/customers', async () => {
    await delay(200)
    return paged(customers)
  }),

  // POST /api/crm/customers
  http.post('/api/crm/customers', async ({ request }) => {
    await delay(300)
    const body = await request.json() as any
    const cust = { id: uid(), businessId: BUSINESS_ID, loyaltyPoints: 0, tier: 'BRONZE' as const, totalSpend: 0, visitCount: 0, whatsappOptOut: false, ...body }
    customers.push(cust)
    return ok(cust, 201)
  }),

  // GET /api/crm/customers/:id
  http.get('/api/crm/customers/:id', async ({ params }) => {
    await delay(150)
    const c = customers.find(c => c.id === params.id)
    if (!c) return fail('Customer not found', 404)
    return ok(c)
  }),

  // POST /api/crm/customers/:id/loyalty/earn
  http.post('/api/crm/customers/:id/loyalty/earn', async ({ params, request }) => {
    await delay(200)
    const { points } = await request.json() as any
    customers = customers.map(c => c.id === params.id ? { ...c, loyaltyPoints: c.loyaltyPoints + points } : c)
    return ok({ newBalance: customers.find(c => c.id === params.id)?.loyaltyPoints })
  }),

  // ── HR ────────────────────────────────────────────────────────────────────

  // GET /api/hr/employees
  http.get('/api/hr/employees', async () => {
    await delay(200)
    return paged(employees)
  }),

  // POST /api/hr/employees
  http.post('/api/hr/employees', async ({ request }) => {
    await delay(300)
    const body = await request.json() as any
    const emp = { id: uid(), businessId: BUSINESS_ID, status: 'ACTIVE', ...body }
    employees.push(emp)
    return ok(emp, 201)
  }),

  // POST /api/hr/attendance/check-in
  http.post('/api/hr/attendance/check-in', async ({ request }) => {
    await delay(200)
    const { employeeId } = await request.json() as any
    return ok({ employeeId, checkedIn: true, time: new Date().toISOString() })
  }),

  // POST /api/hr/attendance/check-out
  http.post('/api/hr/attendance/check-out', async ({ request }) => {
    await delay(200)
    const { employeeId } = await request.json() as any
    return ok({ employeeId, checkedOut: true, time: new Date().toISOString() })
  }),

  // ── INVENTORY ─────────────────────────────────────────────────────────────

  // GET /api/inventory/items
  http.get('/api/inventory/items', async () => {
    await delay(200)
    return paged(inventory)
  }),

  // POST /api/inventory/items
  http.post('/api/inventory/items', async ({ request }) => {
    await delay(300)
    const body = await request.json() as any
    const item = { id: uid(), businessId: BUSINESS_ID, active: true, lowStock: false, ...body }
    inventory.push(item)
    return ok(item, 201)
  }),

  // POST /api/inventory/items/:itemId/stock
  http.post('/api/inventory/items/:itemId/stock', async ({ params, request }) => {
    await delay(200)
    const { movementType, quantity } = await request.json() as any
    inventory = inventory.map(i => {
      if (i.id !== params.itemId) return i
      const delta = ['PURCHASE', 'ADJUSTMENT'].includes(movementType) ? quantity : -quantity
      const newStock = Math.max(0, i.currentStock + delta)
      return { ...i, currentStock: newStock, lowStock: newStock <= i.reorderLevel }
    })
    return ok(inventory.find(i => i.id === params.itemId))
  }),

  // ── FINANCE ───────────────────────────────────────────────────────────────

  // GET /api/finance/invoices
  http.get('/api/finance/invoices', async () => {
    await delay(200)
    const invoices = orders.filter(o => o.paymentStatus === 'PAID').map(o => ({
      id: uid(), orderId: o.id, invoiceNumber: `INV-${o.orderNumber.slice(4)}`,
      total: o.total, taxAmount: o.taxAmount, createdAt: o.createdAt,
    }))
    return paged(invoices)
  }),

  // GET /api/finance/gstr1
  http.get('/api/finance/gstr1', async () => {
    await delay(300)
    return ok({
      period: '2025-07',
      totalTaxableValue: 184600,
      totalCgst: 11076,
      totalSgst: 11076,
      totalIgst: 0,
      records: demoOrders.map(o => ({ invoiceNo: o.orderNumber, value: o.subtotal, gst: o.taxAmount })),
    })
  }),

  // GET /api/finance/pl-summary
  http.get('/api/finance/pl-summary', async () => {
    await delay(300)
    return ok({
      revenue: 184600,
      costOfGoods: 73840,
      grossProfit: 110760,
      operatingExpenses: 42000,
      netProfit: 68760,
      netMarginPercent: 37.2,
    })
  }),

  // ── WHATSAPP ──────────────────────────────────────────────────────────────

  // GET /api/whatsapp/logs
  http.get('/api/whatsapp/logs', async () => {
    await delay(200)
    return paged([
      { id: uid(), toPhone: '9999999999', templateName: 'order_confirmation', status: 'SENT', sentAt: new Date().toISOString() },
      { id: uid(), toPhone: '9822222222', templateName: 'daily_summary',      status: 'SENT', sentAt: new Date(Date.now() - 86400000).toISOString() },
    ])
  }),

  // ── SETTINGS ─────────────────────────────────────────────────────────────

  // GET /api/business/outlets
  http.get('/api/business/outlets', async () => {
    await delay(150)
    return ok([demoOutlet])
  }),

  // POST /api/business/outlets
  http.post('/api/business/outlets', async ({ request }) => {
    await delay(300)
    const body = await request.json() as any
    return ok({ id: uid(), businessId: BUSINESS_ID, isActive: true, ...body }, 201)
  }),
]

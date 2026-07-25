/** Static fixture data used by MSW handlers in mock/dev mode */

import type {
  Business, Outlet, AuthUser, Category, MenuItem,
  Order, Customer, DashboardData,
} from '@/types'

// ── IDs ──────────────────────────────────────────────────────────────────────
export const BUSINESS_ID = 'biz-demo-0001'
export const OUTLET_ID   = 'out-demo-0001'
export const OWNER_ID    = 'usr-demo-owner'
export const CASHIER_ID  = 'usr-demo-cashier'
export const KITCHEN_ID  = 'usr-demo-kitchen'

// ── Business ─────────────────────────────────────────────────────────────────
export const demoBusiness: Business = {
  id: BUSINESS_ID,
  name: 'Demo Dhaba',
  businessType: 'RESTAURANT',
  gstin: '29AABCU9603R1ZX',
  pan: 'AABCU9603R',
  addressLine1: '12 MG Road',
  city: 'Bengaluru',
  state: 'Karnataka',
  pincode: '560001',
  currencyCode: 'INR',
  timezone: 'Asia/Kolkata',
  status: 'ACTIVE',
  gstInclusive: false,
  onboardingStep: 5,
}

export const demoOutlet: Outlet = {
  id: OUTLET_ID,
  businessId: BUSINESS_ID,
  name: 'Main Branch',
  outletType: 'DINE_IN',
  phone: '9999999999',
  addressLine1: '12 MG Road',
  city: 'Bengaluru',
  state: 'Karnataka',
  isActive: true,
}

// ── Users ─────────────────────────────────────────────────────────────────────
export const demoOwner: AuthUser = {
  id: OWNER_ID,
  name: 'Demo Owner',
  email: 'owner@demodhabha.com',
  phone: '9999999999',
  role: 'BUSINESS_OWNER',
  businessId: BUSINESS_ID,
  outletId: OUTLET_ID,
  mobileVerified: true,
}

// ── Categories ────────────────────────────────────────────────────────────────
export const demoCategories: Category[] = [
  { id: 'cat-001', businessId: BUSINESS_ID, name: 'Starters',   imageUrl: '', sortOrder: 1, active: true },
  { id: 'cat-002', businessId: BUSINESS_ID, name: 'Mains',      imageUrl: '', sortOrder: 2, active: true },
  { id: 'cat-003', businessId: BUSINESS_ID, name: 'Breads',     imageUrl: '', sortOrder: 3, active: true },
  { id: 'cat-004', businessId: BUSINESS_ID, name: 'Beverages',  imageUrl: '', sortOrder: 4, active: true },
  { id: 'cat-005', businessId: BUSINESS_ID, name: 'Desserts',   imageUrl: '', sortOrder: 5, active: true },
]

// ── Menu Items ────────────────────────────────────────────────────────────────
export const demoMenuItems: MenuItem[] = [
  // Starters
  { id: 'mi-001', businessId: BUSINESS_ID, categoryId: 'cat-001', name: 'Paneer Tikka',      basePrice: 220, veg: true,  available: true, archived: false, sortOrder: 1, preparationTime: 12 },
  { id: 'mi-002', businessId: BUSINESS_ID, categoryId: 'cat-001', name: 'Chicken 65',        basePrice: 280, veg: false, available: true, archived: false, sortOrder: 2, preparationTime: 15 },
  { id: 'mi-003', businessId: BUSINESS_ID, categoryId: 'cat-001', name: 'Veg Spring Rolls',  basePrice: 160, veg: true,  available: true, archived: false, sortOrder: 3, preparationTime: 10 },
  // Mains
  { id: 'mi-004', businessId: BUSINESS_ID, categoryId: 'cat-002', name: 'Dal Makhani',       basePrice: 180, veg: true,  available: true, archived: false, sortOrder: 1, preparationTime: 20 },
  { id: 'mi-005', businessId: BUSINESS_ID, categoryId: 'cat-002', name: 'Butter Chicken',    basePrice: 320, veg: false, available: true, archived: false, sortOrder: 2, preparationTime: 20 },
  { id: 'mi-006', businessId: BUSINESS_ID, categoryId: 'cat-002', name: 'Palak Paneer',      basePrice: 200, veg: true,  available: true, archived: false, sortOrder: 3, preparationTime: 18 },
  { id: 'mi-007', businessId: BUSINESS_ID, categoryId: 'cat-002', name: 'Mutton Rogan Josh', basePrice: 380, veg: false, available: true, archived: false, sortOrder: 4, preparationTime: 25 },
  // Breads
  { id: 'mi-008', businessId: BUSINESS_ID, categoryId: 'cat-003', name: 'Butter Naan',       basePrice: 40,  veg: true,  available: true, archived: false, sortOrder: 1, preparationTime: 8  },
  { id: 'mi-009', businessId: BUSINESS_ID, categoryId: 'cat-003', name: 'Lachha Paratha',    basePrice: 45,  veg: true,  available: true, archived: false, sortOrder: 2, preparationTime: 8  },
  { id: 'mi-010', businessId: BUSINESS_ID, categoryId: 'cat-003', name: 'Tandoori Roti',     basePrice: 30,  veg: true,  available: true, archived: false, sortOrder: 3, preparationTime: 6  },
  // Beverages
  { id: 'mi-011', businessId: BUSINESS_ID, categoryId: 'cat-004', name: 'Masala Chai',       basePrice: 30,  veg: true,  available: true, archived: false, sortOrder: 1, preparationTime: 5  },
  { id: 'mi-012', businessId: BUSINESS_ID, categoryId: 'cat-004', name: 'Mango Lassi',       basePrice: 80,  veg: true,  available: true, archived: false, sortOrder: 2, preparationTime: 5  },
  { id: 'mi-013', businessId: BUSINESS_ID, categoryId: 'cat-004', name: 'Cold Coffee',       basePrice: 120, veg: true,  available: true, archived: false, sortOrder: 3, preparationTime: 7  },
  // Desserts
  { id: 'mi-014', businessId: BUSINESS_ID, categoryId: 'cat-005', name: 'Gulab Jamun',       basePrice: 80,  veg: true,  available: true, archived: false, sortOrder: 1, preparationTime: 5  },
  { id: 'mi-015', businessId: BUSINESS_ID, categoryId: 'cat-005', name: 'Kulfi Falooda',     basePrice: 120, veg: true,  available: true, archived: false, sortOrder: 2, preparationTime: 5  },
]

// ── Orders ────────────────────────────────────────────────────────────────────
export const demoOrders: Order[] = [
  {
    id: 'ord-001', businessId: BUSINESS_ID, outletId: OUTLET_ID,
    orderNumber: 'ORD-2001', orderType: 'DINE_IN', status: 'DELIVERED',
    subtotal: 720, taxAmount: 129.6, discountAmount: 0, serviceCharge: 0,
    total: 849.6, paymentStatus: 'PAID',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
    items: [
      { id: 'oi-001', orderId: 'ord-001', menuItemId: 'mi-001', menuItemName: 'Paneer Tikka',  quantity: 2, unitPrice: 220, addons: [], taxAmount: 79.2,  totalPrice: 440, kdsStatus: 'DONE' },
      { id: 'oi-002', orderId: 'ord-001', menuItemId: 'mi-004', menuItemName: 'Dal Makhani',   quantity: 1, unitPrice: 180, addons: [], taxAmount: 32.4,  totalPrice: 180, kdsStatus: 'DONE' },
      { id: 'oi-003', orderId: 'ord-001', menuItemId: 'mi-008', menuItemName: 'Butter Naan',   quantity: 3, unitPrice: 40,  addons: [], taxAmount: 21.6,  totalPrice: 120, kdsStatus: 'DONE' },
    ],
  },
  {
    id: 'ord-002', businessId: BUSINESS_ID, outletId: OUTLET_ID,
    orderNumber: 'ORD-2002', orderType: 'TAKEAWAY', status: 'PREPARING',
    subtotal: 600, taxAmount: 108, discountAmount: 0, serviceCharge: 0,
    total: 708, paymentStatus: 'PAID',
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 3600000).toISOString(),
    items: [
      { id: 'oi-004', orderId: 'ord-002', menuItemId: 'mi-005', menuItemName: 'Butter Chicken', quantity: 1, unitPrice: 320, addons: [], taxAmount: 57.6, totalPrice: 320, kdsStatus: 'PREPARING' },
      { id: 'oi-005', orderId: 'ord-002', menuItemId: 'mi-008', menuItemName: 'Butter Naan',    quantity: 4, unitPrice: 40,  addons: [], taxAmount: 28.8, totalPrice: 160, kdsStatus: 'PENDING'   },
      { id: 'oi-006', orderId: 'ord-002', menuItemId: 'mi-012', menuItemName: 'Mango Lassi',    quantity: 2, unitPrice: 80,  addons: [], taxAmount: 28.8, totalPrice: 160, kdsStatus: 'PENDING'   },
    ],
  },
  {
    id: 'ord-003', businessId: BUSINESS_ID, outletId: OUTLET_ID,
    orderNumber: 'ORD-2003', orderType: 'DINE_IN', status: 'PLACED',
    subtotal: 400, taxAmount: 72, discountAmount: 0, serviceCharge: 0,
    total: 472, paymentStatus: 'PENDING',
    createdAt: new Date(Date.now() - 900000).toISOString(),
    updatedAt: new Date(Date.now() - 900000).toISOString(),
    items: [
      { id: 'oi-007', orderId: 'ord-003', menuItemId: 'mi-002', menuItemName: 'Chicken 65',   quantity: 1, unitPrice: 280, addons: [], taxAmount: 50.4, totalPrice: 280, kdsStatus: 'PENDING' },
      { id: 'oi-008', orderId: 'ord-003', menuItemId: 'mi-011', menuItemName: 'Masala Chai',  quantity: 2, unitPrice: 30,  addons: [], taxAmount: 10.8, totalPrice: 60,  kdsStatus: 'PENDING' },
      { id: 'oi-009', orderId: 'ord-003', menuItemId: 'mi-009', menuItemName: 'Lachha Paratha',quantity: 2, unitPrice: 45, addons: [], taxAmount: 16.2, totalPrice: 90,  kdsStatus: 'PENDING' },
    ],
  },
]

// ── Customers ─────────────────────────────────────────────────────────────────
export const demoCustomers: Customer[] = [
  { id: 'cus-001', businessId: BUSINESS_ID, name: 'Rohan Sharma',   phone: '9811111111', loyaltyPoints: 450, tier: 'SILVER',   totalSpend: 4500,  visitCount: 9,  whatsappOptOut: false },
  { id: 'cus-002', businessId: BUSINESS_ID, name: 'Priya Nair',     phone: '9822222222', loyaltyPoints: 1200, tier: 'GOLD',   totalSpend: 12000, visitCount: 24, whatsappOptOut: false },
  { id: 'cus-003', businessId: BUSINESS_ID, name: 'Amit Verma',     phone: '9833333333', loyaltyPoints: 80,  tier: 'BRONZE',  totalSpend: 800,   visitCount: 2,  whatsappOptOut: true  },
  { id: 'cus-004', businessId: BUSINESS_ID, name: 'Sunita Gupta',   phone: '9844444444', loyaltyPoints: 3100, tier: 'PLATINUM', totalSpend: 31000, visitCount: 62, whatsappOptOut: false },
  { id: 'cus-005', businessId: BUSINESS_ID, name: 'Vikram Mehta',   phone: '9855555555', loyaltyPoints: 220, tier: 'BRONZE',  totalSpend: 2200,  visitCount: 4,  whatsappOptOut: false },
]

// ── Dashboard ─────────────────────────────────────────────────────────────────
export const demoDashboard: DashboardData = {
  revenue: {
    total: 548230,
    today: 12840,
    thisWeek: 68400,
    thisMonth: 184600,
    growthPercent: 12.4,
  },
  orders: {
    total: 1438,
    avgOrderValue: 381,
    peakHour: 13,
    cancelRate: 2.1,
  },
  topItems: [
    { name: 'Butter Chicken',  quantity: 312, revenue: 99840 },
    { name: 'Dal Makhani',     quantity: 289, revenue: 52020 },
    { name: 'Paneer Tikka',    quantity: 245, revenue: 53900 },
    { name: 'Butter Naan',     quantity: 892, revenue: 35680 },
    { name: 'Mango Lassi',     quantity: 198, revenue: 15840 },
  ],
  paymentBreakdown: { cash: 38420, upi: 94200, card: 32180, online: 19800 },
}

// ── Sales chart data (last 7 days) ────────────────────────────────────────────
export const demoSalesChart = [
  { date: '2025-07-01', revenue: 22400, orders: 58 },
  { date: '2025-07-02', revenue: 19800, orders: 51 },
  { date: '2025-07-03', revenue: 24600, orders: 64 },
  { date: '2025-07-04', revenue: 28200, orders: 73 },
  { date: '2025-07-05', revenue: 21000, orders: 55 },
  { date: '2025-07-06', revenue: 31400, orders: 82 },
  { date: '2025-07-07', revenue: 18400, orders: 48 },
]

// ── HR / Employees ────────────────────────────────────────────────────────────
export const demoEmployees = [
  { id: 'emp-001', businessId: BUSINESS_ID, employeeCode: 'EMP001', name: 'Demo Owner',    designation: 'Owner',   department: 'MANAGEMENT', basicSalary: 0,     status: 'ACTIVE', mobile: '9999999999', joinDate: '2024-01-01' },
  { id: 'emp-002', businessId: BUSINESS_ID, employeeCode: 'EMP002', name: 'Arjun Cashier', designation: 'Cashier', department: 'OPERATIONS', basicSalary: 18000, status: 'ACTIVE', mobile: '8888888888', joinDate: '2024-02-01' },
  { id: 'emp-003', businessId: BUSINESS_ID, employeeCode: 'EMP003', name: 'Ravi Kitchen',  designation: 'Cook',    department: 'KITCHEN',    basicSalary: 20000, status: 'ACTIVE', mobile: '7777777777', joinDate: '2024-02-15' },
  { id: 'emp-004', businessId: BUSINESS_ID, employeeCode: 'EMP004', name: 'Meena Waiter',  designation: 'Waiter',  department: 'OPERATIONS', basicSalary: 15000, status: 'ACTIVE', mobile: '9666666666', joinDate: '2024-03-01' },
]

// ── Inventory ─────────────────────────────────────────────────────────────────
export const demoInventoryItems = [
  { id: 'inv-001', businessId: BUSINESS_ID, name: 'Chicken (kg)',    unit: 'KG',  currentStock: 12.5, reorderLevel: 5, costPerUnit: 180 },
  { id: 'inv-002', businessId: BUSINESS_ID, name: 'Paneer (kg)',     unit: 'KG',  currentStock: 4.2,  reorderLevel: 2, costPerUnit: 280 },
  { id: 'inv-003', businessId: BUSINESS_ID, name: 'Rice (kg)',       unit: 'KG',  currentStock: 32,   reorderLevel: 10, costPerUnit: 60 },
  { id: 'inv-004', businessId: BUSINESS_ID, name: 'Milk (litre)',    unit: 'LTR', currentStock: 18,   reorderLevel: 6, costPerUnit: 55 },
  { id: 'inv-005', businessId: BUSINESS_ID, name: 'Cooking Oil (L)', unit: 'LTR', currentStock: 8,    reorderLevel: 3, costPerUnit: 130 },
  { id: 'inv-006', businessId: BUSINESS_ID, name: 'Onion (kg)',      unit: 'KG',  currentStock: 15,   reorderLevel: 5, costPerUnit: 35 },
  { id: 'inv-007', businessId: BUSINESS_ID, name: 'Tomato (kg)',     unit: 'KG',  currentStock: 2.5,  reorderLevel: 4, costPerUnit: 45 },
]

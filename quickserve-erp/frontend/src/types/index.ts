/** Shared TypeScript types for QuickServe ERP */

// ===== API Response Wrappers =====

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  errors?: string[]
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

// ===== Auth =====

export type UserRole =
  | 'SUPER_ADMIN'
  | 'BUSINESS_OWNER'
  | 'OUTLET_MANAGER'
  | 'CASHIER'
  | 'WAITER'
  | 'KITCHEN_STAFF'
  | 'ACCOUNTANT'
  | 'HR_MANAGER'

export interface AuthUser {
  id: string
  name: string
  email?: string
  phone: string
  role: UserRole
  businessId?: string
  outletId?: string
  mobileVerified: boolean
}

export interface LoginResponse {
  accessToken: string
  userId: string
  businessId?: string
  outletId?: string
  role: UserRole
  name: string
}

// ===== Business & Outlets =====

export type BusinessType = 'RESTAURANT' | 'CAFE' | 'QSR' | 'RETAIL' | 'BAKERY' | 'FRANCHISE' | 'OTHER'
export type BusinessStatus = 'ONBOARDING' | 'ACTIVE' | 'SUSPENDED' | 'CANCELLED'

export interface Business {
  id: string
  name: string
  businessType?: BusinessType
  gstin?: string
  pan?: string
  addressLine1?: string
  city?: string
  state?: string
  pincode?: string
  logoUrl?: string
  currencyCode: string
  timezone: string
  status: BusinessStatus
  gstInclusive: boolean
  onboardingStep: number
  trialEndsAt?: string
}

export interface Outlet {
  id: string
  businessId: string
  name: string
  outletType: 'DINE_IN' | 'TAKEAWAY' | 'DELIVERY_ONLY' | 'BOTH'
  phone?: string
  addressLine1?: string
  city?: string
  state?: string
  isActive: boolean
}

// ===== Menu =====

export interface Category {
  id: string
  businessId: string
  name: string
  imageUrl?: string
  sortOrder: number
  active: boolean
}

export interface TaxSlab {
  id: string
  businessId: string
  name: string
  percentage: number
  hsnCode?: string
  sacCode?: string
}

export interface MenuItem {
  id: string
  businessId: string
  categoryId: string
  taxSlabId?: string
  name: string
  description?: string
  basePrice: number
  imageUrl?: string
  veg: boolean
  available: boolean
  archived: boolean
  preparationTime?: number
  calories?: number
  sortOrder: number
}

// ===== Orders =====

export type OrderType    = 'DINE_IN' | 'TAKEAWAY' | 'DELIVERY' | 'QR_SELF'
export type OrderStatus  = 'DRAFT' | 'PLACED' | 'PREPARING' | 'READY' | 'DELIVERED' | 'CANCELLED'
export type PaymentStatus = 'PENDING' | 'PARTIAL' | 'PAID'
export type PaymentMethod = 'CASH' | 'CARD' | 'UPI' | 'RAZORPAY' | 'CREDIT'
export type KdsStatus    = 'PENDING' | 'PREPARING' | 'DONE'

export interface OrderItem {
  id: string
  orderId: string
  menuItemId?: string
  menuItemName: string
  quantity: number
  unitPrice: number
  variantName?: string
  addons: { id: string; name: string; price: number }[]
  taxAmount: number
  totalPrice: number
  kdsStatus: KdsStatus
  kdsNotes?: string
}

export interface Order {
  id: string
  businessId: string
  outletId: string
  orderNumber: string
  orderType: OrderType
  tableId?: string
  customerId?: string
  staffId?: string
  status: OrderStatus
  subtotal: number
  taxAmount: number
  discountAmount: number
  serviceCharge: number
  total: number
  paymentStatus: PaymentStatus
  notes?: string
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export interface Payment {
  id: string
  orderId: string
  amount: number
  method: PaymentMethod
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'
  paidAt?: string
}

// ===== Tables =====

export type TableStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'CLEANING'

export interface Table {
  id: string
  outletId: string
  name: string
  capacity: number
  status: TableStatus
  qrCodeUrl?: string
  sectionId?: string
}

// ===== Customers =====

export type CustomerTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM'

export interface Customer {
  id: string
  businessId: string
  name: string
  phone?: string
  email?: string
  loyaltyPoints: number
  tier: CustomerTier
  totalSpend: number
  visitCount: number
  lastVisitAt?: string
  dateOfBirth?: string
  whatsappOptOut: boolean
}

// ===== Analytics =====

export interface DashboardData {
  revenue: {
    total: number
    today: number
    thisWeek: number
    thisMonth: number
    growthPercent: number
  }
  orders: {
    total: number
    avgOrderValue: number
    peakHour: number
    cancelRate: number
  }
  topItems: { name: string; quantity: number; revenue: number }[]
  paymentBreakdown: { cash: number; upi: number; card: number; online: number }
}

// ===== Cart (local state) =====

export interface CartItem {
  menuItemId: string
  menuItemName: string
  basePrice: number
  quantity: number
  variantId?: string
  variantName?: string
  addonIds?: string[]
  notes?: string
}

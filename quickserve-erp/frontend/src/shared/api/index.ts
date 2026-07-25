import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/shared/store/authStore'

// Base API instance
const api = axios.create({
  baseURL: '/api',
  withCredentials: true,        // Send HTTP-only cookie with every request
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor — attach Authorization header if token present
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor — handle 401 globally
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api

// ============================================================
// Auth API
// ============================================================

export const authApi = {
  register: (data: {
    businessName: string
    ownerName: string
    mobile: string
    email: string
    password: string
  }) => api.post('/auth/register', data),

  verifyOtp: (mobile: string, otp: string) =>
    api.post('/auth/verify-otp', { mobile, otp }),

  resendOtp: (mobile: string) =>
    api.post('/auth/resend-otp', { mobile }),

  login: (mobile: string, password: string) =>
    api.post('/auth/login', { mobile, password }),

  logout: () => api.post('/auth/logout'),

  me: () => api.get('/auth/me'),

  forgotPassword: (mobile: string) =>
    api.post('/auth/forgot-password', { mobile }),

  resetPassword: (mobile: string, otp: string, newPassword: string) =>
    api.post('/auth/reset-password', { mobile, otp, newPassword }),
}

// ============================================================
// Menu API
// ============================================================

export const menuApi = {
  getCategories: () => api.get('/menu/categories'),
  createCategory: (data: { name: string; imageUrl?: string; sortOrder?: number }) =>
    api.post('/menu/categories', data),
  updateCategory: (id: string, data: object) => api.put(`/menu/categories/${id}`, data),
  deleteCategory: (id: string) => api.delete(`/menu/categories/${id}`),
  getItems: (page = 0, size = 50) => api.get(`/menu/items?page=${page}&size=${size}`),
  createItem: (data: object) => api.post('/menu/items', data),
  updateItem: (id: string, data: object) => api.put(`/menu/items/${id}`, data),
  toggleAvailability: (id: string, available: boolean) =>
    api.patch(`/menu/items/${id}/availability`, { available }),
  getTaxSlabs: () => api.get('/menu/tax-slabs'),
  getPublicMenu: (outletId: string) => api.get(`/menu/public/${outletId}`),
}

// ============================================================
// Order API
// ============================================================

export const orderApi = {
  getOrders: (page = 0, size = 20) => api.get(`/orders?page=${page}&size=${size}`),
  getOrder: (id: string) => api.get(`/orders/${id}`),
  createOrder: (data: object) => api.post('/orders', data),
  updateStatus: (id: string, status: string) => api.patch(`/orders/${id}/status`, { status }),
  processPayment: (id: string, data: object) => api.post(`/orders/${id}/payment`, data),
  cancelOrder: (id: string, reason?: string) => api.post(`/orders/${id}/cancel`, { reason }),
  applyDiscount: (id: string, amount: number, isPercentage: boolean) =>
    api.post(`/orders/${id}/apply-discount`, { amount, isPercentage }),
  getInvoicePdf: (id: string) => api.get(`/orders/${id}/invoice`, { responseType: 'blob' }),
}

// ============================================================
// KDS API
// ============================================================

export const kdsApi = {
  getActiveOrders: (outletId: string) => api.get(`/kds/display/${outletId}`),
  updateItemStatus: (itemId: string, status: string) =>
    api.patch(`/kds/items/${itemId}/status`, { status }),
}

// ============================================================
// Finance API
// ============================================================

export const financeApi = {
  getInvoices:    (page = 0, size = 20) => api.get(`/finance/invoices?page=${page}&size=${size}`),
  getGstr1:       (month: string) => api.get(`/finance/gst/gstr1?month=${month}`),
  getGstr3b:      (month: string) => api.get(`/finance/gst/gstr3b?month=${month}`),
  getTrialBalance:(date: string) => api.get(`/finance/trial-balance?date=${date}`),
  getProfitLoss:  (from: string, to: string) => api.get(`/finance/profit-loss?from=${from}&to=${to}`),
  getExpenses:    (page = 0) => api.get(`/finance/expenses?page=${page}`),
  createExpense:  (data: object) => api.post('/finance/expenses', data),
}

// ============================================================
// Inventory API
// ============================================================

export const inventoryApi = {
  getItems:      (page = 0) => api.get(`/inventory/items?page=${page}`),
  createItem:    (data: object) => api.post('/inventory/items', data),
  updateItem:    (id: string, data: object) => api.put(`/inventory/items/${id}`, data),
  getLowStock:   () => api.get('/inventory/items/low-stock'),
  adjustStock:   (data: object) => api.post('/inventory/adjust', data),
  getPOs:        () => api.get('/inventory/purchase-orders'),
  createPO:      (data: object) => api.post('/inventory/purchase-orders', data),
  receiveGRN:    (poId: string, data: object) => api.post(`/inventory/purchase-orders/${poId}/grn`, data),
  getSuppliers:  () => api.get('/inventory/suppliers'),
  createSupplier:(data: object) => api.post('/inventory/suppliers', data),
}

// ============================================================
// HR API
// ============================================================

export const hrApi = {
  getEmployees:  (page = 0) => api.get(`/hr/employees?page=${page}`),
  createEmployee:(data: object) => api.post('/hr/employees', data),
  updateEmployee:(id: string, data: object) => api.put(`/hr/employees/${id}`, data),
  checkIn:       (employeeId: string) => api.post('/hr/attendance/check-in', { employeeId }),
  checkOut:      (employeeId: string) => api.post('/hr/attendance/check-out', { employeeId }),
  getLeaves:     () => api.get('/hr/leaves'),
  applyLeave:    (data: object) => api.post('/hr/leaves/apply', data),
  approveLeave:  (id: string) => api.patch(`/hr/leaves/${id}/approve`),
  processPayroll:(month: number, year: number) => api.post(`/hr/payroll/process?month=${month}&year=${year}`),
}

// ============================================================
// CRM API
// ============================================================

export const crmApi = {
  getCustomers:  (page = 0) => api.get(`/crm/customers?page=${page}`),
  searchCustomers:(q: string) => api.get(`/crm/customers/search?q=${encodeURIComponent(q)}`),
  createCustomer:(data: object) => api.post('/crm/customers', data),
  updateCustomer:(id: string, data: object) => api.put(`/crm/customers/${id}`, data),
  earnPoints:    (customerId: string, amount: number) => api.post('/crm/loyalty/earn', { customerId, amount }),
  redeemPoints:  (customerId: string, points: number) => api.post('/crm/loyalty/redeem', { customerId, points }),
  getCampaigns:  () => api.get('/crm/campaigns'),
  createCampaign:(data: object) => api.post('/crm/campaigns', data),
  sendCampaign:  (id: string) => api.post(`/crm/campaigns/${id}/send-now`),
}

// ============================================================
// Analytics API
// ============================================================

export const analyticsApi = {
  getDashboard:  (from?: string, to?: string) => api.get(`/analytics/dashboard${from ? `?from=${from}&to=${to}` : ''}`),
  getSalesReport:(from: string, to: string, groupBy = 'DAY') =>
    api.get(`/analytics/sales-report?from=${from}&to=${to}&groupBy=${groupBy}`),
  getPLSummary:  (from: string, to: string) => api.get(`/analytics/pl-summary?from=${from}&to=${to}`),
}

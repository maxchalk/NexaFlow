export interface User {
  userId: number
  email: string
  firstName: string
  role: 'ADMIN' | 'CUSTOMER'
}

export interface Product {
  id: number
  name: string
  description: string
  category: string
  price: number
  stockQuantity: number
  imageUrl?: string
  active: boolean
  createdAt: string
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Order {
  id: number
  userId: number
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
  totalAmount: number
  cancellationReason?: string
  createdAt: string
  updatedAt: string
  items: OrderItem[]
}

export interface Notification {
  id: string
  userId: number
  type: string
  title: string
  message: string
  read: boolean
  createdAt: string
}

export interface OrderStats {
  totalOrders: number
  pendingOrders: number
  confirmedOrders: number
  shippedOrders: number
  deliveredOrders: number
  cancelledOrders: number
  totalRevenue: number
}

export interface CartItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
}

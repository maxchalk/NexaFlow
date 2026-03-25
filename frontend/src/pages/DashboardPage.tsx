import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShoppingCart, Package, Bell, CheckCircle } from 'lucide-react'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'
import client from '../api/client'
import { Order } from '../types'

export default function DashboardPage() {
  const navigate = useNavigate()
  const userId = localStorage.getItem('userId')
  const firstName = localStorage.getItem('firstName')
  const [orders, setOrders] = useState<Order[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [ordersRes, notifRes] = await Promise.all([
          client.get('/orders/my'),
          client.get(`/notifications/${userId}/unread-count`),
        ])
        setOrders(ordersRes.data)
        setUnreadCount(notifRes.data.count || 0)
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [userId])

  const pending = orders.filter(o => o.status === 'PENDING').length
  const recent = orders.slice(0, 5)

  const stats = [
    { label: 'Total Orders', value: orders.length, icon: ShoppingCart, color: 'bg-blue-500' },
    { label: 'Pending Orders', value: pending, icon: Package, color: 'bg-yellow-500' },
    { label: 'Notifications', value: unreadCount, icon: Bell, color: 'bg-purple-500' },
    { label: 'Delivered', value: orders.filter(o => o.status === 'DELIVERED').length, icon: CheckCircle, color: 'bg-green-500' },
  ]

  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="max-w-7xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-slate-900 mb-2">Welcome back, {firstName}!</h1>
        <p className="text-slate-500 mb-8">Here's what's happening with your orders today.</p>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {stats.map(stat => (
            <div key={stat.label} className="bg-white rounded-xl p-6 shadow-sm border border-slate-100">
              <div className="flex items-center gap-4">
                <div className={`${stat.color} p-3 rounded-lg`}>
                  <stat.icon className="h-6 w-6 text-white" />
                </div>
                <div>
                  <p className="text-sm text-slate-500">{stat.label}</p>
                  <p className="text-2xl font-bold text-slate-900">{loading ? '...' : stat.value}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-100 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Recent Orders</h2>
          {loading ? (
            <p className="text-slate-400">Loading orders...</p>
          ) : recent.length === 0 ? (
            <p className="text-slate-400 text-center py-8">No orders yet. Start shopping!</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100">
                  <th className="text-left py-2 text-slate-500 font-medium">Order ID</th>
                  <th className="text-left py-2 text-slate-500 font-medium">Date</th>
                  <th className="text-left py-2 text-slate-500 font-medium">Total</th>
                  <th className="text-left py-2 text-slate-500 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {recent.map(order => (
                  <tr key={order.id} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="py-3 font-medium">#{order.id}</td>
                    <td className="py-3 text-slate-500">{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td className="py-3 font-medium">${order.totalAmount.toFixed(2)}</td>
                    <td className="py-3"><StatusBadge status={order.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="flex gap-3">
          <button onClick={() => navigate('/products')} className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
            Browse Products
          </button>
          <button onClick={() => navigate('/orders')} className="bg-slate-700 hover:bg-slate-800 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
            View All Orders
          </button>
          <button onClick={() => navigate('/notifications')} className="border border-slate-300 hover:bg-slate-100 text-slate-700 px-4 py-2 rounded-lg text-sm font-medium transition-colors">
            View Notifications
          </button>
        </div>
      </div>
    </div>
  )
}

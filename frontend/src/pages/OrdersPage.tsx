import { useEffect, useState } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import toast from 'react-hot-toast'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'
import client from '../api/client'
import { Order } from '../types'

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')
  const [expanded, setExpanded] = useState<number | null>(null)

  const fetchOrders = async () => {
    setLoading(true)
    try {
      const res = await client.get('/orders/my')
      setOrders(res.data)
    } catch (e) {
      toast.error('Failed to load orders')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchOrders() }, [])

  const cancelOrder = async (orderId: number) => {
    try {
      await client.patch(`/orders/${orderId}/cancel`, { reason: 'Cancelled by customer' })
      toast.success('Order cancelled')
      fetchOrders()
    } catch (e: any) {
      toast.error(e.response?.data?.error || 'Cannot cancel order')
    }
  }

  const filtered = filter ? orders.filter(o => o.status === filter) : orders

  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-slate-900">My Orders</h1>
          <select value={filter} onChange={e => setFilter(e.target.value)}
            className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="SHIPPED">Shipped</option>
            <option value="DELIVERED">Delivered</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        {loading ? (
          <div className="text-center py-16 text-slate-400">Loading orders...</div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16 text-slate-400">No orders found.</div>
        ) : (
          <div className="space-y-3">
            {filtered.map(order => (
              <div key={order.id} className="bg-white rounded-xl shadow-sm border border-slate-100">
                <div className="p-4 flex items-center justify-between cursor-pointer" onClick={() => setExpanded(expanded === order.id ? null : order.id)}>
                  <div className="flex items-center gap-6">
                    <span className="font-bold text-slate-900">#{order.id}</span>
                    <span className="text-sm text-slate-500">{new Date(order.createdAt).toLocaleDateString()}</span>
                    <span className="font-semibold text-blue-600">${order.totalAmount.toFixed(2)}</span>
                    <StatusBadge status={order.status} />
                  </div>
                  <div className="flex items-center gap-3">
                    {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
                      <button onClick={e => { e.stopPropagation(); cancelOrder(order.id) }}
                        className="text-xs bg-red-100 hover:bg-red-200 text-red-700 px-3 py-1 rounded-full font-medium transition-colors">
                        Cancel
                      </button>
                    )}
                    {expanded === order.id ? <ChevronUp className="h-4 w-4 text-slate-400" /> : <ChevronDown className="h-4 w-4 text-slate-400" />}
                  </div>
                </div>
                {expanded === order.id && (
                  <div className="border-t border-slate-100 p-4">
                    {order.cancellationReason && (
                      <p className="text-sm text-red-600 mb-3">Reason: {order.cancellationReason}</p>
                    )}
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-slate-100">
                          <th className="text-left py-1 text-slate-500 font-medium">Product</th>
                          <th className="text-left py-1 text-slate-500 font-medium">Qty</th>
                          <th className="text-left py-1 text-slate-500 font-medium">Unit Price</th>
                          <th className="text-left py-1 text-slate-500 font-medium">Subtotal</th>
                        </tr>
                      </thead>
                      <tbody>
                        {order.items.map(item => (
                          <tr key={item.id} className="border-b border-slate-50">
                            <td className="py-2">{item.productName}</td>
                            <td className="py-2">{item.quantity}</td>
                            <td className="py-2">${item.unitPrice.toFixed(2)}</td>
                            <td className="py-2 font-medium">${item.subtotal.toFixed(2)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

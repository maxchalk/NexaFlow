import { useEffect, useState, useCallback } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import Navbar from '../components/Navbar'
import client from '../api/client'
import { OrderStats, Product } from '../types'
import toast from 'react-hot-toast'

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<OrderStats | null>(null)
  const [products, setProducts] = useState<Product[]>([])
  const [stockUpdates, setStockUpdates] = useState<Record<number, string>>({})

  const fetchData = useCallback(async () => {
    try {
      const [statsRes, productsRes] = await Promise.all([
        client.get('/orders/stats'),
        client.get('/products'),
      ])
      setStats(statsRes.data)
      setProducts(productsRes.data)
    } catch (e) {
      console.error(e)
    }
  }, [])

  useEffect(() => {
    fetchData()
    const interval = setInterval(fetchData, 10000)
    return () => clearInterval(interval)
  }, [fetchData])

  const updateStock = async (productId: number) => {
    const qty = parseInt(stockUpdates[productId] || '0')
    if (isNaN(qty)) { toast.error('Invalid quantity'); return }
    try {
      await client.patch(`/products/${productId}/stock`, { quantity: qty })
      toast.success('Stock updated!')
      fetchData()
      setStockUpdates(prev => ({...prev, [productId]: ''}))
    } catch (e) {
      toast.error('Failed to update stock')
    }
  }

  const chartData = stats ? [
    { name: 'Pending', count: stats.pendingOrders },
    { name: 'Confirmed', count: stats.confirmedOrders },
    { name: 'Shipped', count: stats.shippedOrders },
    { name: 'Delivered', count: stats.deliveredOrders },
    { name: 'Cancelled', count: stats.cancelledOrders },
  ] : []

  const lowStock = products.filter(p => p.stockQuantity < 5)

  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="max-w-7xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-slate-900 mb-2">Admin Dashboard</h1>
        <p className="text-slate-500 mb-8">Live metrics refresh every 10 seconds.</p>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {[
            { label: 'Total Orders', value: stats?.totalOrders ?? '—' },
            { label: 'Total Revenue', value: stats ? `$${stats.totalRevenue.toFixed(2)}` : '—' },
            { label: 'Low Stock Products', value: lowStock.length },
            { label: 'Delivered', value: stats?.deliveredOrders ?? '—' },
          ].map(card => (
            <div key={card.label} className="bg-white rounded-xl p-6 shadow-sm border border-slate-100">
              <p className="text-sm text-slate-500 mb-1">{card.label}</p>
              <p className="text-2xl font-bold text-slate-900">{card.value}</p>
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          <div className="bg-white rounded-xl shadow-sm border border-slate-100 p-6">
            <h2 className="text-lg font-semibold mb-4">Orders by Status</h2>
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={chartData}>
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-slate-100 p-6">
            <h2 className="text-lg font-semibold mb-4">Stock Management</h2>
            <div className="space-y-3 max-h-48 overflow-y-auto">
              {products.slice(0, 10).map(p => (
                <div key={p.id} className="flex items-center gap-3 text-sm">
                  <span className="flex-1 truncate font-medium">{p.name}</span>
                  <span className={`text-xs px-2 py-0.5 rounded-full ${p.stockQuantity < 5 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                    {p.stockQuantity}
                  </span>
                  <input type="number" min="0" value={stockUpdates[p.id] || ''} onChange={e => setStockUpdates(prev => ({...prev, [p.id]: e.target.value}))}
                    placeholder="New qty" className="w-20 border border-slate-300 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500" />
                  <button onClick={() => updateStock(p.id)} className="text-xs bg-blue-600 hover:bg-blue-700 text-white px-2 py-1 rounded">Set</button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

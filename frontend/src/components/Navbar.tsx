import { Link, useNavigate } from 'react-router-dom'
import { Bell, LogOut, Package, ShoppingCart, LayoutDashboard, Shield } from 'lucide-react'
import { useEffect, useState } from 'react'
import client from '../api/client'

export default function Navbar() {
  const navigate = useNavigate()
  const firstName = localStorage.getItem('firstName') || 'User'
  const role = localStorage.getItem('role')
  const userId = localStorage.getItem('userId')
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    if (userId) {
      client.get(`/notifications/${userId}/unread-count`)
        .then(res => setUnreadCount(res.data.count || 0))
        .catch(() => {})
    }
  }, [userId])

  const logout = () => {
    localStorage.clear()
    navigate('/login')
  }

  return (
    <nav className="bg-slate-900 text-white px-6 py-4 flex items-center justify-between shadow-lg">
      <div className="flex items-center gap-2">
        <Package className="h-6 w-6 text-blue-400" />
        <span className="text-xl font-bold text-blue-400">NexaFlow</span>
      </div>
      <div className="flex items-center gap-6">
        <Link to="/dashboard" className="flex items-center gap-1 hover:text-blue-300 transition-colors text-sm">
          <LayoutDashboard className="h-4 w-4" /> Dashboard
        </Link>
        <Link to="/products" className="flex items-center gap-1 hover:text-blue-300 transition-colors text-sm">
          <Package className="h-4 w-4" /> Products
        </Link>
        <Link to="/orders" className="flex items-center gap-1 hover:text-blue-300 transition-colors text-sm">
          <ShoppingCart className="h-4 w-4" /> Orders
        </Link>
        {role === 'ADMIN' && (
          <Link to="/admin" className="flex items-center gap-1 hover:text-blue-300 transition-colors text-sm">
            <Shield className="h-4 w-4" /> Admin
          </Link>
        )}
        <Link to="/notifications" className="relative flex items-center gap-1 hover:text-blue-300 transition-colors text-sm">
          <Bell className="h-4 w-4" />
          {unreadCount > 0 && (
            <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full h-4 w-4 flex items-center justify-center">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Link>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-300">Hi, {firstName}</span>
          <button onClick={logout} className="flex items-center gap-1 text-slate-400 hover:text-red-400 transition-colors">
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </nav>
  )
}

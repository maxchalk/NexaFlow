import { useEffect, useState } from 'react'
import { CheckCircle, XCircle, Bell } from 'lucide-react'
import Navbar from '../components/Navbar'
import client from '../api/client'
import { Notification } from '../types'

export default function NotificationsPage() {
  const userId = localStorage.getItem('userId')
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    client.get(`/notifications/${userId}`)
      .then(res => setNotifications(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [userId])

  const markAsRead = async (id: string) => {
    try {
      await client.patch(`/notifications/${id}/read`)
      setNotifications(prev => prev.map(n => n.id === id ? {...n, read: true} : n))
    } catch (e) {
      console.error(e)
    }
  }

  const getIcon = (type: string) => {
    if (type === 'ORDER_PLACED') return <CheckCircle className="h-5 w-5 text-green-500" />
    if (type === 'ORDER_CANCELLED') return <XCircle className="h-5 w-5 text-red-500" />
    return <Bell className="h-5 w-5 text-blue-500" />
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="max-w-3xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-slate-900 mb-6">Notifications</h1>
        {loading ? (
          <div className="text-center py-16 text-slate-400">Loading...</div>
        ) : notifications.length === 0 ? (
          <div className="text-center py-16">
            <Bell className="h-12 w-12 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-400">No notifications yet.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {notifications.map(notif => (
              <div key={notif.id}
                onClick={() => !notif.read && markAsRead(notif.id)}
                className={`bg-white rounded-xl p-4 shadow-sm border cursor-pointer transition-all ${
                  notif.read ? 'border-slate-100 opacity-75' : 'border-l-4 border-blue-500 border-slate-100'
                }`}>
                <div className="flex items-start gap-3">
                  {getIcon(notif.type)}
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <p className={`font-semibold ${notif.read ? 'text-slate-600' : 'text-slate-900'}`}>{notif.title}</p>
                      <span className="text-xs text-slate-400">{new Date(notif.createdAt).toLocaleString()}</span>
                    </div>
                    <p className="text-sm text-slate-500 mt-1">{notif.message}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

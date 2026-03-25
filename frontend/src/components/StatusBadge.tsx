type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

const statusConfig: Record<OrderStatus, { label: string; className: string }> = {
  PENDING: { label: 'Pending', className: 'bg-yellow-100 text-yellow-800' },
  CONFIRMED: { label: 'Confirmed', className: 'bg-blue-100 text-blue-800' },
  SHIPPED: { label: 'Shipped', className: 'bg-purple-100 text-purple-800' },
  DELIVERED: { label: 'Delivered', className: 'bg-green-100 text-green-800' },
  CANCELLED: { label: 'Cancelled', className: 'bg-red-100 text-red-800' },
}

export default function StatusBadge({ status }: { status: string }) {
  const config = statusConfig[status as OrderStatus] ?? { label: status, className: 'bg-gray-100 text-gray-800' }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}>
      {config.label}
    </span>
  )
}

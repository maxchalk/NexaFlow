import { useEffect, useState } from 'react'
import { Search, ShoppingCart, X, Plus, Package } from 'lucide-react'
import toast from 'react-hot-toast'
import Navbar from '../components/Navbar'
import client from '../api/client'
import { Product, CartItem } from '../types'

export default function ProductsPage() {
  const role = localStorage.getItem('role')
  const [products, setProducts] = useState<Product[]>([])
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [cartOpen, setCartOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [newProduct, setNewProduct] = useState({ name: '', description: '', category: '', price: '', stockQuantity: '', imageUrl: '' })
  const [showAddProduct, setShowAddProduct] = useState(false)

  const fetchProducts = async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams()
      if (keyword) params.set('keyword', keyword)
      if (category) params.set('category', category)
      const res = await client.get(`/products/search?${params}`)
      setProducts(res.data)
    } catch (e) {
      toast.error('Failed to load products')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProducts() }, [keyword, category])

  const addToCart = (product: Product) => {
    setCart(prev => {
      const existing = prev.find(i => i.productId === product.id)
      if (existing) return prev.map(i => i.productId === product.id ? {...i, quantity: i.quantity + 1} : i)
      return [...prev, { productId: product.id, productName: product.name, quantity: 1, unitPrice: product.price }]
    })
    toast.success(`${product.name} added to cart`)
  }

  const placeOrder = async () => {
    if (cart.length === 0) { toast.error('Cart is empty'); return }
    try {
      await client.post('/orders', { items: cart })
      toast.success('Order placed successfully!')
      setCart([])
      setCartOpen(false)
    } catch (e) {
      toast.error('Failed to place order')
    }
  }

  const handleAddProduct = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await client.post('/products', { ...newProduct, price: parseFloat(newProduct.price), stockQuantity: parseInt(newProduct.stockQuantity) })
      toast.success('Product added!')
      setShowAddProduct(false)
      setNewProduct({ name: '', description: '', category: '', price: '', stockQuantity: '', imageUrl: '' })
      fetchProducts()
    } catch (e) {
      toast.error('Failed to add product')
    }
  }

  const stockBadge = (qty: number) => {
    if (qty === 0) return <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full">Out of Stock</span>
    if (qty < 5) return <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full">Low Stock ({qty})</span>
    return <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">In Stock ({qty})</span>
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-slate-900">Product Catalog</h1>
          <div className="flex gap-3">
            {role === 'ADMIN' && (
              <button onClick={() => setShowAddProduct(true)} className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg text-sm font-medium">
                <Plus className="h-4 w-4" /> Add Product
              </button>
            )}
            <button onClick={() => setCartOpen(true)} className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium">
              <ShoppingCart className="h-4 w-4" /> Cart ({cart.reduce((s, i) => s + i.quantity, 0)})
            </button>
          </div>
        </div>

        <div className="flex gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="Search products..."
              className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <select value={category} onChange={e => setCategory(e.target.value)}
            className="border border-slate-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Categories</option>
            <option value="Electronics">Electronics</option>
            <option value="Clothing">Clothing</option>
            <option value="Books">Books</option>
            <option value="Food">Food</option>
            <option value="Sports">Sports</option>
          </select>
        </div>

        {loading ? (
          <div className="text-center py-16 text-slate-400">Loading products...</div>
        ) : products.length === 0 ? (
          <div className="text-center py-16 text-slate-400">No products found.</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map(product => (
              <div key={product.id} className="bg-white rounded-xl shadow-sm border border-slate-100 p-4 flex flex-col">
                <div className="bg-slate-100 rounded-lg h-40 flex items-center justify-center mb-4">
                  {product.imageUrl ? (
                    <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover rounded-lg" />
                  ) : (
                    <Package className="h-12 w-12 text-slate-300" />
                  )}
                </div>
                <h3 className="font-semibold text-slate-900 mb-1">{product.name}</h3>
                <p className="text-slate-500 text-xs mb-2 flex-1 line-clamp-2">{product.description}</p>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-lg font-bold text-blue-600">${product.price.toFixed(2)}</span>
                  {stockBadge(product.stockQuantity)}
                </div>
                <button onClick={() => addToCart(product)} disabled={product.stockQuantity === 0}
                  className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 text-white py-2 rounded-lg text-sm font-medium transition-colors">
                  Add to Cart
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Cart Sidebar */}
      {cartOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex justify-end">
          <div className="bg-white w-full max-w-sm h-full flex flex-col shadow-2xl">
            <div className="flex items-center justify-between p-4 border-b">
              <h2 className="text-lg font-bold">Shopping Cart</h2>
              <button onClick={() => setCartOpen(false)}><X className="h-5 w-5" /></button>
            </div>
            <div className="flex-1 overflow-y-auto p-4">
              {cart.length === 0 ? (
                <p className="text-slate-400 text-center mt-8">Cart is empty</p>
              ) : cart.map(item => (
                <div key={item.productId} className="flex justify-between items-center py-3 border-b">
                  <div>
                    <p className="font-medium text-sm">{item.productName}</p>
                    <p className="text-slate-500 text-xs">Qty: {item.quantity} x ${item.unitPrice.toFixed(2)}</p>
                  </div>
                  <p className="font-bold">${(item.quantity * item.unitPrice).toFixed(2)}</p>
                </div>
              ))}
            </div>
            {cart.length > 0 && (
              <div className="p-4 border-t">
                <div className="flex justify-between mb-4">
                  <span className="font-semibold">Total:</span>
                  <span className="font-bold text-lg">${cart.reduce((s, i) => s + i.quantity * i.unitPrice, 0).toFixed(2)}</span>
                </div>
                <button onClick={placeOrder} className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-lg font-medium">
                  Place Order
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Add Product Modal */}
      {showAddProduct && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md">
            <div className="flex justify-between mb-4">
              <h2 className="text-lg font-bold">Add New Product</h2>
              <button onClick={() => setShowAddProduct(false)}><X className="h-5 w-5" /></button>
            </div>
            <form onSubmit={handleAddProduct} className="space-y-3">
              <input value={newProduct.name} onChange={e => setNewProduct({...newProduct, name: e.target.value})} required placeholder="Product name"
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              <textarea value={newProduct.description} onChange={e => setNewProduct({...newProduct, description: e.target.value})} placeholder="Description"
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 h-20" />
              <input value={newProduct.category} onChange={e => setNewProduct({...newProduct, category: e.target.value})} placeholder="Category"
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              <div className="grid grid-cols-2 gap-2">
                <input type="number" step="0.01" value={newProduct.price} onChange={e => setNewProduct({...newProduct, price: e.target.value})} required placeholder="Price"
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                <input type="number" value={newProduct.stockQuantity} onChange={e => setNewProduct({...newProduct, stockQuantity: e.target.value})} required placeholder="Stock"
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <input value={newProduct.imageUrl} onChange={e => setNewProduct({...newProduct, imageUrl: e.target.value})} placeholder="Image URL (optional)"
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              <button type="submit" className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg font-medium text-sm">Add Product</button>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

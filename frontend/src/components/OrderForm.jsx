import { useMemo, useState } from 'react'
import api from '../services/api'

const OrderForm = ({ tradingPair, marketPrice, bidPrice, askPrice, onOrderPlaced }) => {
  const [orderType, setOrderType] = useState('market')
  const [side, setSide] = useState('buy')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [leverage, setLeverage] = useState(1)
  const [takeProfit, setTakeProfit] = useState('')
  const [stopLoss, setStopLoss] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const toFiniteNumber = (value, fallback = 0) => {
    const num = Number(value)
    return Number.isFinite(num) ? num : fallback
  }

  const currentPrice = toFiniteNumber(side === 'buy' ? askPrice ?? marketPrice : bidPrice ?? marketPrice, 0)
  const hasLivePrice = currentPrice > 0
  const qty = Number(quantity || 0)
  const notionalValue = qty * currentPrice
  const marginRequired = leverage > 0 ? notionalValue / leverage : 0
  const liquidationPrice = qty > 0
    ? side === 'buy'
      ? currentPrice * (1 - 1 / Math.max(leverage, 1))
      : currentPrice * (1 + 1 / Math.max(leverage, 1))
    : 0

  const submitLabel = useMemo(() => `${side === 'buy' ? 'Buy' : 'Sell'} ${tradingPair.split('/')[0]}`, [side, tradingPair])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    if (!Number.isFinite(qty) || qty <= 0) {
      setError('Enter a valid quantity greater than 0')
      return
    }

    if (orderType === 'market' && !hasLivePrice) {
      setError('Market price unavailable. Please wait for live price data and try again.')
      return
    }

    if (orderType === 'limit' && (!Number.isFinite(Number(price)) || Number(price) <= 0)) {
      setError('Enter a valid limit price greater than 0')
      return
    }

    setLoading(true)

    try {
      const orderData = {
        tradingPair,
        type: orderType.toUpperCase(),
        side: side.toUpperCase(),
        quantity: parseFloat(quantity),
        price: orderType === 'limit' ? parseFloat(price) : null,
      }

      const response = await api.post('/orders', orderData)
      setQuantity('')
      setPrice('')
      setTakeProfit('')
      setStopLoss('')
      if (onOrderPlaced) {
        onOrderPlaced(response.data)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to place order')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bg-dark-800 rounded-2xl border border-dark-600/50 p-5 text-sm">
      <div className="flex justify-between items-center mb-5">
        <span className="text-lg font-semibold text-white">{tradingPair.split('/')[0]}</span>
        <span className="text-2xl font-mono font-bold text-white">
          {hasLivePrice
            ? `$${currentPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
            : '--'}
        </span>
      </div>

      {error && <div className="mb-3 rounded-lg bg-red-500/10 border border-red-500/30 px-3 py-2 text-danger">{error}</div>}
      {orderType === 'market' && !hasLivePrice && (
        <div className="mb-3 rounded-lg bg-amber-500/10 border border-amber-500/30 px-3 py-2 text-amber-300">
          Live market price is unavailable. Start backend data feed or switch to Limit order.
        </div>
      )}

      <div className="grid grid-cols-2 gap-2 mb-4">
        <button
          type="button"
          onClick={() => setSide('buy')}
          className={`py-3 rounded-xl font-semibold transition-all ${side === 'buy' ? 'bg-success text-white' : 'bg-dark-700 text-muted hover:bg-dark-600 hover:text-white'}`}
        >
          Buy
        </button>
        <button
          type="button"
          onClick={() => setSide('sell')}
          className={`py-3 rounded-xl font-semibold transition-all ${side === 'sell' ? 'bg-danger text-white' : 'bg-dark-700 text-muted hover:bg-dark-600 hover:text-white'}`}
        >
          Sell
        </button>
      </div>

      <div className="grid grid-cols-2 rounded-xl overflow-hidden mb-4">
        <button
          type="button"
          onClick={() => setOrderType('market')}
          className={`py-2.5 font-medium transition-colors ${orderType === 'market' ? 'bg-dark-600 text-white' : 'bg-dark-700 text-muted'}`}
        >
          Market
        </button>
        <button
          type="button"
          onClick={() => setOrderType('limit')}
          className={`py-2.5 font-medium transition-colors ${orderType === 'limit' ? 'bg-dark-600 text-white' : 'bg-dark-700 text-muted'}`}
        >
          Limit
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="text-muted text-sm mb-2 block">Quantity ({tradingPair.split('/')[0]})</label>
          <input
            type="number"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="0.00"
            step="0.00000001"
            required
            className="input-field"
          />
        </div>

        {orderType === 'limit' && (
          <div>
            <label className="text-muted text-sm mb-2 block">Limit Price</label>
            <input
              type="number"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="0.00"
              step="0.01"
              required
              className="input-field"
            />
          </div>
        )}

        <div>
          <label className="text-muted text-sm mb-2 flex items-center justify-between">
            <span>Leverage</span>
            <span className="text-white font-semibold">{leverage}x</span>
          </label>
          <input
            type="range"
            min="1"
            max="100"
            value={leverage}
            onChange={(e) => setLeverage(Number(e.target.value))}
            className="w-full"
          />
          <div className="flex justify-between text-xs text-muted mt-1">
            <span>1x</span>
            <span>25x</span>
            <span>50x</span>
            <span>75x</span>
            <span>100x</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-muted text-sm mb-2 block">Take Profit</label>
            <input
              type="number"
              value={takeProfit}
              onChange={(e) => setTakeProfit(e.target.value)}
              placeholder="Optional"
              className="input-field"
            />
          </div>
          <div>
            <label className="text-muted text-sm mb-2 block">Stop Loss</label>
            <input
              type="number"
              value={stopLoss}
              onChange={(e) => setStopLoss(e.target.value)}
              placeholder="Optional"
              className="input-field"
            />
          </div>
        </div>

        <div className="rounded-xl border border-dark-600/50 bg-dark-700/30 p-4 space-y-2.5">
          <div className="flex justify-between"><span className="text-muted">Market Price</span><span className="font-mono text-white">{hasLivePrice ? `$${currentPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : '--'}</span></div>
          <div className="flex justify-between"><span className="text-muted">Notional Value</span><span className="font-mono text-white">${notionalValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></div>
          <div className="flex justify-between"><span className="text-muted">Liquidation Price (Est.)</span><span className="font-mono text-danger">${liquidationPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></div>
          <div className="border-t border-dark-600/40 my-2" />
          <div className="flex justify-between"><span className="text-muted">Margin Required</span><span className="font-mono font-semibold text-white">${marginRequired.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className={`w-full py-4 rounded-xl font-semibold text-base transition-all disabled:opacity-50 disabled:cursor-not-allowed ${side === 'buy' ? 'bg-success text-white hover:bg-success/90 border-2 border-success' : 'bg-danger text-white hover:bg-danger/90 border-2 border-danger'}`}
        >
          {loading ? 'Processing...' : submitLabel}
        </button>
      </form>
    </div>
  )
}

export default OrderForm

import { useEffect, useMemo, useState } from 'react'
import api from '../services/api'
import { usePortfolioStore } from '../store/portfolioStore'

const PortfolioPage = () => {
  const { portfolio, loading, fetchPortfolio } = usePortfolioStore()
  const [asset, setAsset] = useState('USDC')
  const [amount, setAmount] = useState('')
  const [depositLoading, setDepositLoading] = useState(false)
  const [depositError, setDepositError] = useState('')
  const [depositSuccess, setDepositSuccess] = useState('')

  useEffect(() => {
    fetchPortfolio()
  }, [])

  const balances = useMemo(() => {
    if (!portfolio?.holdings) return []
    return portfolio.holdings.map((holding) => ({
      symbol: holding.currency,
      amount: Number(holding.quantity || 0),
      value: Number(holding.totalValue || 0),
    }))
  }, [portfolio])

  const handleDeposit = async (e) => {
    e.preventDefault()
    setDepositError('')
    setDepositSuccess('')

    const parsedAmount = Number(amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setDepositError('Amount must be greater than 0')
      return
    }

    setDepositLoading(true)
    try {
      await api.post('/portfolio/deposit', {
        currency: asset,
        amount: parsedAmount,
      })
      setAmount('')
      setDepositSuccess('Deposit completed successfully')
      await fetchPortfolio()
    } catch (error) {
      setDepositError(error.response?.data?.message || 'Deposit failed')
    } finally {
      setDepositLoading(false)
    }
  }

  if (loading) {
    return <div className="p-6 text-muted">Loading wallet...</div>
  }

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Wallet</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-dark-800 rounded-xl border border-dark-600/50 p-6">
          <h2 className="text-lg font-semibold mb-4">Assets</h2>

          {balances.length === 0 ? (
            <p className="text-muted text-sm">No balances found.</p>
          ) : (
            <div className="space-y-3">
              {balances.map((item) => (
                <div key={item.symbol} className="bg-dark-700/50 rounded-lg p-3 flex items-center justify-between">
                  <div>
                    <p className="font-medium text-white">{item.symbol}</p>
                    <p className="text-muted text-sm">{item.amount.toFixed(6)}</p>
                  </div>
                  <p className="font-mono text-white">${item.value.toFixed(2)}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-dark-800 rounded-xl border border-dark-600/50 p-6 h-fit">
          <h2 className="text-lg font-semibold mb-4">Deposit Funds</h2>

          <form onSubmit={handleDeposit} className="space-y-4">
            <div>
              <label className="block text-muted mb-2">Asset</label>
              <select
                value={asset}
                onChange={(e) => setAsset(e.target.value)}
                className="input-field"
              >
                <option value="USDC">USDC</option>
                <option value="BTC">BTC</option>
                <option value="ETH">ETH</option>
                <option value="SOL">SOL</option>
              </select>
            </div>

            <div>
              <label className="block text-muted mb-2">Amount</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
                className="input-field"
              />
            </div>

            <button
              type="submit"
              disabled={depositLoading}
              className="w-full py-3 rounded-xl bg-accent text-white text-3xl font-semibold hover:bg-blue-500 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {depositLoading ? 'Depositing...' : 'Deposit'}
            </button>

            {depositError && <p className="text-danger text-sm">{depositError}</p>}
            {depositSuccess && <p className="text-success text-sm">{depositSuccess}</p>}

            <p className="text-muted text-sm leading-relaxed">
              * This is a simulation. Funds are added to your virtual wallet balance instantly.
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}

export default PortfolioPage

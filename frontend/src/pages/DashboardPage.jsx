import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMarketStore } from '../store/marketStore'
import { usePortfolioStore } from '../store/portfolioStore'

const DashboardPage = () => {
  const { prices, fetchPrices } = useMarketStore()
  const { portfolio, fetchPortfolio } = usePortfolioStore()
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadData = async () => {
      setLoading(true)
      await Promise.all([
        fetchPrices('BTC/USDT'),
        fetchPrices('ETH/USDT'),
        fetchPrices('BNB/USDT'),
        fetchPrices('XRP/USDT'),
        fetchPortfolio(),
      ])
      setLoading(false)
    }
    loadData()
  }, [])

  const tradingPairs = ['BTC/USDT', 'ETH/USDT', 'BNB/USDT', 'XRP/USDT', 'ADA/USDT']

  if (loading) {
    return <div className="p-6 text-muted">Loading dashboard...</div>
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Trading Dashboard</h1>

      {/* Portfolio Summary */}
      {portfolio && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="card">
            <h3 className="text-muted text-sm mb-1">Total Balance</h3>
            <p className="text-3xl font-bold text-accent">
              ${parseFloat(portfolio.totalBalance).toFixed(2)}
            </p>
          </div>
          <div className="card">
            <h3 className="text-muted text-sm mb-1">Available Balance</h3>
            <p className="text-3xl font-bold text-success">
              ${parseFloat(portfolio.availableBalance).toFixed(2)}
            </p>
          </div>
          <div className="card">
            <h3 className="text-muted text-sm mb-1">Holdings</h3>
            <p className="text-3xl font-bold text-white">
              {portfolio.holdings?.length || 0} coins
            </p>
          </div>
        </div>
      )}

      {/* Trading Pairs */}
      <div className="mb-8">
        <h2 className="text-xl font-bold mb-4">Market Overview</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {tradingPairs.map((pair) => {
            const priceData = prices[pair]
            return (
              <Link
                key={pair}
                to={`/trading/${pair.replace('/', '-')}`}
                className="card hover:bg-dark-700/80 transition cursor-pointer"
              >
                <h3 className="font-bold text-lg mb-2">{pair}</h3>
                <div className="flex justify-between items-end">
                  <div>
                    <p className="text-muted text-sm">Price</p>
                    <p className="text-2xl font-bold">
                      ${priceData?.price ? parseFloat(priceData.price).toFixed(2) : 'Loading...'}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-muted text-sm">Bid / Ask</p>
                    <p className="text-sm">
                      {priceData
                        ? `${parseFloat(priceData.bidPrice).toFixed(2)} / ${parseFloat(priceData.askPrice).toFixed(2)}`
                        : 'Loading...'}
                    </p>
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="card">
        <h2 className="text-xl font-bold mb-4">Quick Actions</h2>
        <div className="flex gap-4">
          <Link to="/portfolio" className="btn-primary">
            View Portfolio
          </Link>
          <Link to="/trading/BTC-USDT" className="btn-secondary">
            Start Trading
          </Link>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage

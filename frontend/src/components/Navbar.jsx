import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useMarketStore } from '../store/marketStore'
import { usePortfolioStore } from '../store/portfolioStore'

const Navbar = () => {
  const { user, logout } = useAuthStore()
  const location = useLocation()
  const { prices, fetchPrices } = useMarketStore()
  const { portfolio, fetchPortfolio } = usePortfolioStore()

  const pathPair = location.pathname.startsWith('/trading/')
    ? location.pathname.split('/trading/')[1]?.replace('-', '/')
    : 'BTC/USDT'

  const currentPair = pathPair || 'BTC/USDT'
  const currentPrice = prices[currentPair]?.price

  useEffect(() => {
    fetchPrices(currentPair)
    fetchPortfolio()
  }, [currentPair])

  return (
    <nav className="h-16 border-b border-dark-600/50 bg-dark-900/95 backdrop-blur-xl">
      <div className="h-full px-4 md:px-6 flex items-center justify-between">
        <div className="flex items-center gap-6 min-w-0">
          <Link to="/trading/BTC-USDT" className="text-2xl font-bold tracking-tight text-accent leading-none">
            TradeNova
          </Link>
          <div className="hidden md:block h-8 w-px bg-dark-600/50" />
          <div className="hidden md:block">
            <p className="text-sm text-muted">{currentPair.replace('USDT', 'USDC')}</p>
            <p className="text-3xl leading-none font-bold text-white">
              ${currentPrice ? Number(currentPrice).toLocaleString() : '0.00'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 md:gap-6">
          <div className="text-right hidden sm:block">
            <p className="text-sm text-muted">Wallet Balance</p>
            <p className="text-3xl leading-none font-bold text-white">
              ${portfolio?.availableBalance ? Number(portfolio.availableBalance).toLocaleString() : '0.00'}
            </p>
          </div>

          <Link
            to="/portfolio"
            className="px-4 py-2 rounded-xl border border-dark-600/70 text-gray-300 hover:text-white hover:border-dark-500 transition-colors"
          >
            Wallet
          </Link>

          <button
            onClick={logout}
            className="text-gray-300 hover:text-white transition-colors text-sm md:text-base"
          >
            Log Out
          </button>
        </div>
      </div>
    </nav>
  )
}

export default Navbar

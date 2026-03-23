import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMarketStore } from '../store/marketStore'
import CandlestickChart from '../components/CandlestickChart'
import OrderForm from '../components/OrderForm'
import OrderHistory from '../components/OrderHistory'

const TradingPage = () => {
  const { pair } = useParams()
  const tradingPair = pair.replace('-', '/')
  const {
    prices,
    recentTrades,
    userOrders,
    candlesticks,
    fetchPrices,
    fetchRecentTrades,
    fetchUserOrders,
    fetchCandlesticks,
    setSelectedPair,
  } = useMarketStore()
  const [interval, setInterval] = useState('1h')
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  const watchlistPairs = ['SOL/USDT', 'BTC/USDT', 'ETH/USDT']

  useEffect(() => {
    setSelectedPair(tradingPair)
    loadMarketData(tradingPair, interval)
    fetchUserOrders()
  }, [tradingPair])

  useEffect(() => {
    watchlistPairs.forEach((watchPair) => {
      fetchPrices(watchPair)
    })
  }, [])

  useEffect(() => {
    const pollId = setInterval(() => {
      loadMarketData(tradingPair, interval)
      watchlistPairs.forEach((watchPair) => fetchPrices(watchPair))
    }, 3000)

    return () => clearInterval(pollId)
  }, [tradingPair, interval])

  useEffect(() => {
    loadMarketData(tradingPair, interval)
  }, [interval])

  const loadMarketData = async (pairValue, intervalValue) => {
    setLoading(true)
    await Promise.all([
      fetchPrices(pairValue),
      fetchRecentTrades(pairValue),
      fetchCandlesticks(pairValue, intervalValue),
    ])
    setLoading(false)
  }

  const priceData = prices[tradingPair]
  const filteredPairs = useMemo(
    () => watchlistPairs.filter((item) => item.toLowerCase().includes(search.toLowerCase())),
    [search]
  )

  const intervals = ['1m', '5m', '30m', '1h', '4h', '1d', '1w']

  const renderPrice = (value, fallback = '0.00') =>
    value
      ? Number(value).toLocaleString(undefined, {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })
      : fallback

  return (
    <div className="flex h-[calc(100vh-64px)] overflow-hidden bg-dark-900">
      <div className="hidden md:flex w-[292px] border-r border-dark-600/50 flex-col bg-dark-800/30">
        <div className="p-3 border-b border-dark-600/50">
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search"
            className="w-full bg-dark-700 border border-dark-600/50 rounded-xl px-3 py-2 text-sm text-white placeholder:text-muted focus:border-accent/40 focus:outline-none"
          />
        </div>

        <div className="grid grid-cols-3 px-4 py-3 text-sm text-muted border-b border-dark-600/30">
          <span>SYMBOL</span>
          <span className="text-right">BID</span>
          <span className="text-right">ASK</span>
        </div>

        <div className="overflow-y-auto">
          {filteredPairs.map((watchPair) => {
            const short = watchPair.split('/')[0]
            const watchPrice = prices[watchPair]
            const isActive = watchPair === tradingPair
            return (
              <Link
                key={watchPair}
                to={`/trading/${watchPair.replace('/', '-')}`}
                className={`grid grid-cols-3 px-4 py-4 border-b border-dark-600/20 hover:bg-dark-700/50 transition-colors ${
                  isActive ? 'bg-dark-700/70' : ''
                }`}
              >
                <span className="font-semibold text-gray-200">{short}</span>
                <span className="text-right text-success font-mono">{renderPrice(watchPrice?.bidPrice)}</span>
                <span className="text-right text-danger font-mono">{renderPrice(watchPrice?.askPrice)}</span>
              </Link>
            )
          })}
        </div>
      </div>

      <div className="flex-1 min-w-0 flex flex-col border-r border-dark-600/30">
          {/* chart section */}
          <section className="flex-1 min-h-0">
            <div className="border-b border-dark-600/40 bg-[#0a0a0b] px-5 py-3 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <h2 className="text-2xl font-bold text-white">{tradingPair.split('/')[0]}</h2>
                <div className="flex gap-1 bg-dark-800/30 rounded-lg p-1">
                  {intervals.map((int) => (
                    <button
                      key={int}
                      onClick={() => setInterval(int)}
                      className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
                        interval === int ? 'bg-dark-700 text-white' : 'text-muted/80 hover:text-white hover:bg-dark-700/50'
                      }`}
                    >
                      {int}
                    </button>
                  ))}
                </div>
              </div>
              <p className="text-muted text-sm">{loading ? 'Loading chart data...' : ''}</p>
            </div>

            <div className="flex-1 min-h-[300px] bg-[#0a0a0b] px-2 py-2">
              {loading ? (
                <div className="h-full rounded-xl border border-dark-600/40 bg-dark-800/20" />
              ) : (
                <CandlestickChart data={candlesticks} interval={interval} livePrice={priceData?.price} />
              )}
            </div>
          </section>

          {/* order history section located beneath the chart */}
          <section className="h-[38%] min-h-[220px] border-t border-dark-600/40 bg-dark-800/20">
            <div className="flex border-b border-dark-600/40 text-sm md:text-xl">
              <button className="px-6 py-4 font-semibold border-b-2 border-accent text-white">
                Order History
              </button>
            </div>
            <div className="h-[calc(100%-48px)]">
              <OrderHistory orders={userOrders} />
            </div>
          </section>
      </div>

      <div className="w-[340px] hidden lg:block p-4 overflow-y-auto bg-dark-900">
        <OrderForm
          tradingPair={tradingPair}
          marketPrice={priceData?.price}
          bidPrice={priceData?.bidPrice}
          askPrice={priceData?.askPrice}
          onOrderPlaced={() => {
            loadMarketData(tradingPair, interval)
            fetchUserOrders()
          }}
        />
      </div>

      <div className="lg:hidden fixed bottom-0 left-0 right-0 p-3 bg-dark-900/95 border-t border-dark-600/50">
        <Link to="/portfolio" className="block text-center py-3 rounded-xl border border-accent/40 text-accent font-semibold">
          Wallet
        </Link>
      </div>
    </div>
  )
}

export default TradingPage

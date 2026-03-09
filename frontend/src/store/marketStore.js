import { create } from 'zustand'
import api from '../services/api'

export const useMarketStore = create((set) => ({
  prices: {},
  orderBook: null,
  recentTrades: [],
  userOrders: [],
  candlesticks: [],
  selectedPair: 'BTC/USDT',
  loading: false,

  fetchPrices: async (tradingPair) => {
    set({ loading: true })
    try {
      const response = await api.get('/market/price', {
        params: { tradingPair },
      })
      set((state) => ({
        prices: { ...state.prices, [tradingPair]: response.data },
        loading: false,
      }))
    } catch (error) {
      console.error('Failed to fetch price', error)
      set({ loading: false })
    }
  },

  fetchOrderBook: async (tradingPair) => {
    try {
      const response = await api.get('/market/orderbook', {
        params: { tradingPair },
      })
      set({ orderBook: response.data })
    } catch (error) {
      console.error('Failed to fetch order book', error)
    }
  },

  fetchRecentTrades: async (tradingPair) => {
    try {
      const response = await api.get(`/trades/pair/${tradingPair}/recent`)
      set({ recentTrades: response.data })
    } catch (error) {
      console.error('Failed to fetch recent trades', error)
    }
  },

  fetchUserOrders: async (page = 0, size = 20) => {
    try {
      const response = await api.get('/orders', { params: { page, size } })
      // backend returns a Page<OrderDto>; grab the content array
      set({ userOrders: response.data.content || [] })
    } catch (error) {
      console.error('Failed to fetch user orders', error)
    }
  },

  fetchCandlesticks: async (tradingPair, interval = '1h', limit = 100) => {
    try {
      const response = await api.get('/market/candlesticks', {
        params: { tradingPair, interval, limit },
      })
      set({ candlesticks: response.data })
    } catch (error) {
      console.error('Failed to fetch candlesticks', error)
    }
  },

  setSelectedPair: (pair) => set({ selectedPair: pair }),

  updatePrice: (pair, price) =>
    set((state) => ({
      prices: {
        ...state.prices,
        [pair]: { ...state.prices[pair], price: price.price },
      },
    })),
}))

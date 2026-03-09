import { create } from 'zustand'
import api from '../services/api'

export const usePortfolioStore = create((set) => ({
  portfolio: null,
  loading: false,
  error: null,

  fetchPortfolio: async () => {
    set({ loading: true, error: null })
    try {
      const response = await api.get('/portfolio')
      set({ portfolio: response.data, loading: false })
    } catch (error) {
      set({ error: error.response?.data?.message || 'Failed to fetch portfolio', loading: false })
    }
  },

  updatePortfolio: (newPortfolio) => set({ portfolio: newPortfolio }),
}))

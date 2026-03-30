import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import api from '../services/api'

export const useAuthStore = create(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: false,
      error: null,

      login: async (identifier, password) => {
        set({ loading: true, error: null })
        try {
          const response = await api.post('/auth/login', { identifier: identifier.trim(), password })
          const { token, user } = response.data
          localStorage.setItem('token', token)
          set({ user, token, isAuthenticated: true, loading: false })
          return { success: true }
        } catch (error) {
          const message = error.response?.data?.message || 'Login failed'
          set({ error: message, loading: false })
          return { success: false, message }
        }
      },

      register: async (userData) => {
        set({ loading: true, error: null })
        try {
          const response = await api.post('/auth/register', userData)
          set({ user: response.data, loading: false })
          return { success: true }
        } catch (error) {
          const validationErrors = error.response?.data?.validationErrors
          const status = error.response?.status
          const backendMessage =
            typeof error.response?.data === 'string'
              ? error.response.data
              : error.response?.data?.message

          let message = backendMessage || 'Registration failed'

          if (validationErrors && typeof validationErrors === 'object') {
            const firstValidationError = Object.values(validationErrors)[0]
            if (typeof firstValidationError === 'string') {
              message = firstValidationError
            }
          }

          if (!error.response) {
            message = 'Cannot reach server. Please try again.'
          } else if (!validationErrors && !backendMessage && status) {
            message = `Registration failed (HTTP ${status})`
          }

          set({ error: message, loading: false })
          return { success: false, message }
        }
      },

      logout: () => {
        localStorage.removeItem('token')
        set({ user: null, token: null, isAuthenticated: false })
      },

      loadUser: async () => {
        const token = localStorage.getItem('token')
        if (token) {
          set({ token, isAuthenticated: true })
          try {
            const response = await api.get('/auth/me')
            set({ user: response.data })
          } catch (error) {
            console.error('Failed to load user', error)
            localStorage.removeItem('token')
            set({ token: null, isAuthenticated: false })
          }
        }
      },

      setError: (error) => set({ error }),
      clearError: () => set({ error: null }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ token: state.token, user: state.user }),
    }
  )
)

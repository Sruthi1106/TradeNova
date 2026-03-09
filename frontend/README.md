# TT Crypto Trading Platform - Frontend

A modern React + Vite frontend for cryptocurrency trading with real-time price updates, advanced charting, and responsive UI design.

## Technology Stack

- **Framework**: React 18.2.0
- **Build Tool**: Vite 5.0
- **Styling**: Tailwind CSS 3.4
- **State Management**: Zustand 4.4
- **HTTP Client**: Axios 1.6
- **Charts**: Recharts 2.10
- **WebSocket**: SockJS + Stomp
- **Routing**: React Router 6.20
- **Utilities**: Date-fns 2.30

## Project Structure

```
frontend/
├── src/
│   ├── components/         # Reusable React components
│   │   ├── CandlestickChart.jsx
│   │   ├── OrderBook.jsx
│   │   ├── OrderForm.jsx
│   │   ├── RecentTrades.jsx
│   │   ├── Navbar.jsx
│   │   └── ProtectedRoute.jsx
│   ├── pages/             # Page components
│   │   ├── LoginPage.jsx
│   │   ├── RegisterPage.jsx
│   │   ├── DashboardPage.jsx
│   │   ├── TradingPage.jsx
│   │   └── PortfolioPage.jsx
│   ├── services/          # API and WebSocket services
│   │   ├── api.js
│   │   └── websocket.js
│   ├── store/            # Zustand stores
│   │   ├── authStore.js
│   │   ├── marketStore.js
│   │   └── portfolioStore.js
│   ├── styles/           # Global styles
│   │   └── index.css
│   ├── App.jsx           # Main App component
│   └── main.jsx          # Entry point
├── public/               # Static assets
├── index.html            # HTML template
├── vite.config.js        # Vite configuration
├── tailwind.config.js    # Tailwind configuration
├── postcss.config.js     # PostCSS configuration
├── package.json
└── README.md
```

## Prerequisites

- Node.js 16.0 or higher
- npm 8.0 or higher
- Backend API running on `http://localhost:8080`

## Setup Instructions

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Configure Environment Variables

Create a `.env` file in the frontend directory (if needed):

```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws/trading
```

### 3. Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### 4. Build for Production

```bash
npm run build
```

Output will be in the `dist/` directory.

### 5. Preview Production Build

```bash
npm run preview
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm run format` - Format code with Prettier

## Component Overview

### Pages

**LoginPage**
- User authentication
- Demo credentials display
- Form validation
- Error handling

**RegisterPage**
- New user registration
- Form validation
- Email and username uniqueness checks
- Password strength requirements

**DashboardPage**
- Portfolio overview
- Market overview with live prices
- Quick access to main features
- Holdings summary

**TradingPage**
- Real-time candlestick charts
- Order book display (bids/asks)
- Recent trades list
- Order placement form
- Multiple timeframe charts (1m, 5m, 15m, 1h, 1d)

**PortfolioPage**
- Portfolio summary (total balance, P&L)
- Holdings table with details
- Asset allocation visualization
- Real-time balance updates

### Components

**CandlestickChart**
- Displays candlestick/OHLCV data
- Volume bars
- Multiple indicators
- Responsive design
- Uses Recharts library

**OrderBook**
- Bids (buy orders) on the left
- Asks (sell orders) on the right
- Real-time updates
- Scrollable display
- Color-coded (green for bids, red for asks)

**OrderForm**
- Buy/Sell toggle
- Market/Limit order types
- Quantity and price inputs
- Order validation
- Real-time feedback

**RecentTrades**
- Shows latest executed trades
- Time, price, quantity, total
- Sortable and scrollable
- Auto-updates via WebSocket

**Navbar**
- Navigation links
- User menu
- Logout functionality
- Logo/branding

**ProtectedRoute**
- Guards authenticated routes
- Redirects to login if not authenticated

## Store Management (Zustand)

### authStore
Manages authentication state and operations.

```javascript
// Login
await login(identifier, password)

// Register
await register(userData)

// Logout
logout()

// Load user from localStorage
loadUser()

// State
user, token, isAuthenticated, loading, error
```

### marketStore
Manages market data and price information.

```javascript
// Fetch data
await fetchPrices(tradingPair)
await fetchOrderBook(tradingPair)
await fetchRecentTrades(tradingPair)
await fetchCandlesticks(tradingPair, interval, limit)

// State
prices, orderBook, recentTrades, candlesticks, selectedPair, loading
```

### portfolioStore
Manages user portfolio information.

```javascript
// Fetch portfolio
await fetchPortfolio()

// Update portfolio
updatePortfolio(newPortfolio)

// State
portfolio, loading, error
```

## API Service

The `api.js` service provides axios instance with:
- Base URL configuration
- Automatic token injection in Authorization header
- Error handling and 401 redirect
- Request/Response interceptors

## WebSocket Service

The `websocket.js` service handles:
- STOMP over SockJS connection
- Auto-reconnection
- Topic subscriptions
- Message broadcasting

**Usage:**
```javascript
// Connect
websocket.connect(onConnect, onMessage, onError)

// Subscribe
const subscription = websocket.subscribe('/topic/price/BTC/USDT', (data) => {
  console.log('Price update:', data)
})

// Send message
websocket.send('/app/subscribe', { tradingPair: 'BTC/USDT' })

// Disconnect
websocket.disconnect()
```

## Styling

### Tailwind CSS Classes

**Custom utilities defined:**
- `.card` - Card container styling
- `.btn-primary` - Primary action button
- `.btn-secondary` - Secondary action button
- `.btn-success` - Success (buy) button
- `.btn-danger` - Danger (sell) button
- `.input-field` - Form input styling
- `.table-row` - Hover effects for tables
- `.text-success`, `.text-danger`, `.text-warning` - Color utility
- `.scrollbar-hide` - Hide scrollbars

**Color Scheme:**
- `bg-primary` - Dark gray (#1f2937)
- `bg-secondary` - Darker gray (#111827)
- `text-accent` - Blue (#3b82f6)
- Success - Green (#10b981)
- Danger - Red (#ef4444)
- Warning - Amber (#f59e0b)

## Authentication Flow

1. User registers or logs in
2. Backend returns JWT token
3. Frontend stores token in localStorage
4. Token automatically included in all requests
5. If 401 received, redirect to login
6. On app load, check localStorage for existing token

## Real-time Updates

The application uses WebSocket for:
- Live price ticks
- Order book updates
- Trade notifications
- Portfolio changes

Events are published to:
- `/topic/price/{pair}` - Individual price updates
- `/topic/orders/{pair}` - Order book changes
- `/topic/trades` - Execution notifications

## Features

✅ User authentication with JWT
✅ Real-time price charts with Recharts
✅ Order book display
✅ Recent trades feed
✅ Place market and limit orders
✅ Portfolio overview and holdings
✅ Responsive mobile design
✅ Dark theme UI
✅ WebSocket real-time updates
✅ Error handling and validation
✅ Loading states
✅ Order history
✅ Multiple timeframe charts
✅ Auto-refresh market data
✅ Protected routes

## Production Deployment

### Build Docker Image

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "run", "preview"]
```

### Environment Configuration

For production, ensure backend URL is correctly set:
```env
VITE_API_URL=https://api.yourdomain.com
VITE_WS_URL=wss://api.yourdomain.com/ws/trading
```

### Nginx Configuration

```nginx
server {
    listen 80;
    server_name trading.yourdomain.com;

    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Performance Optimization

- Code splitting with React.lazy()
- Image optimization
- CSS minification via Tailwind
- Gzip compression
- Browser caching
- API response caching

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers

## Troubleshooting

### API Connection Failed
- Verify backend is running on port 8080
- Check CORS configuration
- Verify API_URL in environment

### WebSocket Connection Issues
- Ensure WebSocket proxy is configured
- Check browser console for connection errors
- Verify server is in `/ws/trading` endpoint

### Token Not Being Sent
- Check localStorage for token
- Verify Authorization header format
- Clear browser cache and retry

### Charts Not Displaying
- Verify candlestick data is being fetched
- Check browser console for errors
- Ensure Recharts is properly installed

## Common Issues

**CORS Error**
- Backend CORS settings may need adjustment
- Check cors.allowed-origins in application.properties

**401 Unauthorized**
- Token may have expired
- Check token in localStorage
- Log out and log back in

**WebSocket Timeout**
- Network connectivity issue
- Verify firewall allows WebSocket
- Check server logs

## License

Proprietary - TT Crypto Trading Platform

## Support

For issues and questions, please refer to the backend documentation or contact support.

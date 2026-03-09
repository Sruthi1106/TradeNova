import { useEffect, useMemo, useRef, useState } from 'react'

const CandlestickChart = ({ data, interval = '1h' }) => {
  const containerRef = useRef(null)
  const [size, setSize] = useState({ width: 900, height: 450 })
  const [hoverIndex, setHoverIndex] = useState(null)

  if (!data || data.length === 0) {
    return <div className="text-gray-400 p-4">Loading chart...</div>
  }

  const formatXAxis = (value) => {
    const date = new Date(value)
    if (interval === '1d' || interval === '1w') {
      return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
    }
    if (interval === '4h' || interval === '1h' || interval === '30m') {
      return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
    }
    return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', hour12: false })
  }

  const chartData = useMemo(
    () =>
      data.map((candle) => {
        const open = Number(candle.open)
        const close = Number(candle.close)
        const high = Number(candle.high)
        const low = Number(candle.low)
        return {
          time: Number(candle.openTime),
          open,
          close,
          high,
          low,
          isBullish: close >= open,
        }
      }),
    [data]
  )

  useEffect(() => {
    if (!containerRef.current) return

    const observer = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (!entry) return
      const { width, height } = entry.contentRect
      setSize({
        width: Math.max(320, Math.floor(width)),
        height: Math.max(260, Math.floor(height)),
      })
    })

    observer.observe(containerRef.current)
    return () => observer.disconnect()
  }, [])

  const allPrices = chartData.flatMap((d) => [d.high, d.low])
  const minPrice = Math.min(...allPrices)
  const maxPrice = Math.max(...allPrices)
  const range = Math.max(maxPrice - minPrice, 1)
  const padding = range * 0.1
  const domainMin = minPrice - padding
  const domainMax = maxPrice + padding

  // Use only the latest candle for current price marker.
  const currentPrice = chartData[chartData.length - 1]?.close || maxPrice

  const margin = { top: 14, right: 84, bottom: 28, left: 14 }
  const plotWidth = Math.max(10, size.width - margin.left - margin.right)
  const plotHeight = Math.max(10, size.height - margin.top - margin.bottom)
  const step = plotWidth / Math.max(chartData.length, 1)
  const candleWidth = Math.max(3, Math.min(12, step * 0.62))

  const yForPrice = (price) =>
    margin.top + ((domainMax - price) / Math.max(domainMax - domainMin, 1)) * plotHeight

  const xForIndex = (index) => margin.left + step * index + step / 2

  const yTicks = 8
  const xTicks = Math.min(11, chartData.length)

  const hoveredCandle = hoverIndex !== null ? chartData[hoverIndex] : null
  const tooltipX =
    hoverIndex !== null
      ? Math.min(size.width - 190, Math.max(12, xForIndex(hoverIndex) + 12))
      : 0

  return (
    <div
      ref={containerRef}
      className="relative w-full h-[450px] rounded-lg bg-[#0b0e14]"
      onMouseLeave={() => setHoverIndex(null)}
      onMouseMove={(e) => {
        const rect = e.currentTarget.getBoundingClientRect()
        const x = e.clientX - rect.left
        const rawIndex = Math.floor((x - margin.left) / Math.max(step, 1))
        const clampedIndex = Math.max(0, Math.min(chartData.length - 1, rawIndex))
        setHoverIndex(clampedIndex)
      }}
    >
      <svg width="100%" height="100%" viewBox={`0 0 ${size.width} ${size.height}`} preserveAspectRatio="none">
        {Array.from({ length: yTicks + 1 }).map((_, i) => {
          const y = margin.top + (plotHeight / yTicks) * i
          return (
            <line
              key={`y-grid-${i}`}
              x1={margin.left}
              y1={y}
              x2={size.width - margin.right}
              y2={y}
              stroke="#1f232d"
              strokeWidth="1"
            />
          )
        })}

        {Array.from({ length: xTicks + 1 }).map((_, i) => {
          const x = margin.left + (plotWidth / Math.max(xTicks, 1)) * i
          return (
            <line
              key={`x-grid-${i}`}
              x1={x}
              y1={margin.top}
              x2={x}
              y2={size.height - margin.bottom}
              stroke="#1f232d"
              strokeWidth="1"
            />
          )
        })}

        {/* Current price reference line */}
        <line
          x1={margin.left}
          y1={yForPrice(currentPrice)}
          x2={size.width - margin.right}
          y2={yForPrice(currentPrice)}
          stroke="#00c087"
          strokeDasharray="2 3"
          strokeWidth="1"
          opacity="0.95"
        />

        {chartData.map((candle, index) => {
          const x = xForIndex(index)
          const yOpen = yForPrice(candle.open)
          const yClose = yForPrice(candle.close)
          const yHigh = yForPrice(candle.high)
          const yLow = yForPrice(candle.low)

          const bodyTop = Math.min(yOpen, yClose)
          const bodyHeight = Math.max(1, Math.abs(yClose - yOpen))

          return (
            <g key={candle.time}>
              <line x1={x} y1={yHigh} x2={x} y2={yLow} stroke={candle.isBullish ? '#00c087' : '#f6465d'} strokeWidth="1" />
              <rect
                x={x - candleWidth / 2}
                y={bodyTop}
                width={candleWidth}
                height={bodyHeight}
                fill={candle.isBullish ? '#00c087' : '#f6465d'}
              />
            </g>
          )
        })}

        {Array.from({ length: yTicks + 1 }).map((_, i) => {
          const price = domainMax - ((domainMax - domainMin) / yTicks) * i
          const y = margin.top + (plotHeight / yTicks) * i + 4
          return (
            <text
              key={`y-label-${i}`}
              x={size.width - margin.right + 8}
              y={y}
              fill="#6b7280"
              fontSize="11"
              fontFamily="Inter, sans-serif"
            >
              {price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </text>
          )
        })}

        {Array.from({ length: 6 }).map((_, i) => {
          const idx = Math.floor((chartData.length - 1) * (i / 5))
          const x = xForIndex(idx)
          return (
            <text
              key={`x-label-${i}`}
              x={x}
              y={size.height - 8}
              fill="#6b7280"
              fontSize="10"
              textAnchor="middle"
              fontFamily="Inter, sans-serif"
            >
              {formatXAxis(chartData[idx].time)}
            </text>
          )
        })}

        {hoveredCandle && hoverIndex !== null && (
          <line
            x1={xForIndex(hoverIndex)}
            y1={margin.top}
            x2={xForIndex(hoverIndex)}
            y2={size.height - margin.bottom}
            stroke="#394150"
            strokeDasharray="4 4"
            strokeWidth="1"
          />
        )}

        {/* Current price indicator bubble on the right */}
        <circle
          cx={size.width - 10}
          cy={yForPrice(currentPrice)}
          r="6"
          fill="#00c087"
        />
        <rect
          x={size.width - 70}
          y={yForPrice(currentPrice) - 11}
          width="65"
          height="22"
          fill="#00c087"
          rx="3"
        />
        <text
          x={size.width - 37}
          y={yForPrice(currentPrice) + 5}
          textAnchor="middle"
          fill="#ffffff"
          fontSize="12"
          fontWeight="600"
          fontFamily="monospace"
        >
          {currentPrice.toLocaleString(undefined, { maximumFractionDigits: 2 })}
        </text>
      </svg>

      {hoveredCandle && hoverIndex !== null && (
        <div
          className="absolute pointer-events-none rounded-md border border-[#2a3040] bg-[#111723] px-3 py-2 text-xs text-[#c7d0dd] shadow-lg"
          style={{ left: tooltipX, top: 18 }}
        >
          <div className="text-[#9ca3af] mb-1">{formatXAxis(hoveredCandle.time)}</div>
          <div>O {hoveredCandle.open.toLocaleString(undefined, { maximumFractionDigits: 2 })}</div>
          <div>H {hoveredCandle.high.toLocaleString(undefined, { maximumFractionDigits: 2 })}</div>
          <div>L {hoveredCandle.low.toLocaleString(undefined, { maximumFractionDigits: 2 })}</div>
          <div>C {hoveredCandle.close.toLocaleString(undefined, { maximumFractionDigits: 2 })}</div>
        </div>
      )}
    </div>
  )
}

export default CandlestickChart

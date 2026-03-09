const OrderBook = ({ orderBook }) => {
  if (!orderBook) {
    return <div className="text-gray-400 p-4">Loading order book...</div>
  }

  const bids = orderBook.bids || []
  const asks = orderBook.asks || []

  return (
    <div className="grid grid-cols-2 gap-4">
      {/* Bids (Buy Orders) */}
      <div className="card">
        <h3 className="text-lg font-bold text-green-400 mb-4">Bids (Buy)</h3>
        <div className="overflow-y-auto max-h-96 scrollbar-hide">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-600">
                <th className="text-left p-2">Price</th>
                <th className="text-right p-2">Amount</th>
              </tr>
            </thead>
            <tbody>
              {bids.map((bid, idx) => (
                <tr key={idx} className="table-row hover:bg-gray-700">
                  <td className="p-2 text-green-400">{parseFloat(bid.price).toFixed(2)}</td>
                  <td className="p-2 text-right">{parseFloat(bid.quantity).toFixed(8)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Asks (Sell Orders) */}
      <div className="card">
        <h3 className="text-lg font-bold text-red-400 mb-4">Asks (Sell)</h3>
        <div className="overflow-y-auto max-h-96 scrollbar-hide">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-600">
                <th className="text-left p-2">Price</th>
                <th className="text-right p-2">Amount</th>
              </tr>
            </thead>
            <tbody>
              {asks.map((ask, idx) => (
                <tr key={idx} className="table-row hover:bg-gray-700">
                  <td className="p-2 text-red-400">{parseFloat(ask.price).toFixed(2)}</td>
                  <td className="p-2 text-right">{parseFloat(ask.quantity).toFixed(8)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default OrderBook

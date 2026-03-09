import { format } from 'date-fns'

const RecentTrades = ({ trades }) => {
  if (!trades || trades.length === 0) {
    return <div className="text-gray-400 p-4">No recent trades</div>
  }

  return (
    <div className="card">
      <h3 className="text-lg font-bold mb-4">Recent Trades</h3>
      <div className="overflow-y-auto max-h-96 scrollbar-hide">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-600">
              <th className="text-left p-2">Time</th>
              <th className="text-left p-2">Price</th>
              <th className="text-left p-2">Quantity</th>
              <th className="text-right p-2">Total</th>
            </tr>
          </thead>
          <tbody>
            {trades.map((trade) => (
              <tr key={trade.id} className="table-row">
                <td className="p-2 text-gray-400">
                  {format(new Date(trade.createdAt), 'HH:mm:ss')}
                </td>
                <td className="p-2">{parseFloat(trade.price).toFixed(2)}</td>
                <td className="p-2">{parseFloat(trade.quantity).toFixed(8)}</td>
                <td className="p-2 text-right">{parseFloat(trade.totalValue).toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default RecentTrades

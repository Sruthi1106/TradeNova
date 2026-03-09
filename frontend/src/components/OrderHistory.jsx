import React from 'react'

// Displays a user's past orders in a table. The parent container handles
// dimensions and borders; this component simply renders the list passed in.
const OrderHistory = ({ orders }) => {
  if (!orders || orders.length === 0) {
    return (
      <div className="text-muted text-center py-16">
        <h3 className="text-lg font-bold mb-4">Order History</h3>
        No orders placed yet
      </div>
    )
  }

  return (
    <div className="overflow-x-auto h-[calc(100%-68px)]">
      <h3 className="text-lg font-bold mb-4">Order History</h3>
      <table className="w-full min-w-[780px] text-sm">
        <thead>
          <tr className="text-muted border-b border-dark-600/40">
            <th className="text-left px-4 py-3">ID</th>
            <th className="text-left px-4 py-3">Pair</th>
            <th className="text-left px-4 py-3">Type</th>
            <th className="text-left px-4 py-3">Side</th>
            <th className="text-right px-4 py-3">Qty</th>
            <th className="text-right px-4 py-3">Price</th>
            <th className="text-right px-4 py-3">Status</th>
            <th className="text-right px-4 py-3">Created</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id} className="border-b border-dark-600/30">
              <td className="px-4 py-2">{order.id}</td>
              <td className="px-4 py-2">{order.tradingPair}</td>
              <td className="px-4 py-2">{order.type}</td>
              <td className="px-4 py-2">{order.side}</td>
              <td className="px-4 py-2 text-right">
                {Number(order.quantity).toLocaleString(undefined, { maximumFractionDigits: 8 })}
              </td>
              <td className="px-4 py-2 text-right">
                {order.price ? Number(order.price).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '-'}
              </td>
              <td className="px-4 py-2 text-right">{order.status}</td>
              <td className="px-4 py-2 text-right">
                {new Date(order.createdAt).toLocaleString()}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default OrderHistory

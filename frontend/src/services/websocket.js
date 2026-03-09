import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws/trading'

class WebSocketService {
  constructor() {
    this.client = null
    this.connected = false
    this.connecting = false
  }

  connect(onConnect, onMessage, onError) {
    if (this.connected && this.client?.active) {
      if (onConnect) onConnect()
      return
    }

    if (this.connecting) {
      return
    }

    this.connecting = true

    try {
      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        reconnectDelay: 2000,
        debug: () => {},
      })

      this.client.onConnect = () => {
        this.connected = true
        this.connecting = false
        if (onConnect) onConnect()
      }

      this.client.onStompError = (frame) => {
        this.connecting = false
        if (onError) onError(frame)
      }

      this.client.onWebSocketError = (event) => {
        this.connecting = false
        if (onError) onError(event)
      }

      this.client.activate()
    } catch (error) {
      this.connecting = false
      this.connected = false
      if (onError) onError(error)
    }
  }

  disconnect() {
    if (this.client && this.client.active) {
      this.client.deactivate()
    }
    this.connected = false
    this.connecting = false
  }

  subscribe(topic, callback) {
    if (!this.client || !this.connected) {
      return { unsubscribe: () => {} }
    }

    return this.client.subscribe(topic, (message) => {
      try {
        callback(JSON.parse(message.body))
      } catch {
        callback(message.body)
      }
    })
  }

  send(destination, message) {
    if (!this.client || !this.connected) {
      return
    }

    this.client.publish({
      destination,
      body: JSON.stringify(message),
    })
  }

  isConnected() {
    return this.connected && !!this.client && this.client.active
  }
}

export default new WebSocketService()

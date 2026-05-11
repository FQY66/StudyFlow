import { API_BASE_URL } from '@/config/api'

export type ChatSocketMessageHandler = (event: MessageEvent<string>) => void

let socket: WebSocket | null = null
let connectingPromise: Promise<WebSocket> | null = null
let closeByUser = false
let currentToken = ''
let currentUserId: number | null = null
const messageHandlers = new Set<ChatSocketMessageHandler>()
const openHandlers = new Set<() => void>()
const closeHandlers = new Set<() => void>()

const buildWsUrl = (token: string, userId: number) => {
  const base = API_BASE_URL.replace(/^http/i, 'ws').replace(/\/$/, '')
  const query = token ? `?token=${encodeURIComponent(token)}` : ''
  return `${base}/ws/chat${query}`
}

const attachEvents = (ws: WebSocket) => {
  ws.onopen = () => {
    openHandlers.forEach((handler) => handler())
  }

  ws.onmessage = (event) => {
    messageHandlers.forEach((handler) => handler(event as MessageEvent<string>))
  }

  ws.onclose = () => {
    socket = null
    connectingPromise = null
    closeHandlers.forEach((handler) => handler())
    if (!closeByUser && currentToken && currentUserId) {
      void connectChatSocket(currentToken, currentUserId)
    }
  }

  ws.onerror = () => {
    try {
      ws.close()
    } catch {
      // ignore
    }
  }
}

export const connectChatSocket = (token: string, userId: number) => {
  if (!token || !userId) {
    return Promise.reject(new Error('token or userId missing'))
  }

  currentToken = token
  currentUserId = userId
  closeByUser = false

  if (socket && socket.readyState === WebSocket.OPEN) {
    return Promise.resolve(socket)
  }

  if (connectingPromise) {
    return connectingPromise
  }

  connectingPromise = new Promise<WebSocket>((resolve, reject) => {
    try {
      const ws = new WebSocket(buildWsUrl(token, userId))
      socket = ws
      attachEvents(ws)

      ws.onopen = () => {
        openHandlers.forEach((handler) => handler())
        connectingPromise = null
        resolve(ws)
      }

      ws.onclose = () => {
        socket = null
        connectingPromise = null
        closeHandlers.forEach((handler) => handler())
        if (closeByUser) {
          resolve(ws)
          return
        }
        reject(new Error('websocket closed before open'))
        if (currentToken && currentUserId) {
          void connectChatSocket(currentToken, currentUserId)
        }
      }
    } catch (error) {
      connectingPromise = null
      reject(error)
    }
  })

  return connectingPromise
}

export const getChatSocket = () => socket

export const sendChatSocketMessage = (payload: string) => {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    return false
  }
  socket.send(payload)
  return true
}

export const closeChatSocket = () => {
  closeByUser = true
  currentToken = ''
  currentUserId = null
  connectingPromise = null
  if (socket) {
    try {
      socket.close()
    } catch {
      // ignore
    }
  }
  socket = null
}

export const onChatSocketMessage = (handler: ChatSocketMessageHandler) => {
  messageHandlers.add(handler)
  return () => messageHandlers.delete(handler)
}

export const onChatSocketOpen = (handler: () => void) => {
  openHandlers.add(handler)
  return () => openHandlers.delete(handler)
}

export const onChatSocketClose = (handler: () => void) => {
  closeHandlers.add(handler)
  return () => closeHandlers.delete(handler)
}

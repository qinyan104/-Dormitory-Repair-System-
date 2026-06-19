import { ref } from 'vue'
import { Client, type Message } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../stores/auth'
import { useToast } from './useToast'
import { useNotification } from './useNotification'

let client: Client | null = null
const connected = ref(false)

export function useWebSocket() {
  const toast = useToast()

  const connect = () => {
    const authStore = useAuthStore()
    const { incrementUnread } = useNotification()
    if (!authStore.token || !authStore.user) return

    if (client?.active) {
      client.deactivate()
    }

    client = new Client({
      // @ts-ignore
      webSocketFactory: () => new SockJS('/ws', null, { transports: ['websocket', 'xhr-streaming', 'xhr-polling'] }),
      connectHeaders: {
        Authorization: 'Bearer ' + authStore.token
      },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        connected.value = true

        // Subscribe to personal notifications (all roles)
        const userId = authStore.user?.id
        if (userId) {
          client!.subscribe('/user/' + userId + '/queue/notification', (msg: Message) => {
            try {
              const body = JSON.parse(msg.body)
              toast.info(body.content || body.title)
              incrementUnread()
            } catch {
              // ignore
            }
          })
        }

        // Admin: subscribe to new order notifications
        if (authStore.user?.role === 'ADMIN') {
          client!.subscribe('/topic/admin/new-order', (msg: Message) => {
            try {
              const body = JSON.parse(msg.body)
              toast.info('新工单: ' + (body.content || body.title))
            } catch {
              // ignore
            }
          })
        }
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      }
    })

    client.activate()
  }

  const disconnect = () => {
    if (client?.active) {
      client.deactivate()
    }
    connected.value = false
  }

  return { connected, connect, disconnect }
}

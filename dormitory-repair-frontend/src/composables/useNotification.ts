import { ref } from 'vue'
import { getUnreadCountApi } from '../api/notification'

const unreadCount = ref(0)

export function useNotification() {
  const fetchUnreadCount = async () => {
    try {
      const data: any = await getUnreadCountApi()
      unreadCount.value = typeof data === 'number' ? data : 0
    } catch {
      // ignore
    }
  }

  const incrementUnread = () => {
    unreadCount.value++
  }

  return { unreadCount, fetchUnreadCount, incrementUnread }
}

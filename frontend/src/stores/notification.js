import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

/**
 * 通知未读数仓库
 * <p>
 * 未读数是跨组件共享状态：顶栏铃铛与通知列表页都要读它，
 * 标记已读后两处必须同步，因此放在 Pinia 而不是页面里。
 * 列表数据只有通知页一个消费方，仍由页面自己持有。
 * </p>
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  // 拉取未读数
  async function fetchUnreadCount() {
    try {
      const res = await request.get('/user/notifications/unread-count')
      if (res.code === 200) {
        unreadCount.value = res.data || 0
      }
    } catch (error) {
      console.error('获取未读数失败', error)
    }
  }

  // 标记单条已读，成功后同步未读数
  async function markAsRead(id) {
    await request.put(`/user/notifications/${id}/read`)
    await fetchUnreadCount()
  }

  // 标记全部已读，成功后同步未读数
  async function markAllAsRead() {
    await request.put('/user/notifications/read-all')
    await fetchUnreadCount()
  }

  return { unreadCount, fetchUnreadCount, markAsRead, markAllAsRead }
})

<template>
  <div class="notification-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>通知中心</span>
          <div class="header-actions">
            <el-switch
              v-model="onlyUnread"
              active-text="只看未读"
              style="margin-right: 12px"
              @change="handleFilterChange"
            />
            <el-button
              type="primary"
              plain
              :disabled="total === 0"
              style="margin-right: 12px"
              @click="handleMarkAll"
            >
              全部已读
            </el-button>
            <el-button type="primary" @click="refreshList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe @row-click="handleRowClick">
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column prop="isRead" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isRead === 1" type="info" size="small">已读</el-tag>
            <el-tag v-else type="danger" size="small" effect="dark">未读</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleRowClick(row)">查看</el-button>
            <el-button
              v-if="row.isRead === 0"
              link
              type="primary"
              @click.stop="markRead(row)"
            >
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/format'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const onlyUnread = ref(false)

// 通知类型：0驳回 1待办 2通过
const typeMap = {
  0: { text: '驳回', type: 'danger' },
  1: { text: '待办', type: 'warning' },
  2: { text: '通过', type: 'success' },
}

const getTypeText = (type) => typeMap[type]?.text || '通知'
const getTypeType = (type) => typeMap[type]?.type || 'info'

const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (onlyUnread.value) {
      params.onlyUnread = true
    }
    const res = await request.get('/user/notifications', { params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('加载通知失败', error)
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  pageNum.value = 1
  loadData()
}

const refreshList = () => {
  pageNum.value = 1
  loadData()
}

// 标记单条已读：已经是已读就不重复发请求
const markRead = async (row) => {
  if (row.isRead === 1) return
  try {
    await notificationStore.markAsRead(row.id)
    await loadData()
  } catch (error) {
    console.error('标记已读失败', error)
  }
}

// 点击行：先标记已读，再跳转到对应详情
const handleRowClick = async (row) => {
  await markRead(row)
  jumpTo(row)
}

const jumpTo = (row) => {
  // 待办类通知跳任务处理页，其余跳申请详情
  if (row.type === 1 && row.taskId) {
    router.push(`/task/${row.taskId}`)
  } else if (row.instanceId) {
    router.push(`/instance/${row.instanceId}`)
  }
}

const handleMarkAll = async () => {
  try {
    await ElMessageBox.confirm('确定把全部通知标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await notificationStore.markAllAsRead()
    ElMessage.success('已全部标记为已读')
    pageNum.value = 1
    await loadData()
  } catch (error) {
    console.error('全部已读失败', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.notification-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

<template>
  <div class="mq-outbox-admin">
    <!-- 概览：四张统计卡 + 最久积压时间，10s 自动轮询 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待发送</div>
          <div class="stat-value">{{ overview.pending ?? '—' }}</div>
          <div class="stat-sub">含退避等待中的 {{ overview.waitingBackoff ?? 0 }} 条</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--warn">
          <div class="stat-label">退避等待</div>
          <div class="stat-value">{{ overview.waitingBackoff ?? '—' }}</div>
          <div class="stat-sub">最近一轮有发送失败正在等下次重试</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--ok">
          <div class="stat-label">已发送</div>
          <div class="stat-value">{{ overview.sent ?? '—' }}</div>
          <div class="stat-sub">已投递至 MQ Broker</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--danger">
          <div class="stat-label">死信</div>
          <div class="stat-value">{{ overview.dead ?? '—' }}</div>
          <div class="stat-sub">
            大于 0 需人工介入
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="backlog-row">
      <el-col :span="24">
        <el-card shadow="never">
          <div class="backlog-line">
            <span class="backlog-label">最久积压待发送登记时间：</span>
            <span class="backlog-value">
              {{ overview.oldestPendingTime ? formatDateTime(overview.oldestPendingTime) : '—' }}
            </span>
            <span v-if="backlogAgeMinutes != null" class="backlog-age">
              距今约 {{ backlogAgeMinutes }} 分钟
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 消息记录列表：按需手动刷新 -->
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>消息记录</span>
          <div class="header-actions">
            <el-select
              v-model="filterStatus"
              placeholder="全部状态"
              clearable
              style="width: 140px"
              @change="handleSearch"
            >
              <el-option label="待发送" :value="0" />
              <el-option label="已发送" :value="1" />
              <el-option label="死信" :value="2" />
            </el-select>

            <el-input
              v-model="filterMsgKey"
              placeholder="消息业务键（taskId）精确匹配"
              clearable
              style="width: 240px; margin-left: 12px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>

            <el-button style="margin-left: 12px" @click="refreshList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="topic" label="主题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="msgKey" label="业务键(taskId)" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" disable-transitions>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试" width="80" align="center" />
        <el-table-column prop="nextRetryTime" label="下次重试" width="160">
          <template #default="{ row }">
            {{ row.status === 2 ? '—' : formatDateTime(row.nextRetryTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="最近失败原因" min-width="200">
          <template #default="{ row }">
            <span v-if="row.errorMsg" class="error-msg" :title="row.errorMsg">
              {{ truncate(row.errorMsg, 80) }}
            </span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="登记时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showBody(row)">查看报文</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadList"
        @current-change="loadList"
        class="pagination"
      />
    </el-card>

    <!-- 报文查看对话框 -->
    <el-dialog
      v-model="bodyDialogVisible"
      :title="`消息报文 #${bodyDialogRecord?.id ?? ''}`"
      width="720px"
    >
      <div v-if="bodyDialogRecord" class="body-meta">
        <div><span class="meta-key">主题：</span>{{ bodyDialogRecord.topic }}</div>
        <div><span class="meta-key">业务键：</span>{{ bodyDialogRecord.msgKey }}</div>
        <div><span class="meta-key">状态：</span>{{ statusText(bodyDialogRecord.status) }}</div>
        <div v-if="bodyDialogRecord.errorMsg">
          <span class="meta-key">失败原因：</span>{{ bodyDialogRecord.errorMsg }}
        </div>
      </div>
      <pre class="body-pre">{{ prettyJson(bodyDialogRecord?.body) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/format'

// 概览：单独状态，与列表刷新策略解耦
const overview = ref({
  pending: null,
  waitingBackoff: null,
  sent: null,
  dead: null,
  oldestPendingTime: null,
})

// 概览轮询：30s 自动，标签页隐藏时停止，避免资源浪费
const OVERVIEW_POLL_MS = 30000
let overviewTimer = null

const loadOverview = async () => {
  try {
    const res = await request.get('/admin/mq-outbox/overview')
    overview.value = res.data || {}
  } catch {
    // 轮询失败静默，不污染 UI
  }
}

// 列表：按需手动
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref(null)
const filterMsgKey = ref('')

const loadList = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    if (filterMsgKey.value) {
      params.msgKey = filterMsgKey.value
    }
    const res = await request.get('/admin/mq-outbox/records', { params })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadList()
}

const refreshList = () => {
  filterStatus.value = null
  filterMsgKey.value = ''
  pageNum.value = 1
  loadList()
}

// 报文对话框
const bodyDialogVisible = ref(false)
const bodyDialogRecord = ref(null)

const showBody = (row) => {
  bodyDialogRecord.value = row
  bodyDialogVisible.value = true
}

// 状态展示
const statusText = (s) => {
  const map = { 0: '待发送', 1: '已发送', 2: '死信' }
  return map[s] ?? '—'
}

const statusTagType = (s) => {
  // 0 待发送（info）、1 已发送（success）、2 死信（danger）
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'info'
}

// 报文 JSON 美化（解析失败就原文展示）
const prettyJson = (raw) => {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

const truncate = (str, max) => {
  if (!str) return ''
  return str.length > max ? str.substring(0, max) + '…' : str
}

// 最久积压时长：基于客户端当前时间，仅作体感提示，不强求精度
const backlogAgeMinutes = computed(() => {
  const t = overview.value.oldestPendingTime
  if (!t) return null
  const ts = new Date(t).getTime()
  if (Number.isNaN(ts)) return null
  return Math.max(0, Math.floor((Date.now() - ts) / 60000))
})

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    // 切回可见立即拉，弥补停期间的盲区
    loadOverview()
    if (!overviewTimer) startOverviewPolling()
  } else {
    stopOverviewPolling()
  }
}

const startOverviewPolling = () => {
  if (overviewTimer) return
  overviewTimer = setInterval(loadOverview, OVERVIEW_POLL_MS)
}

const stopOverviewPolling = () => {
  if (overviewTimer) {
    clearInterval(overviewTimer)
    overviewTimer = null
  }
}

onMounted(() => {
  loadOverview()
  loadList()
  startOverviewPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  stopOverviewPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.mq-outbox-admin {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.overview-row {
  margin: 0 !important;
}
.stat-card {
  text-align: center;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  color: #303133;
}
.stat-sub {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}
.stat-card--warn .stat-value {
  color: #e6a23c;
}
.stat-card--ok .stat-value {
  color: #67c23a;
}
.stat-card--danger .stat-value {
  color: #f56c6c;
}
.backlog-row {
  margin: 0 !important;
}
.backlog-line {
  display: flex;
  align-items: center;
  gap: 12px;
}
.backlog-label {
  color: #909399;
}
.backlog-value {
  color: #303133;
  font-weight: 500;
}
.backlog-age {
  color: #e6a23c;
  font-size: 12px;
}
.list-card {
  margin: 0;
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
.error-msg {
  color: #f56c6c;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}
.muted {
  color: #c0c4cc;
}
.body-meta {
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-key {
  color: #909399;
  margin-right: 4px;
}
.body-pre {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  margin: 0;
  max-height: 420px;
  overflow: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
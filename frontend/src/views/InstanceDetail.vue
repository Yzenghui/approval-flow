<template>
  <div class="instance-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>申请详情</span>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <!-- 基本信息 -->
      <el-descriptions title="基本信息" :column="2" border>
        <el-descriptions-item label="申请ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="流程名称">{{ detail.definitionName }}</el-descriptions-item>
        <el-descriptions-item label="提交次数">{{ detail.submitCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType">
            {{ statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          formatDateTime(detail.createTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{
          formatDateTime(detail.updateTime)
        }}</el-descriptions-item>
      </el-descriptions>

      <!-- 申请内容：键值对列表（只读） -->
      <el-descriptions title="申请内容" :column="2" border class="form-data-section">
        <el-descriptions-item v-for="(value, key) in parsedFormData" :key="key" :label="key">
          {{ value }}
        </el-descriptions-item>
        <el-descriptions-item v-if="Object.keys(parsedFormData).length === 0" label="暂无数据">
          —
        </el-descriptions-item>
      </el-descriptions>

      <!-- 审批轨迹 -->
      <div class="approval-section">
        <div class="section-title">审批轨迹</div>
        <el-table :data="records" stripe size="small">
          <el-table-column prop="submitRound" label="轮次" width="70" align="center">
            <template #default="{ row }">
              {{ row.submitRound || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column prop="action" label="操作" width="100">
            <template #default="{ row }">
              {{ getActionText(row.action) }}
            </template>
          </el-table-column>
          <el-table-column prop="nodeName" label="节点" width="150">
            <template #default="{ row }">
              {{ row.nodeName || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="意见" min-width="150">
            <template #default="{ row }">
              {{ row.remark || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>
        <div v-if="records.length === 0" class="empty-tip">暂无审批记录</div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <!-- 草稿状态：显示编辑 + 提交 -->
        <template v-if="detail.status === 0">
          <el-button type="primary" @click="handleEditDraft">编辑</el-button>
          <el-button type="success" @click="handleSubmitDraft">提交</el-button>
        </template>

        <!-- 驳回/撤回：显示修改重提 -->
        <el-button
          v-if="detail.status === 3 || detail.status === 4"
          type="primary"
          @click="handleModifyResubmit"
        >
          修改重提
        </el-button>

        <!-- 审批中：显示撤回 -->
        <el-button v-if="detail.status === 1" type="danger" @click="handleWithdraw">
          撤回
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const instanceId = route.params.id

const loading = ref(false)
const detail = ref({})
const records = ref([])

const statusMap = {
  0: { text: '草稿', type: 'info' },
  1: { text: '审批中', type: 'warning' },
  2: { text: '通过', type: 'success' },
  3: { text: '驳回', type: 'danger' },
  4: { text: '撤回', type: 'info' },
}
const statusText = computed(() => statusMap[detail.value.status]?.text || '未知')
const statusType = computed(() => statusMap[detail.value.status]?.type || 'info')

const actionMap = {
  0: '提交',
  1: '通过',
  2: '驳回',
  3: '撤回',
  4: '修改重提',
}
const getActionText = (action) => actionMap[action] || '未知'

const formatDateTime = (isoString) => {
  if (!isoString) return '—'
  return isoString.replace('T', ' ').substring(0, 19)
}

const parsedFormData = computed(() => {
  if (!detail.value.formData) return {}
  try {
    const data =
      typeof detail.value.formData === 'string'
        ? JSON.parse(detail.value.formData)
        : detail.value.formData
    return data
  } catch {
    return {}
  }
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/user/instances/${instanceId}`)
    if (res.code === 200) {
      detail.value = res.data
      records.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载详情失败', error)
  } finally {
    loading.value = false
  }
}

// 编辑草稿
const handleEditDraft = () => {
  router.push(`/draft-edit/${instanceId}`)
}

// 提交草稿
const handleSubmitDraft = async () => {
  try {
    await ElMessageBox.confirm('确定提交此申请吗？提交后将进入审批流程。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const res = await request.post(`/user/instances/${instanceId}/submit`)
    if (res.code === 200) {
      ElMessage.success('提交成功')
      loadDetail()
      setTimeout(() => {
        router.push('/instances')
      }, 1500)
    }
  } catch (error) {
    if (error !== 'cancel') console.error('提交失败', error)
  }
}

// 修改重提
const handleModifyResubmit = () => {
  router.push(`/resubmit-edit/${instanceId}`)
}

// 撤回申请
const handleWithdraw = async () => {
  try {
    await ElMessageBox.confirm('确定撤回此申请吗？撤回后流程终止，可修改后重提。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const res = await request.post(`/user/instances/${instanceId}/withdraw`)
    if (res.code === 200) {
      ElMessage.success('撤回成功')
      loadDetail()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回失败', error)
    }
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.instance-detail {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-data-section {
  margin-top: 20px;
}
.approval-section {
  margin-top: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px;
}
.action-bar {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
  justify-content: flex-start;
}
</style>

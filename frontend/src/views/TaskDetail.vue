<template>
  <div class="task-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>审批</span>
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

      <!-- 申请内容：键值对列表 -->
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

      <!-- 审批意见 -->
      <el-descriptions title="审批意见" :column="1" border>
        <el-descriptions-item label="意见">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请输入审批意见（可选）"
          />
        </el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="success" @click="handleApprove" :loading="submitting"> 通过 </el-button>
        <el-button type="danger" @click="handleReject" :loading="submitting"> 驳回 </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const taskId = route.params.taskId
const instanceId = route.query.instanceId

const loading = ref(false)
const submitting = ref(false)
const detail = ref({})
const records = ref([])
const remark = ref('')

const statusMap = {
  1: { text: '审批中', type: 'warning' },
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
  if (!instanceId) {
    ElMessage.error('缺少申请ID')
    goBack()
    return
  }

  loading.value = true
  try {
    const res = await request.get(`/user/instances/${instanceId}`)
    detail.value = res.data
    records.value = res.data.records || []
  } catch (error) {
    console.error('加载详情失败', error)
    ElMessage.error('加载申请详情失败')
    goBack()
  } finally {
    loading.value = false
  }
}

const handleApprove = async () => {
  try {
    await ElMessageBox.confirm('确定通过此申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    submitting.value = true
    await request.post(`/user/tasks/${taskId}/approve`, {
      remark: remark.value,
    })
    ElMessage.success('审批通过')
    router.push('/pending')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审批失败', error)
    }
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  try {
    await ElMessageBox.confirm('确定驳回此申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    submitting.value = true
    await request.post(`/user/tasks/${taskId}/reject`, {
      remark: remark.value,
    })
    ElMessage.success('已驳回')
    router.push('/pending')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('驳回失败', error)
    }
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/pending')
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.task-detail {
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

<template>
  <div class="my-instances">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的申请</span>
          <div class="header-actions">
            <el-select
              v-model="filterStatus"
              placeholder="全部状态"
              clearable
              style="width: 120px; margin-right: 12px"
              @change="handleStatusChange"
            >
              <el-option label="草稿" :value="0" />
              <el-option label="审批中" :value="1" />
              <el-option label="通过" :value="2" />
              <el-option label="驳回" :value="3" />
              <el-option label="撤回" :value="4" />
            </el-select>
            <el-button type="primary" @click="refreshList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="申请ID" width="80" />
        <el-table-column prop="definitionName" label="流程名称" min-width="150" />
        <el-table-column prop="formData" label="申请内容" min-width="200">
          <template #default="{ row }">
            <div class="form-data-preview">{{ formatFormData(row.formData) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="submitCount" label="提交次数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="submitDraft(row.id)">
              提交
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

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref(null) // 状态筛选，null 表示全部

// 状态映射
const statusMap = {
  0: { text: '草稿', type: 'info' },
  1: { text: '审批中', type: 'warning' },
  2: { text: '通过', type: 'success' },
  3: { text: '驳回', type: 'danger' },
  4: { text: '撤回', type: 'info' },
}

const getStatusText = (status) => statusMap[status]?.text || '未知'
const getStatusType = (status) => statusMap[status]?.type || 'info'

// 格式化 formData 预览
const formatFormData = (formData) => {
  if (!formData) return '—'
  try {
    const data = typeof formData === 'string' ? JSON.parse(formData) : formData
    return Object.entries(data)
      .map(([k, v]) => `${k}: ${v}`)
      .join(' | ')
  } catch {
    return '—'
  }
}

// 格式化日期时间
const formatDateTime = (isoString) => {
  if (!isoString) return '—'
  return isoString.replace('T', ' ').substring(0, 19)
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    const res = await request.get('/user/instances', { params })
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载失败', error)
  } finally {
    loading.value = false
  }
}

// 状态筛选变化时重置页码并重新加载
const handleStatusChange = () => {
  pageNum.value = 1
  loadData()
}

// 刷新
const refreshList = () => {
  pageNum.value = 1
  loadData()
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/instance/${id}`)
}

// 提交草稿
const submitDraft = async (id) => {
  try {
    await ElMessageBox.confirm('确定提交此申请吗？', '提示')
    const res = await request.post(`/user/instances/${id}/submit`)
    if (res.code === 200) {
      ElMessage.success('提交成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败', error)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-instances {
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

.form-data-preview {
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

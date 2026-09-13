<template>
  <div class="pending-tasks">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待办任务</span>
          <el-button type="primary" @click="refreshList">刷新</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskId" label="任务ID" width="80" />
        <el-table-column prop="instanceId" label="申请ID" width="80" />
        <el-table-column prop="definitionName" label="流程名称" min-width="150" />
        <el-table-column prop="nodeName" label="当前节点" width="120" />
        <el-table-column prop="initiatorName" label="申请人" width="100" />
        <el-table-column label="申请内容" min-width="200">
          <template #default="{ row }">
            <div class="form-data-preview">
              {{ formatFormData(row.formData) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleApprove(row.taskId, row.instanceId)">
              审批
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
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
import request from '@/utils/request'
import { formatDateTime, formatFormData } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/tasks/pending', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('加载待办列表失败', error)
  } finally {
    loading.value = false
  }
}

const refreshList = () => {
  pageNum.value = 1
  loadData()
}

const handleApprove = (taskId, instanceId) => {
  router.push(`/task/${taskId}?instanceId=${instanceId}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.pending-tasks {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
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

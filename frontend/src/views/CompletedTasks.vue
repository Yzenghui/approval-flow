<template>
  <div class="completed-tasks">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>已办任务</span>
          <el-button type="primary" @click="refreshList">刷新</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskId" label="任务ID" width="80" />
        <el-table-column prop="instanceId" label="申请ID" width="80" />
        <el-table-column prop="definitionName" label="流程名称" min-width="150" />
        <el-table-column prop="nodeName" label="节点名称" width="120" />
        <el-table-column prop="initiatorName" label="申请人" width="100" />
        <el-table-column label="我的操作" width="100">
          <template #default="{ row }">
            <el-tag :type="row.action === 1 ? 'success' : 'danger'" size="small">
              {{ getActionText(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="我的意见" min-width="150">
          <template #default="{ row }">
            {{ row.remark || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="任务创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="completeTime" label="完成时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.completeTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.instanceId)"> 详情 </el-button>
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
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const actionMap = {
  1: '通过',
  2: '驳回',
}
const getActionText = (action) => actionMap[action] || '未知'

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/tasks/completed', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('加载已办列表失败', error)
  } finally {
    loading.value = false
  }
}

const refreshList = () => {
  pageNum.value = 1
  loadData()
}

const viewDetail = (instanceId) => {
  router.push(`/instance/${instanceId}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.completed-tasks {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

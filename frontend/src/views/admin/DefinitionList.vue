<template>
  <div class="definition-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>流程定义</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="按流程名称搜索"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>

            <el-select
              v-model="filterStatus"
              placeholder="全部状态"
              clearable
              style="width: 120px; margin-left: 12px"
              @change="handleSearch"
            >
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>

            <el-button type="primary" style="margin-left: 12px" @click="handleCreate">
              新增流程
            </el-button>
            <el-button @click="refreshList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="流程名称" min-width="150" />
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="roleCode" label="适用角色" width="120" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
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
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row.id)">详情</el-button>
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
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const filterStatus = ref(null)

const formatDateTime = (isoString) => {
  if (!isoString) return '—'
  return isoString.replace('T', ' ').substring(0, 19)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    const res = await request.get('/admin/definitions', { params })
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载流程列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const refreshList = () => {
  keyword.value = ''
  filterStatus.value = null
  pageNum.value = 1
  loadData()
}

const handleStatusChange = async (row) => {
  try {
    const action = row.status === 1 ? 'enable' : 'disable'
    const res = await request.put(`/admin/definitions/${row.id}/${action}`)
    if (res.code === 200) {
      ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
    } else {
      row.status = row.status === 1 ? 0 : 1
    }
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    console.error('状态修改失败', error)
  }
}

const handleCreate = () => {
  router.push('/admin/definition/create')
}

const handleDetail = (id) => {
  router.push(`/admin/definition/${id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.definition-list {
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

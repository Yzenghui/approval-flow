<template>
  <div class="apply-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>发起申请</span>
          <!-- 只在表单填写时显示返回按钮 -->
          <el-button v-if="selectedDefinition" @click="cancelSelect">返回</el-button>
        </div>
      </template>

      <!-- 第一步：选择流程 -->
      <div v-if="!selectedDefinition" class="step-select">
        <h3>请选择申请类型</h3>
        <el-table
          :data="definitions"
          v-loading="loadingDefs"
          stripe
          @row-click="selectDefinition"
          style="cursor: pointer"
        >
          <el-table-column prop="name" label="流程名称" />
          <el-table-column prop="roleCode" label="适用角色" width="120" />
        </el-table>
        <el-pagination
          v-model:current-page="defPageNum"
          v-model:page-size="defPageSize"
          :total="defTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadDefinitions"
          @current-change="loadDefinitions"
          class="pagination"
        />
      </div>

      <!-- 第二步：填写表单 -->
      <div v-else>
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
          v-loading="loadingForm"
        >
          <el-form-item
            v-for="field in formSchema"
            :key="field.field"
            :label="field.label"
            :prop="field.field"
            :required="field.required"
          >
            <el-input-number
              v-if="field.type === 'number'"
              v-model="formData[field.field]"
              :placeholder="`请输入${field.label}`"
              controls-position="right"
              style="width: 100%"
            />
            <el-input
              v-else-if="field.type === 'textarea'"
              v-model="formData[field.field]"
              type="textarea"
              :rows="4"
              :placeholder="`请输入${field.label}`"
            />
            <el-input
              v-else
              v-model="formData[field.field]"
              :placeholder="`请输入${field.label}`"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting"> 提交 </el-button>
            <el-button @click="handleSaveDraft" :loading="savingDraft"> 保存草稿 </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const definitions = ref([])
const loadingDefs = ref(false)
const defPageNum = ref(1)
const defPageSize = ref(10)
const defTotal = ref(0)

const selectedDefinition = ref(null)
const formSchema = ref([])
const loadingForm = ref(false)
const formData = ref({})
const submitting = ref(false)
const savingDraft = ref(false)
const formRef = ref()

// 动态生成校验规则
const formRules = computed(() => {
  const rules = {}
  for (const field of formSchema.value) {
    if (field.required) {
      rules[field.field] = [{ required: true, message: `请输入${field.label}`, trigger: 'blur' }]
    }
  }
  return rules
})

const loadDefinitions = async () => {
  loadingDefs.value = true
  try {
    const res = await request.get('/user/available-definitions', {
      params: { pageNum: defPageNum.value, pageSize: defPageSize.value },
    })
    if (res.code === 200) {
      definitions.value = res.data.records || []
      defTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载流程列表失败', error)
  } finally {
    loadingDefs.value = false
  }
}

const selectDefinition = (row) => {
  selectedDefinition.value = row

  // 解析 formSchema
  let schema = row.formSchema
  if (typeof schema === 'string') {
    try {
      schema = JSON.parse(schema)
    } catch (e) {
      console.error('解析 formSchema 失败', e)
      schema = []
    }
  }
  formSchema.value = schema || []

  // 初始化表单数据
  const initData = {}
  for (const field of formSchema.value) {
    if (field.type === 'number') {
      initData[field.field] = undefined
    } else {
      initData[field.field] = ''
    }
  }
  formData.value = initData

  ElMessage.success(`已选择：${row.name}`)
}

const cancelSelect = () => {
  selectedDefinition.value = null
  formSchema.value = []
  formData.value = {}
}

const prepareFormData = () => {
  const result = { ...formData.value }
  for (const field of formSchema.value) {
    if (field.type === 'number' && typeof result[field.field] === 'string') {
      result[field.field] = parseFloat(result[field.field])
    }
  }
  return result
}

const handleSubmit = async () => {
  if (!selectedDefinition.value) return

  // 表单校验
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await request.post('/user/instances', {
      definitionId: selectedDefinition.value.id,
      formData: JSON.stringify(prepareFormData()),
    })
    if (res.code === 200) {
      ElMessage.success('提交成功')
      router.push('/instances')
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitting.value = false
  }
}

const handleSaveDraft = async () => {
  if (!selectedDefinition.value) return

  // 保存草稿不校验，允许保存空内容
  savingDraft.value = true
  try {
    const res = await request.post('/user/drafts', {
      definitionId: selectedDefinition.value.id,
      formData: JSON.stringify(prepareFormData()),
    })
    if (res.code === 200) {
      ElMessage.success('草稿保存成功')
      router.push('/instances')
    }
  } catch (error) {
    console.error('保存草稿失败', error)
  } finally {
    savingDraft.value = false
  }
}

onMounted(() => {
  loadDefinitions()
})
</script>

<style scoped>
.apply-form {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.step-select h3 {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

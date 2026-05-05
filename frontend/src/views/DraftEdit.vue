<template>
  <div class="draft-edit">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>编辑草稿</span>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </template>

      <!-- 流程已禁用提示 -->
      <el-alert
        v-if="flowDisabled"
        title="流程已被禁用"
        description="该申请关联的流程已被管理员禁用，无法编辑或提交。请返回列表页，重新发起申请。"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <!-- 流程名称变更提示 -->
      <el-alert
        v-if="definitionName !== currentDefinitionName && !flowDisabled"
        title="流程名称已变更"
        :description="`该草稿原属于「${definitionName}」，当前流程已更名为「${currentDefinitionName}」，提交后将以新流程名称记录。`"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        :disabled="flowDisabled"
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
          <el-input v-else v-model="formData[field.field]" :placeholder="`请输入${field.label}`" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving" :disabled="flowDisabled">
            保存草稿
          </el-button>
          <el-button
            type="success"
            @click="handleSubmit"
            :loading="submitting"
            :disabled="flowDisabled"
          >
            提交
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
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
const saving = ref(false)
const submitting = ref(false)
const formRef = ref()
const formData = ref({})
const formSchema = ref([])
const definitionId = ref(null)
const definitionName = ref('')
const currentDefinitionName = ref('')
const flowDisabled = ref(false)

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

const loadData = async () => {
  loading.value = true
  try {
    // 1. 加载草稿数据
    const instanceRes = await request.get(`/user/instances/${instanceId}`)
    if (instanceRes.code === 200) {
      const data = instanceRes.data
      definitionId.value = data.definitionId
      definitionName.value = data.definitionName || ''

      if (data.formData) {
        const parsed = typeof data.formData === 'string' ? JSON.parse(data.formData) : data.formData
        formData.value = parsed
      }

      // 2. 从可用流程列表中获取 formSchema
      const defRes = await request.get('/user/available-definitions', {
        params: { pageNum: 1, pageSize: 100 },
      })
      if (defRes.code === 200) {
        const definitions = defRes.data.records || []
        const matched = definitions.find((d) => d.id === definitionId.value)

        if (!matched) {
          flowDisabled.value = true
          formSchema.value = []
          ElMessage.warning('该申请关联的流程已被禁用，无法编辑或提交')
          return
        }

        currentDefinitionName.value = matched.name || ''

        // 解析 formSchema
        let schema = matched.formSchema
        if (typeof schema === 'string') {
          try {
            schema = JSON.parse(schema)
          } catch (e) {
            console.error('解析 formSchema 失败', e)
            schema = []
          }
        }
        formSchema.value = schema || []
      }
    }
  } catch (error) {
    console.error('加载失败', error)
    ElMessage.error('加载数据失败')
    goBack()
  } finally {
    loading.value = false
  }
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

const handleSave = async () => {
  if (flowDisabled.value) return

  saving.value = true
  try {
    const res = await request.put(`/user/drafts/${instanceId}`, {
      formData: JSON.stringify(prepareFormData()),
    })
    if (res.code === 200) {
      ElMessage.success('草稿保存成功')
      router.push('/instances')
    }
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  if (flowDisabled.value) return

  // 表单校验
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('确定提交此申请吗？提交后将进入审批流程。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    submitting.value = true
    const res = await request.post(`/user/instances/${instanceId}/submit`)
    if (res.code === 200) {
      ElMessage.success('提交成功')
      router.push('/instances')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败', error)
    }
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/instances')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.draft-edit {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

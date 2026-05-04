<template>
  <div class="draft-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>编辑草稿</span>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" label-width="100px" v-loading="loading">
        <el-form-item v-for="(value, key) in formData" :key="key" :label="key" :prop="key">
          <el-input
            v-if="typeof value === 'string' || typeof value === 'number'"
            v-model="formData[key]"
            :placeholder="`请输入${key}`"
          />
          <el-input v-else v-model="formData[key]" type="textarea" :placeholder="`请输入${key}`" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="submitting">保存草稿</el-button>
          <el-button @click="handleSubmit" :loading="submitting">提交</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const instanceId = route.params.id

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const formData = ref({})

const loadInstanceData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/user/instances/${instanceId}`)
    if (res.code === 200) {
      const data = res.data
      if (data.formData) {
        let parsed = typeof data.formData === 'string' ? JSON.parse(data.formData) : data.formData
        for (const key in parsed) {
          if (typeof parsed[key] === 'string' && /^\d+(\.\d+)?$/.test(parsed[key])) {
            parsed[key] = parseFloat(parsed[key])
          }
        }
        formData.value = parsed
      }
    }
  } catch (error) {
    console.error('加载失败', error)
    ElMessage.error('加载草稿数据失败')
    goBack()
  } finally {
    loading.value = false
  }
}

const prepareFormData = (data) => {
  const result = { ...data }
  for (const key in result) {
    if (typeof result[key] === 'string' && /^\d+(\.\d+)?$/.test(result[key])) {
      result[key] = parseFloat(result[key])
    }
  }
  return result
}

// 保存草稿
const handleSave = async () => {
  try {
    submitting.value = true
    const res = await request.put(`/user/drafts/${instanceId}`, {
      formData: JSON.stringify(prepareFormData(formData.value)),
    })
    if (res.code === 200) {
      ElMessage.success('草稿保存成功')
      router.push('/instances')
    }
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    submitting.value = false
  }
}

// 提交草稿
const handleSubmit = async () => {
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
  router.back()
}

onMounted(() => {
  loadInstanceData()
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

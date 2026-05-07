<template>
  <div class="definition-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ pageTitle }}</span>
          <div>
            <el-button v-if="!isEditMode && !isCreateMode" type="primary" @click="enterEditMode">
              编辑
            </el-button>
            <el-button v-if="isEditMode" @click="cancelEdit">取消</el-button>
            <el-button
              v-if="isEditMode || isCreateMode"
              type="primary"
              @click="handleSave"
              :loading="saving"
            >
              保存
            </el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>

      <!-- 基础信息 -->
      <el-descriptions title="基础信息" :column="2" border>
        <el-descriptions-item label="流程名称">
          <el-input
            v-if="isEditMode || isCreateMode"
            v-model="form.name"
            placeholder="请输入流程名称"
            clearable
          />
          <span v-else>{{ form.name || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="适用角色">
          <el-select
            v-if="isEditMode || isCreateMode"
            v-model="form.roleCode"
            placeholder="请选择适用角色"
            style="width: 100%"
          >
            <el-option label="员工" value="STAFF" />
            <el-option label="主管" value="MANAGER" />
            <el-option label="经理" value="DIRECTOR" />
            <el-option label="总监" value="PRESIDENT" />
          </el-select>
          <span v-else>{{ form.roleCode || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="版本">
          <span>{{ form.version || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="form.status === 1 ? 'success' : 'danger'">
            {{ form.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 表单结构配置 -->
      <div class="section">
        <div class="section-header">
          <span class="section-title">表单结构</span>
          <el-button
            v-if="isEditMode || isCreateMode"
            type="primary"
            size="small"
            @click="addFormField"
          >
            + 添加字段
          </el-button>
        </div>

        <!-- 只读模式：表格展示 -->
        <el-table v-if="!isEditMode && !isCreateMode" :data="form.formSchema" border size="small">
          <el-table-column prop="field" label="字段名" width="150" />
          <el-table-column prop="label" label="标签" width="150" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              {{ getTypeText(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="required" label="必填" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'success' : 'info'" size="small">
                {{ row.required ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 编辑模式：可编辑表格 -->
        <el-table v-else :data="form.formSchema" border size="small">
          <el-table-column label="字段名" width="150">
            <template #default="{ row }">
              <el-input
                v-model="row.field"
                placeholder="字段名（字母开头）"
                size="small"
                @blur="validateFieldName(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="标签" width="150">
            <template #default="{ row }">
              <el-input v-model="row.label" placeholder="显示名称" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.type" placeholder="类型" size="small">
                <el-option label="单行文本" value="text" />
                <el-option label="多行文本" value="textarea" />
                <el-option label="数字" value="number" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="80" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.required" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeFormField($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="form.formSchema.length === 0 && (isEditMode || isCreateMode)" class="empty-tip">
          暂无表单字段，点击「添加字段」开始配置
        </div>
        <div v-else-if="form.formSchema.length === 0" class="empty-tip">
          该流程没有配置任何表单字段
        </div>
      </div>

      <!-- 节点配置 -->
      <div class="section">
        <div class="section-header">
          <span class="section-title">审批节点</span>
          <el-button v-if="isEditMode || isCreateMode" type="primary" size="small" @click="addNode">
            + 添加节点
          </el-button>
        </div>

        <!-- 只读模式：表格展示 -->
        <el-table v-if="!isEditMode && !isCreateMode" :data="form.nodes" border size="small">
          <el-table-column prop="nodeOrder" label="顺序" width="80" align="center" />
          <el-table-column prop="nodeName" label="节点名称" width="150" />
          <el-table-column prop="assigneeType" label="分配策略" width="120">
            <template #default="{ row }">
              {{ getAssigneeTypeText(row.assigneeType) }}
            </template>
          </el-table-column>
          <el-table-column prop="assigneeExpr" label="分配目标" min-width="150" />
          <el-table-column prop="conditionExpr" label="条件表达式" min-width="150">
            <template #default="{ row }">
              {{ row.conditionExpr || '—' }}
            </template>
          </el-table-column>
        </el-table>

        <!-- 编辑模式：可编辑表格 -->
        <el-table v-else :data="form.nodes" border size="small">
          <el-table-column label="顺序" width="100" align="center">
            <template #default="{ $index }">
              <el-input-number
                v-model="form.nodes[$index].nodeOrder"
                :min="1"
                :max="99"
                controls-position="right"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="节点名称" width="150">
            <template #default="{ $index }">
              <el-input
                v-model="form.nodes[$index].nodeName"
                placeholder="如：主管审批"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="分配策略" width="130">
            <template #default="{ $index }">
              <el-select v-model="form.nodes[$index].assigneeType" size="small">
                <el-option label="相对关系" value="RELATIVE" />
                <el-option label="指定用户" value="USER" />
                <el-option label="指定角色" value="ROLE" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分配目标" min-width="160">
            <template #default="{ $index }">
              <!-- 相对关系：下拉选择 -->
              <el-select
                v-if="form.nodes[$index].assigneeType === 'RELATIVE'"
                v-model="form.nodes[$index].assigneeExpr"
                size="small"
                style="width: 100%"
              >
                <el-option label="直属上级" value="manager_id" />
                <el-option label="隔级上级" value="manager_id.manager_id" />
                <el-option label="隔两级上级" value="manager_id.manager_id.manager_id" />
              </el-select>
              <!-- 指定角色：下拉选择 -->
              <el-select
                v-else-if="form.nodes[$index].assigneeType === 'ROLE'"
                v-model="form.nodes[$index].assigneeExpr"
                size="small"
                style="width: 100%"
              >
                <el-option label="员工" value="STAFF" />
                <el-option label="主管" value="MANAGER" />
                <el-option label="经理" value="DIRECTOR" />
                <el-option label="总监" value="PRESIDENT" />
                <el-option label="管理员" value="ADMIN" />
              </el-select>
              <!-- 指定用户：输入框 -->
              <el-input
                v-else
                v-model="form.nodes[$index].assigneeExpr"
                placeholder="请输入用户ID"
                size="small"
                clearable
              />
            </template>
          </el-table-column>
          <el-table-column label="条件表达式" min-width="160">
            <template #default="{ $index }">
              <el-input
                v-model="form.nodes[$index].conditionExpr"
                placeholder="如: days > 3 && reason != null (可选)"
                size="small"
                clearable
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeNode($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="form.nodes.length === 0 && (isEditMode || isCreateMode)" class="empty-tip">
          暂无审批节点，点击「添加节点」开始配置（至少需要一个节点）
        </div>
        <div v-else-if="form.nodes.length === 0" class="empty-tip">该流程没有配置审批节点</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

const isCreateMode = route.path.includes('/create')
const definitionId = route.params.id
const isEditMode = ref(isCreateMode || route.query.edit === 'true')

const loading = ref(false)
const saving = ref(false)

const pageTitle = computed(() => {
  if (isCreateMode) return '新增流程'
  if (isEditMode.value) return '编辑流程'
  return '流程详情'
})

const form = ref({
  name: '',
  roleCode: '',
  status: 1,
  version: 1,
  formSchema: [],
  nodes: [],
})

const getTypeText = (type) => {
  const map = { text: '单行文本', textarea: '多行文本', number: '数字' }
  return map[type] || type
}

const getAssigneeTypeText = (type) => {
  const map = { RELATIVE: '相对关系', USER: '指定用户', ROLE: '指定角色' }
  return map[type] || type
}

// 添加/删除表单字段
const addFormField = () => {
  form.value.formSchema.push({
    field: '',
    label: '',
    type: 'text',
    required: false,
  })
}

const removeFormField = (index) => {
  form.value.formSchema.splice(index, 1)
}

// 简单字段名校验
const validateFieldName = (row) => {
  if (row.field && !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(row.field)) {
    ElMessage.warning(`字段名「${row.field}」只能以字母开头，包含字母、数字、下划线`)
  }
}

// 添加/删除节点
const addNode = () => {
  const maxOrder = Math.max(0, ...form.value.nodes.map((n) => n.nodeOrder || 0))
  form.value.nodes.push({
    nodeName: '',
    nodeOrder: maxOrder + 1,
    assigneeType: 'RELATIVE',
    assigneeExpr: '',
    conditionExpr: '',
  })
}

const removeNode = (index) => {
  form.value.nodes.splice(index, 1)
}

// 加载详情
const loadDetail = async () => {
  if (isCreateMode) return

  loading.value = true
  try {
    const res = await request.get(`/admin/definitions/${definitionId}`)
    if (res.code === 200) {
      const data = res.data

      // 解析 formSchema
      let formSchema = data.formSchema
      if (typeof formSchema === 'string') {
        try {
          formSchema = JSON.parse(formSchema)
        } catch {
          formSchema = []
        }
      }

      // 解析 nodes
      let nodes = data.nodes
      if (typeof nodes === 'string') {
        try {
          nodes = JSON.parse(nodes)
        } catch {
          nodes = []
        }
      }

      form.value = {
        name: data.name || '',
        roleCode: data.roleCode || '',
        status: data.status ?? 1,
        version: data.version || 1,
        formSchema: formSchema || [],
        nodes: nodes || [],
      }
    }
  } catch (error) {
    console.error('加载失败', error)
    ElMessage.error('加载流程详情失败')
    goBack()
  } finally {
    loading.value = false
  }
}

// 条件表达式校验（符号 + 变量存在性）
const validateConditionExpressions = (nodes, currentFormSchema) => {
  const schemaFields = currentFormSchema.map((f) => f.field)
  const keywords = ['true', 'false', 'null', 'nil']
  const varPattern = /[a-zA-Z_][a-zA-Z0-9_]*/g
  const allowedPattern = /^[a-zA-Z0-9_\s><=!&|().'"]*$/

  for (const node of nodes) {
    const expr = node.conditionExpr
    if (!expr || !expr.trim()) continue

    if (!allowedPattern.test(expr)) {
      ElMessage.warning(
        `节点「${node.nodeName}」的条件表达式包含非法字符（只能使用字母、数字、空格、比较符、逻辑符、括号等）`,
      )
      return false
    }

    const vars = expr.match(varPattern) || []
    for (const v of vars) {
      if (keywords.includes(v)) continue
      if (!schemaFields.includes(v)) {
        ElMessage.warning(`节点「${node.nodeName}」的条件表达式使用了不存在的表单字段「${v}」`)
        return false
      }
    }
  }
  return true
}

// 保存
const handleSave = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入流程名称')
    return
  }
  if (!form.value.roleCode) {
    ElMessage.warning('请选择适用角色')
    return
  }
  if (form.value.formSchema.length === 0) {
    ElMessage.warning('请至少添加一个表单字段')
    return
  }
  if (form.value.nodes.length === 0) {
    ElMessage.warning('请至少添加一个审批节点')
    return
  }

  for (const field of form.value.formSchema) {
    if (!field.field || !field.field.trim()) {
      ElMessage.warning('所有表单字段的「字段名」不能为空')
      return
    }
    if (!field.label || !field.label.trim()) {
      ElMessage.warning('所有表单字段的「标签」不能为空')
      return
    }
  }

  for (const node of form.value.nodes) {
    if (!node.nodeName || !node.nodeName.trim()) {
      ElMessage.warning('所有节点的名称不能为空')
      return
    }
    if (!node.assigneeExpr || !node.assigneeExpr.trim()) {
      ElMessage.warning(`节点「${node.nodeName}」的分配目标不能为空`)
      return
    }
  }

  for (const field of form.value.formSchema) {
    if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(field.field)) {
      ElMessage.warning('字段名必须以字母开头，只能包含字母、数字、下划线')
      return
    }
  }

  if (!validateConditionExpressions(form.value.nodes, form.value.formSchema)) {
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      roleCode: form.value.roleCode,
      formSchema: JSON.stringify(form.value.formSchema),
      nodes: form.value.nodes,
    }

    let res
    if (isCreateMode) {
      res = await request.post('/admin/definitions', payload)
    } else {
      res = await request.patch(`/admin/definitions/${definitionId}`, payload)
    }

    if (res.code === 200) {
      ElMessage.success(isCreateMode ? '创建成功' : '保存成功')
      router.push('/definitions')
    }
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.warning('保存失败，请检查条件表达式语法是否正确')
  } finally {
    saving.value = false
  }
}

const enterEditMode = () => {
  isEditMode.value = true
}

const cancelEdit = () => {
  if (isCreateMode) {
    goBack()
  } else {
    loadDetail()
    isEditMode.value = false
  }
}

const goBack = () => {
  router.push('/definitions')
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.definition-detail {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.section {
  margin-top: 24px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-title {
  font-size: 16px;
  font-weight: 500;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background-color: #fafafa;
}
</style>

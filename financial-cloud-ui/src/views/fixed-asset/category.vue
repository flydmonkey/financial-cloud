<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :model="queryParams"
        :inline="true"
        label-width="68px"
      >
        <el-form-item label="编码">
          <el-input
            v-model="queryParams.code"
            clearable
            placeholder="类别编码"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="名称">
          <el-input
            v-model="queryParams.name"
            clearable
            placeholder="类别名称"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery">
            查询
          </el-button>
          <el-button @click="resetQuery">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="common-card">
      <div class="btn-form">
        <el-button
          type="primary"
          @click="handleAdd"
        >
          新增
        </el-button>
        <el-button
          type="danger"
          :disabled="ids.length === 0"
          @click="handleDelete()"
        >
          批量删除
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        border
        :data="list"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="50"
          align="center"
        />
        <el-table-column
          prop="code"
          label="编码"
          width="100"
          align="center"
        />
        <el-table-column
          prop="name"
          label="名称"
          min-width="120"
        />
        <el-table-column
          prop="depreciationMethodLabel"
          label="折旧方法"
          width="120"
          align="center"
        />
        <el-table-column
          label="预计使用年限"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ row.usefulLifeYears ?? (row.usefulLifeMonths ? Math.floor(row.usefulLifeMonths / 12) : '') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="residualRate"
          label="净残值率%"
          width="100"
          align="center"
        />
        <el-table-column
          prop="fixedAssetSubjectName"
          label="固定资产科目"
          min-width="140"
        />
        <el-table-column
          prop="accumDeprSubjectName"
          label="累计折旧科目"
          min-width="140"
        />
        <el-table-column
          prop="remark"
          label="备注"
          min-width="100"
          show-overflow-tooltip
        />
        <el-table-column
          label="操作"
          width="90"
          fixed="right"
          align="center"
        >
          <template #default="{ row }">
            <el-button
              link
              icon="Edit"
              @click="handleUpdate(row)"
            />
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(row)"
            />
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNumber"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <el-drawer
      v-model="dialog.visible"
      :close-on-click-modal="false"
      size="480px"
    >
      <template #header>
        <h4>{{ dialog.title }}</h4>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="130px"
        inline-message
      >
        <el-form-item
          label="类别编码"
          prop="code"
        >
          <el-input
            v-model="form.code"
            placeholder="请输入编码"
          />
        </el-form-item>
        <el-form-item
          label="类别名称"
          prop="name"
        >
          <el-input
            v-model="form.name"
            placeholder="请输入名称"
          />
        </el-form-item>
        <el-form-item
          label="折旧方法"
          prop="depreciationMethod"
        >
          <el-select
            v-model="form.depreciationMethod"
            style="width: 100%"
          >
            <el-option
              label="平均年限法"
              value="STRAIGHT_LINE"
            />
            <el-option
              label="工作量法"
              value="UNITS_OF_PRODUCTION"
            />
            <el-option
              label="双倍余额递减法"
              value="DOUBLE_DECLINING"
            />
            <el-option
              label="年数总和法"
              value="SUM_OF_YEARS"
            />
            <el-option
              label="不计提折旧"
              value="NONE"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="form.depreciationMethod !== 'NONE' && form.depreciationMethod !== 'UNITS_OF_PRODUCTION'"
          label="预计使用年限"
          prop="usefulLifeYears"
        >
          <el-input-number
            v-model="form.usefulLifeYears"
            :min="1"
            :max="100"
            controls-position="right"
            style="width: 100%"
            @change="onYearsChange"
          />
        </el-form-item>
        <el-form-item
          v-if="form.depreciationMethod !== 'NONE'"
          label="预计使用期数(月)"
          prop="usefulLifeMonths"
        >
          <el-input-number
            v-model="form.usefulLifeMonths"
            :min="1"
            :max="1200"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="预计净残值率%"
          prop="residualRate"
        >
          <el-input-number
            v-model="form.residualRate"
            :min="0"
            :max="100"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item
          label="固定资产科目"
          prop="fixedAssetSubjectId"
        >
          <el-tree-select
            v-model="form.fixedAssetSubjectId"
            :data="subjectOptions"
            :props="subjectProps"
            check-strictly
            filterable
            clearable
            style="width: 100%"
            placeholder="请选择科目"
          />
        </el-form-item>
        <el-form-item
          label="累计折旧科目"
          prop="accumDeprSubjectId"
        >
          <el-tree-select
            v-model="form.accumDeprSubjectId"
            :data="subjectOptions"
            :props="subjectProps"
            check-strictly
            filterable
            clearable
            style="width: 100%"
            placeholder="请选择科目"
          />
        </el-form-item>
        <el-form-item
          label="备注"
          prop="remark"
        >
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="buttonLoading"
          @click="submitForm"
        >
          确定
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="FixedAssetCategory">
import {
  listAssetCategory,
  getAssetCategory,
  addAssetCategory,
  updateAssetCategory,
  delAssetCategory
} from '@/api/fixed-asset/category'
import { getTree } from '@/api/standard/standard-subject'
import bookStore from '@/store/modules/bookStore'
import modal from '@/plugins/modal'
import { reactive, ref, getCurrentInstance } from 'vue'
import type { FormInstance } from 'element-plus'

const { proxy } = getCurrentInstance() as any
const currBookStore = bookStore()
const loading = ref(false)
const buttonLoading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const ids = ref<string[]>([])
const formRef = ref<FormInstance>()
const subjectOptions = ref<any[]>([])
const subjectProps = { value: 'id', label: 'name', children: 'children' }

const dialog = reactive({ visible: false, title: '' })
const queryParams = reactive({
  bookId: currBookStore.bookId,
  pageNumber: 1,
  pageSize: 10,
  code: '',
  name: ''
})
const initForm = () => ({
  id: undefined as string | undefined,
  bookId: currBookStore.bookId,
  code: '',
  name: '',
  depreciationMethod: 'STRAIGHT_LINE',
  usefulLifeYears: 5,
  usefulLifeMonths: 60,
  residualRate: 5,
  fixedAssetSubjectId: '',
  accumDeprSubjectId: '',
  remark: ''
})
const form = reactive(initForm())
const rules = {
  code: [{ required: true, message: '类别编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '类别名称不能为空', trigger: 'blur' }],
  depreciationMethod: [{ required: true, message: '折旧方法不能为空', trigger: 'change' }],
  usefulLifeMonths: [{ required: true, message: '预计使用期数不能为空', trigger: 'blur' }],
  residualRate: [{ required: true, message: '净残值率不能为空', trigger: 'blur' }],
  fixedAssetSubjectId: [{ required: true, message: '固定资产科目不能为空', trigger: 'change' }],
  accumDeprSubjectId: [{ required: true, message: '累计折旧科目不能为空', trigger: 'change' }]
}

function onYearsChange(v: number | undefined) {
  if (v != null) {
    form.usefulLifeMonths = v * 12
  }
}

function findSubjectIdByCode(nodes: any[], code: string): string | undefined {
  for (const n of nodes || []) {
    if (n.code === code) {
      return n.id
    }
    const child = findSubjectIdByCode(n.children || [], code)
    if (child) return child
  }
  return undefined
}

function applyDefaultSubjects() {
  if (!form.fixedAssetSubjectId) {
    form.fixedAssetSubjectId = findSubjectIdByCode(subjectOptions.value, '1601') || ''
  }
  if (!form.accumDeprSubjectId) {
    form.accumDeprSubjectId = findSubjectIdByCode(subjectOptions.value, '1602') || ''
  }
}

function loadSubjects() {
  return getTree({ bookId: currBookStore.bookId }).then((res: any) => {
    subjectOptions.value = res.data || []
  })
}

function getList() {
  loading.value = true
  listAssetCategory(queryParams).then((res: any) => {
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNumber = 1
  getList()
}

function resetQuery() {
  queryParams.code = ''
  queryParams.name = ''
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((i) => i.id)
}

function handleAdd() {
  Object.assign(form, initForm())
  applyDefaultSubjects()
  dialog.title = '新增资产类别'
  dialog.visible = true
}

function handleUpdate(row: any) {
  getAssetCategory(row.id).then((res: any) => {
    Object.assign(form, initForm(), res.data || {})
    if (!form.usefulLifeYears && form.usefulLifeMonths) {
      form.usefulLifeYears = Math.floor(form.usefulLifeMonths / 12)
    }
    dialog.title = '编辑资产类别'
    dialog.visible = true
  })
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    buttonLoading.value = true
    const req = form.id ? updateAssetCategory(form) : addAssetCategory(form)
    req.then((res: any) => {
      if (res.code === 0) {
        modal.msgSuccess(res.message || '保存成功')
        dialog.visible = false
        getList()
      }
    }).finally(() => {
      buttonLoading.value = false
    })
  })
}

function handleDelete(row?: any) {
  const listIds = row?.id ? [row.id] : ids.value
  if (!listIds.length) return
  modal.confirm('确认删除所选资产类别？').then(() => {
    return delAssetCategory({ listIds })
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '删除成功')
      getList()
    }
  }).catch(() => undefined)
}

loadSubjects()
getList()
</script>

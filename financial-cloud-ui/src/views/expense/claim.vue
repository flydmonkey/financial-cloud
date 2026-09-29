<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="70px"
      >
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            style="width: 120px"
            clearable
            @change="handleQuery"
          >
            <el-option
              v-for="(label, value) in STATUS_LABELS"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            style="width: 200px"
            placeholder="单号/报销人/事由"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </el-button>
          <el-button
            type="primary"
            @click="openForm()"
          >
            新增报销单
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="common-card">
      <el-table
        v-loading="loading"
        :data="recordsList"
        border
        size="small"
      >
        <el-table-column
          label="单号"
          prop="claimNo"
          width="140"
        />
        <el-table-column
          label="报销人"
          prop="claimant"
          width="90"
          align="center"
        />
        <el-table-column
          label="报销日期"
          prop="claimDate"
          width="105"
          align="center"
        />
        <el-table-column
          label="事由"
          prop="summary"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          label="费用科目"
          prop="expenseSubjectName"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          label="付款科目"
          prop="fundSubjectName"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column
          label="金额"
          width="110"
          align="right"
        >
          <template #default="scope">
            {{ formatAmount(scope.row.amount) }}
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="90"
          align="center"
        >
          <template #default="scope">
            <el-tag
              :type="STATUS_TAG[scope.row.claimStatus] || 'info'"
              size="small"
            >
              {{ STATUS_LABELS[scope.row.claimStatus] || scope.row.claimStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="300"
          align="center"
        >
          <template #default="scope">
            <el-button
              v-if="editable(scope.row)"
              link
              type="primary"
              @click="openForm(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="editable(scope.row)"
              link
              type="primary"
              @click="handleSubmit(scope.row)"
            >
              提交
            </el-button>
            <el-button
              v-if="scope.row.claimStatus === 'submitted'"
              link
              type="success"
              @click="handleAudit(scope.row, true)"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.claimStatus === 'submitted'"
              link
              type="danger"
              @click="handleAudit(scope.row, false)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="scope.row.claimStatus === 'approved' && !scope.row.voucherId"
              link
              type="warning"
              @click="handleVoucher(scope.row)"
            >
              生成凭证
            </el-button>
            <el-button
              v-if="scope.row.voucherId"
              link
              type="primary"
              @click="goVoucher(scope.row)"
            >
              查看凭证
            </el-button>
            <el-button
              v-if="editable(scope.row)"
              link
              type="danger"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
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

    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑报销单' : '新增报销单'"
      width="520px"
      append-to-body
    >
      <el-form
        label-width="90px"
      >
        <el-form-item
          label="报销人"
          required
        >
          <el-input
            v-model="form.claimant"
            style="width: 220px"
            placeholder="报销人姓名"
          />
        </el-form-item>
        <el-form-item
          label="报销日期"
          required
        >
          <el-date-picker
            v-model="form.claimDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item
          label="费用科目"
          required
        >
          <el-cascader
            v-model="form.expensePath"
            style="width: 380px"
            filterable
            :options="subjectList"
            :props="cascaderProps"
            placeholder="借方科目（如管理费用）"
          />
        </el-form-item>
        <el-form-item
          label="付款科目"
          required
        >
          <el-cascader
            v-model="form.fundPath"
            style="width: 380px"
            filterable
            :options="subjectList"
            :props="cascaderProps"
            placeholder="贷方科目（如库存现金/银行存款）"
          />
        </el-form-item>
        <el-form-item
          label="金额"
          required
        >
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :precision="2"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="事由">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            placeholder="报销事由"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ExpenseClaim" lang="ts">
import {reactive, ref, toRefs, getCurrentInstance} from 'vue'
import {useRouter} from 'vue-router'
import {formatAmount} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import * as subjectApi from '@/api/standard/standard-subject'
import {cascaderSubjectProps} from '@/utils/Subjects'
import {
  expenseClaimPage,
  expenseClaimSave,
  expenseClaimSubmit,
  expenseClaimAudit,
  expenseClaimVoucher,
  expenseClaimDelete
} from '@/api/expense/expense'
import bookStore from '@/store/modules/bookStore'

const router = useRouter()
const {proxy} = getCurrentInstance() as any
const currBookStore = bookStore()

const STATUS_LABELS: Record<string, string> = {
  draft: '暂存',
  submitted: '已提交',
  approved: '已审核',
  rejected: '已拒绝'
}
const STATUS_TAG: Record<string, string> = {
  draft: 'info',
  submitted: 'warning',
  approved: 'success',
  rejected: 'danger'
}

const subjectList = ref<any>([])
const recordsList = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)

const cascaderProps = ref<any>({...cascaderSubjectProps, checkStrictly: true})

const data = reactive({
  queryParams: {
    status: '',
    keyword: '',
    pageNumber: 1,
    pageSize: 20
  },
  form: {
    id: '' as string,
    claimant: '',
    claimDate: parseTime(new Date(), '{y}-{m}-{d}'),
    expensePath: [] as string[],
    fundPath: [] as string[],
    amount: 0.01,
    summary: ''
  }
})
const {queryParams, form} = toRefs(data)

const editable = (row: any) => ['draft', 'rejected'].includes(row.claimStatus)

const lastOf = (path: string[]) => (Array.isArray(path) && path.length ? String(path[path.length - 1]) : '')

function getSubjectList() {
  subjectApi.getTree({bookId: currBookStore.bookId}).then((res: any) => {
    subjectList.value = res.data
  })
}

function getList() {
  loading.value = true
  expenseClaimPage(queryParams.value).then((res: any) => {
    recordsList.value = res.data.records
    total.value = res.data.total
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNumber = 1
  getList()
}

function openForm(row?: any) {
  if (row) {
    form.value = {
      id: row.id,
      claimant: row.claimant,
      claimDate: row.claimDate,
      expensePath: [row.expenseSubjectCode],
      fundPath: [row.fundSubjectCode],
      amount: Number(row.amount),
      summary: row.summary || ''
    }
  } else {
    form.value = {
      id: '',
      claimant: '',
      claimDate: parseTime(new Date(), '{y}-{m}-{d}'),
      expensePath: [],
      fundPath: [],
      amount: 0.01,
      summary: ''
    }
  }
  formVisible.value = true
}

function handleSave() {
  const expenseSubjectCode = lastOf(form.value.expensePath)
  const fundSubjectCode = lastOf(form.value.fundPath)
  if (!form.value.claimant || !expenseSubjectCode || !fundSubjectCode) {
    proxy?.$modal?.msgWarning('请完整填写报销人、费用科目与付款科目')
    return
  }
  saving.value = true
  expenseClaimSave({
    id: form.value.id || undefined,
    claimant: form.value.claimant,
    claimDate: form.value.claimDate,
    expenseSubjectCode,
    fundSubjectCode,
    amount: form.value.amount,
    summary: form.value.summary
  }).then(() => {
    proxy?.$modal?.msgSuccess('已保存')
    formVisible.value = false
    getList()
  }).finally(() => {
    saving.value = false
  })
}

function handleSubmit(row: any) {
  expenseClaimSubmit(row.id).then(() => {
    proxy?.$modal?.msgSuccess('已提交')
    getList()
  })
}

function handleAudit(row: any, approve: boolean) {
  if (approve) {
    proxy?.$modal?.confirm(`确认通过报销单 ${row.claimNo}？`).then(() => {
      expenseClaimAudit(row.id, true, '').then(() => {
        proxy?.$modal?.msgSuccess('已通过')
        getList()
      })
    })
  } else {
    proxy?.$modal?.prompt('请输入拒绝原因', `拒绝报销单 ${row.claimNo}`).then(({value}: any) => {
      expenseClaimAudit(row.id, false, value || '').then(() => {
        proxy?.$modal?.msgSuccess('已拒绝')
        getList()
      })
    })
  }
}

function handleVoucher(row: any) {
  expenseClaimVoucher(row.id).then(() => {
    proxy?.$modal?.msgSuccess('报销凭证已生成（暂存态，请到凭证列表提交审核）')
    getList()
  })
}

function goVoucher(row: any) {
  router.push({path: '/voucher/voucher-edit', query: {id: row.voucherId}})
}

function handleDelete(row: any) {
  proxy?.$modal?.confirm(`确认删除报销单 ${row.claimNo}？`).then(() => {
    expenseClaimDelete(row.id).then(() => {
      proxy?.$modal?.msgSuccess('已删除')
      getList()
    })
  })
}

getSubjectList()
getList()
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.common-card {
  margin-bottom: 12px;
}
</style>

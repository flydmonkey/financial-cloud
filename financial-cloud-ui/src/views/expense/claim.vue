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
        row-key="id"
      >
        <el-table-column type="expand">
          <template #default="scope">
            <div class="item-expand">
              <el-table
                :data="scope.row.items || []"
                size="small"
                border
              >
                <el-table-column
                  label="费用科目"
                  prop="expenseSubjectName"
                  min-width="200"
                />
                <el-table-column
                  label="金额"
                  width="130"
                  align="right"
                >
                  <template #default="line">
                    {{ formatAmount(line.row.amount) }}
                  </template>
                </el-table-column>
                <el-table-column
                  label="费用说明"
                  prop="summary"
                  min-width="200"
                />
              </el-table>
              <div
                v-if="scope.row.rejectReason"
                class="reject-reason"
              >
                拒绝原因：{{ scope.row.rejectReason }}
              </div>
            </div>
          </template>
        </el-table-column>
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
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          label="费用科目"
          prop="expenseSubjectName"
          min-width="150"
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
          width="290"
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
      width="760px"
      append-to-body
    >
      <el-form
        :inline="true"
        label-width="80px"
      >
        <el-form-item
          label="报销人"
          required
        >
          <el-input
            v-model="form.claimant"
            style="width: 180px"
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
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item
          label="付款科目"
          required
        >
          <el-cascader
            v-model="form.fundPath"
            style="width: 260px"
            filterable
            :options="subjectList"
            :props="cascaderProps"
            placeholder="贷方（库存现金/银行存款）"
          />
        </el-form-item>
        <el-form-item label="事由">
          <el-input
            v-model="form.summary"
            style="width: 300px"
            placeholder="报销事由"
          />
        </el-form-item>
      </el-form>

      <div class="items-head">
        <span class="items-title">费用明细</span>
        <el-button
          size="small"
          type="primary"
          plain
          @click="addLine"
        >
          添加明细
        </el-button>
      </div>
      <el-table
        :data="form.lines"
        size="small"
        border
      >
        <el-table-column
          label="费用科目"
          min-width="260"
        >
          <template #default="scope">
            <el-cascader
              v-model="scope.row.path"
              style="width: 100%"
              filterable
              :options="subjectList"
              :props="cascaderProps"
              placeholder="借方费用科目"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="金额"
          width="160"
        >
          <template #default="scope">
            <el-input-number
              v-model="scope.row.amount"
              :min="0.01"
              :precision="2"
              style="width: 140px"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="费用说明"
          min-width="160"
        >
          <template #default="scope">
            <el-input
              v-model="scope.row.summary"
              placeholder="选填"
            />
          </template>
        </el-table-column>
        <el-table-column
          label=""
          width="60"
          align="center"
        >
          <template #default="scope">
            <el-button
              link
              type="danger"
              :disabled="form.lines.length <= 1"
              @click="form.lines.splice(scope.$index, 1)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="items-total">
        合计：<span class="total-amount">{{ formatAmount(totalAmount) }}</span>
      </div>

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
import {computed, reactive, ref, toRefs, getCurrentInstance} from 'vue'
import {useRouter} from 'vue-router'
import {formatAmount} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import * as subjectApi from '@/api/standard/standard-subject'
import {cascaderSubjectProps} from '@/utils/Subjects'
import {
  expenseClaimPage,
  expenseClaimDetail,
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

const emptyLine = () => ({path: [] as string[], amount: 0.01, summary: ''})

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
    fundPath: [] as string[],
    summary: '',
    lines: [emptyLine()] as any[]
  }
})
const {queryParams, form} = toRefs(data)

const editable = (row: any) => ['draft', 'rejected'].includes(row.claimStatus)

const lastOf = (path: string[]) => (Array.isArray(path) && path.length ? String(path[path.length - 1]) : '')

const totalAmount = computed(() =>
  form.value.lines.reduce((sum: number, line: any) => sum + (Number(line.amount) || 0), 0))

function addLine() {
  form.value.lines.push(emptyLine())
}

function getSubjectList() {
  subjectApi.getTree({bookId: currBookStore.bookId}).then((res: any) => {
    subjectList.value = res.data
  })
}

function getList() {
  loading.value = true
  expenseClaimPage(queryParams.value).then(async (res: any) => {
    const rows = res.data.records || []
    // 逐单拉明细供展开行展示
    await Promise.all(rows.map((row: any) =>
      expenseClaimDetail(row.id).then((d: any) => {
        row.items = d.data.items || []
      }).catch(() => {
        row.items = []
      })))
    recordsList.value = rows
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
    expenseClaimDetail(row.id).then((res: any) => {
      const d = res.data
      form.value = {
        id: d.id,
        claimant: d.claimant,
        claimDate: d.claimDate,
        fundPath: [d.fundSubjectCode],
        summary: d.summary || '',
        lines: (d.items || []).map((it: any) => ({
          path: [it.expenseSubjectCode],
          amount: Number(it.amount),
          summary: it.summary || ''
        }))
      }
      if (!form.value.lines.length) {
        form.value.lines = [emptyLine()]
      }
      formVisible.value = true
    })
  } else {
    form.value = {
      id: '',
      claimant: '',
      claimDate: parseTime(new Date(), '{y}-{m}-{d}'),
      fundPath: [],
      summary: '',
      lines: [emptyLine()]
    }
    formVisible.value = true
  }
}

function handleSave() {
  const fundSubjectCode = lastOf(form.value.fundPath)
  const items = form.value.lines
    .filter((line: any) => lastOf(line.path))
    .map((line: any) => ({
      expenseSubjectCode: lastOf(line.path),
      amount: line.amount,
      summary: line.summary
    }))
  if (!form.value.claimant || !fundSubjectCode) {
    proxy?.$modal?.msgWarning('请填写报销人并选择付款科目')
    return
  }
  if (!items.length) {
    proxy?.$modal?.msgWarning('请至少填写一行费用明细')
    return
  }
  saving.value = true
  expenseClaimSave({
    id: form.value.id || undefined,
    claimant: form.value.claimant,
    claimDate: form.value.claimDate,
    fundSubjectCode,
    summary: form.value.summary,
    items
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

.item-expand {
  padding: 8px 24px;
}

.reject-reason {
  margin-top: 6px;
  color: #f56c6c;
  font-size: 12px;
}

.items-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 8px;

  .items-title {
    font-weight: bold;
  }
}

.items-total {
  margin-top: 8px;
  text-align: right;
  color: #606266;

  .total-amount {
    font-weight: bold;
    color: #303133;
  }
}
</style>

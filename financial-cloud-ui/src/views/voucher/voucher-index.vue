<template>
  <div class="app-container voucher-list-page">
    <el-card class="common-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <span class="toolbar-label">凭证期间</span>
          <el-date-picker
            v-model="periodRange"
            type="monthrange"
            range-separator="~"
            start-placeholder="开始期间"
            end-placeholder="结束期间"
            format="YYYY年MM期"
            value-format="YYYY-MM"
            style="width: 260px"
            @change="handlePeriodChange"
          />
          <el-checkbox v-model="showSubtotal">
            显示凭证金额小计
          </el-checkbox>
          <el-button
            icon="Refresh"
            @click="getList"
          >
            刷新
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-dropdown
            split-button
            class="toolbar-split-btn"
            :disabled="ids.length === 0"
            @click="handleAudit()"
          >
            审核
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleUnaudit()">
                  反审核
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown
            split-button
            class="toolbar-split-btn"
            :disabled="ids.length === 0"
            @click="handleSender()"
          >
            过账
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleUnsender()">
                  反过账
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click">
            <el-button>
              更多
              <el-icon class="el-icon--right">
                <ArrowDown />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleExport">
                  导出
                </el-dropdown-item>
                <el-dropdown-item @click="handleManager">
                  主管复核
                </el-dropdown-item>
                <el-dropdown-item @click="handleShowVoucherSuccessive">
                  凭证整理
                </el-dropdown-item>
                <el-dropdown-item
                  divided
                  :disabled="ids.length === 0"
                  @click="handleDelete()"
                >
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <el-table
        v-loading="loading"
        max-height="620"
        :data="tableRows"
        border
        row-key="rowKey"
        :span-method="spanMethod"
        :row-class-name="tableRowClassName"
        show-summary
        :summary-method="getSummaries"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="42"
          align="center"
          fixed="left"
        />
        <el-table-column
          label="日期"
          align="center"
          width="100"
          fixed="left"
        >
          <template #default="scope">
            <span v-if="scope.row.rowType === 'entry' && scope.row.itemIndex === 0">
              {{ parseTime(scope.row.voucher.voucherDate, '{y}-{m}-{d}') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="凭证字号"
          align="center"
          width="88"
          fixed="left"
        >
          <template #default="scope">
            <el-link
              v-if="scope.row.rowType === 'entry' && scope.row.itemIndex === 0"
              type="primary"
              :underline="false"
              @click="handlePreview(scope.row)"
            >
              {{ formatVoucherWord(scope.row.voucher) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="摘要"
          align="left"
          header-align="center"
          min-width="160"
          prop="summary"
          show-overflow-tooltip
        >
          <template #default="scope">
            <span :class="{ 'subtotal-label': scope.row.rowType === 'subtotal' }">
              {{ scope.row.rowType === 'subtotal' ? '小计' : scope.row.summary }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="科目"
          align="left"
          header-align="center"
          min-width="240"
          show-overflow-tooltip
        >
          <template #default="scope">
            <span v-if="scope.row.rowType === 'entry'">{{ formatSubject(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="借方金额"
          align="right"
          header-align="center"
          prop="debitAmount"
          width="120"
        >
          <template #default="scope">
            {{ formatAmountCell(scope.row.debitAmount) }}
          </template>
        </el-table-column>
        <el-table-column
          label="贷方金额"
          align="right"
          header-align="center"
          prop="creditAmount"
          width="120"
        >
          <template #default="scope">
            {{ formatAmountCell(scope.row.creditAmount) }}
          </template>
        </el-table-column>
        <el-table-column
          label="附件"
          align="center"
          width="56"
        >
          <template #default="scope">
            <span
              v-if="scope.row.rowType === 'entry' && scope.row.itemIndex === 0 && scope.row.voucher.receiptNum > 0"
              class="attachment-tag"
            >
              {{ scope.row.voucher.receiptNum }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="制单人"
          align="center"
          prop="createdName"
          width="80"
          show-overflow-tooltip
        >
          <template #default="scope">
            <span v-if="scope.row.rowType === 'entry' && scope.row.itemIndex === 0">
              {{ scope.row.voucher.createdName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="审核人"
          align="center"
          width="80"
          show-overflow-tooltip
        >
          <template #default="scope">
            <span v-if="scope.row.rowType === 'entry' && scope.row.itemIndex === 0">
              {{ scope.row.voucher.auditMemberName || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          align="center"
          width="100"
        >
          <template #default="scope">
            <div
              v-if="scope.row.rowType === 'entry' && scope.row.itemIndex === 0"
              v-html="getVoucherStatusDesc(scope.row.voucher.status, scope.row.voucher.senderId)"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          align="center"
          width="88"
          fixed="right"
        >
          <template #default="scope">
            <template v-if="scope.row.rowType === 'entry'">
              <el-tooltip
                v-if="canEditVoucher(scope.row.voucher)"
                content="编辑"
              >
                <el-button
                  link
                  icon="Edit"
                  @click="handleUpdate(scope.row)"
                />
              </el-tooltip>
              <el-tooltip
                v-else
                content="查看"
              >
                <el-button
                  link
                  icon="View"
                  @click="handlePreview(scope.row)"
                />
              </el-tooltip>
              <el-tooltip
                v-if="'reviewing' === scope.row.voucher.status"
                content="撤回"
              >
                <el-button
                  link
                  icon="RemoveFilled"
                  type="danger"
                  @click="handleCancel(scope.row)"
                />
              </el-tooltip>
              <el-tooltip
                v-if="isDeletable(scope.row.voucher)"
                content="删除"
              >
                <el-button
                  link
                  icon="Delete"
                  type="danger"
                  @click="handleDelete(scope.row)"
                />
              </el-tooltip>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total>0"
        v-model:page="queryParams.pageNumber"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 检查结果 -->
    <el-dialog
      v-model="dialogVoucherSuccessive.visible"
      title="检查结果"
      width="1200px"
      style="margin-top: 20vh !important;"
    >
      <el-alert
        v-if="dialogVoucherSuccessive.isNumber"
        title="恭喜您,所有凭证已连号，无需整理"
        type="success"
        :closable="false"
        show-icon
      />

      <!--      <el-form v-model="dialogVoucherSuccessive" inline style="margin-top: 20px">-->
      <!--        <el-form-item label="凭证字">-->
      <!--          <el-select style="width: 60px" v-model="dialogVoucherSuccessive.wordHead" placeholder="凭证字">-->
      <!--            <el-option label="所有" value=""/>-->
      <!--            <el-option label="记" value="记"/>-->
      <!--            <el-option label="收" value="收"/>-->
      <!--            <el-option label="付" value="付"/>-->
      <!--            <el-option label="转" value="转"/>-->
      <!--          </el-select>-->
      <!--        </el-form-item>-->
      <!--        <el-form-item label="起始凭证号">-->
      <!--          <el-input-number v-model="dialogVoucherSuccessive.startWordNumber" :min="1"></el-input-number>-->
      <!--        </el-form-item>-->
      <!--        <el-form-item>-->
      <!--          <el-radio-group v-model="dialogVoucherSuccessive.successiveMethod">-->
      <!--            <el-radio-button label="按凭证号顺次前移补齐断号" value="sequential"/>-->
      <!--            <el-radio-button label="按凭证日期重新顺次编号" value="date"/>-->
      <!--          </el-radio-group>-->
      <!--        </el-form-item>-->
      <!--        <el-form-item label="作废凭证参与凭证整理">-->
      <!--          <el-switch v-model="dialogVoucherSuccessive.nullify"/>-->
      <!--        </el-form-item>-->
      <!--        <el-form-item>-->
      <!--          <el-button @click="handleVoucherSuccessiveQuery">搜索</el-button>-->
      <!--        </el-form-item>-->
      <!--      </el-form>-->

      <el-table
        v-loading="dialogVoucherSuccessive.loading"
        border
        :data="voucherSuccessiveList"
      >
        <el-table-column
          label="原始凭证号"
          prop="sourceWord"
          align="center"
        />
        <el-table-column
          label="新凭证号"
          prop="targetWord"
          align="center"
        />
        <template #empty>
          <div style="text-align: center">
            暂无记录
          </div>
        </template>
      </el-table>

      <template #footer>
        <el-button @click="dialogVoucherSuccessive.visible = false">
          取消
        </el-button>
        <el-button
          v-if="voucherSuccessiveList.length > 0"
          v-loading="dialogVoucherSuccessive.btnLoading"
          type="primary"
          @click="handleVoucherSuccessiveUpdate"
        >
          确认整理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BooksVoucher">
import * as voucherApis from "@/api/voucher/voucher";
import {useI18n} from "vue-i18n";
import {useRouter} from "vue-router";
import {getVoucherStatusDesc} from "@/utils/enums/VoucherStatusEnum"
import {parseTime} from "@/utils/financialCloud";
import {formatAmount} from "@/utils";
import {reactive, computed, ref} from "vue";
import {ArrowDown} from "@element-plus/icons-vue";
import bookStore from "@/store/modules/bookStore";
import {downloadData} from "@/utils/index"

const currBookStore = bookStore()
const router = useRouter();
const {proxy} = getCurrentInstance();
const {t} = useI18n()
const booksVoucherList = ref([]);
const showSubtotal = ref(false);
const periodRange = ref([]);
const MERGE_COLUMN_INDEXES = [0, 1, 2, 7, 8, 9, 10, 11];
const voucherSuccessiveList = ref([]);
const loading = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const dialogVoucherSuccessive = reactive({
  visible: false,
  loading: false,
  btnLoading: false,
  title: '',
  isNumber: false,
  successiveMethod: "sequential", // 按凭证号顺次前移补齐断号（sequential），按凭证日期重新顺次编号（date）
  nullify: true,
  wordHead: '记',
  startWordNumber: 1
});
const data = reactive({
  queryParams: {
    pageNumber: 1,
    pageSize: 10,
    orderByColumn: "voucherDate,carryForward,wordHead,wordNum",
    isAsc: "desc,asc,desc,desc",
    word: null,
    bookId: null,
    companyName: null,
    voucherYear: null,
    voucherMonth: null,
    voucherDateStart: null,
    voucherDateEnd: null,
    voucherDate: null,
    includeItems: true,
  },
});

const {queryParams} = toRefs(data);

function initPeriodRange() {
  const term = currBookStore.termCurrent || parseTime(new Date(), "{y}-{m}")
  periodRange.value = [term, term]
  applyPeriodRange(periodRange.value)
}

function buildTableRows(vouchers) {
  const rows = []
  vouchers.forEach((voucher, voucherGroupIndex) => {
    const items = voucher.items?.length
        ? voucher.items
        : [{
          id: `${voucher.id}-placeholder`,
          summary: voucher.remark || '-',
          subjectName: '-',
          debitAmount: voucher.debitAmount,
          creditAmount: voucher.creditAmount,
        }]
    const groupSize = items.length + (showSubtotal.value && items.length > 1 ? 1 : 0)
    items.forEach((item, itemIndex) => {
      rows.push({
        rowKey: `${voucher.id}-${item.id || itemIndex}`,
        rowType: 'entry',
        voucherId: voucher.id,
        voucher,
        voucherGroupIndex,
        itemIndex,
        groupSize,
        summary: item.summary,
        subjectName: item.subjectName,
        subjectCode: item.subjectCode,
        debitAmount: item.debitAmount,
        creditAmount: item.creditAmount,
      })
    })
    if (showSubtotal.value && items.length > 1) {
      rows.push({
        rowKey: `${voucher.id}-subtotal`,
        rowType: 'subtotal',
        voucherId: voucher.id,
        voucher,
        voucherGroupIndex,
        itemIndex: items.length,
        groupSize,
        debitAmount: voucher.debitAmount,
        creditAmount: voucher.creditAmount,
      })
    }
  })
  return rows
}

const tableRows = computed(() => buildTableRows(booksVoucherList.value))

function spanMethod({row, columnIndex}) {
  if (row.rowType === 'subtotal') {
    if (MERGE_COLUMN_INDEXES.includes(columnIndex)) {
      return [0, 0]
    }
    if (columnIndex === 3) {
      return [1, 2]
    }
    return [1, 1]
  }
  if (MERGE_COLUMN_INDEXES.includes(columnIndex)) {
    if (row.itemIndex === 0) {
      return [row.groupSize, 1]
    }
    return [0, 0]
  }
  return [1, 1]
}

function tableRowClassName({row}) {
  if (row.rowType === 'subtotal') {
    return 'voucher-subtotal-row'
  }
  return row.voucherGroupIndex % 2 === 0 ? 'voucher-group-even' : 'voucher-group-odd'
}

function formatVoucherWord(voucher) {
  if (!voucher) {
    return ''
  }
  const head = voucher.wordHead || '记'
  const num = voucher.wordNum ?? ''
  return num !== '' ? `${head}-${num}` : head
}

function formatSubject(row) {
  if (!row.subjectName) {
    return '-'
  }
  const code = row.subjectCode || row.subjectName.split('-')[0]
  if (row.subjectName.includes(code)) {
    return row.subjectName.replace('-', ' ')
  }
  return `${code} ${row.subjectName}`
}

function formatAmountCell(value) {
  if (value === null || value === undefined || Number(value) === 0) {
    return ''
  }
  return formatAmount(value)
}

function getSummaries(param) {
  const {columns, data} = param
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (column.property === 'debitAmount' || column.property === 'creditAmount') {
      const total = data
          .filter(row => row.rowType === 'entry')
          .reduce((sum, row) => sum + Number(row[column.property] || 0), 0)
      sums[index] = formatAmount(total)
      return
    }
    sums[index] = ''
  })
  return sums
}

function lastDayOfMonth(yearMonth) {
  const [year, month] = yearMonth.split('-').map(Number)
  const day = new Date(year, month, 0).getDate()
  return `${yearMonth}-${String(day).padStart(2, '0')}`
}

function applyPeriodRange(range) {
  if (!range || range.length !== 2) {
    queryParams.value.voucherDateStart = null
    queryParams.value.voucherDateEnd = null
    queryParams.value.voucherYear = null
    queryParams.value.voucherMonth = null
    return
  }
  const [start, end] = range
  queryParams.value.voucherYear = null
  queryParams.value.voucherMonth = null
  queryParams.value.voucherDateStart = `${start}-01`
  queryParams.value.voucherDateEnd = lastDayOfMonth(end)
}

function handlePeriodChange(range) {
  applyPeriodRange(range)
  handleQuery()
}

initPeriodRange()

/** 查询凭证记录列表 */
function getList() {
  loading.value = true;
  voucherApis.listVouchers(queryParams.value).then(response => {
    booksVoucherList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNumber = 1;
  getList();
}

// 多选框选中数据（按凭证去重）
function handleSelectionChange(selection) {
  ids.value = [...new Set(selection.map(item => item.voucherId))];
  single.value = ids.value.length !== 1;
  multiple.value = !ids.value.length;
}

/** 新增按钮操作 */
function handleAdd() {
  router.push({
    path: "/voucher/voucher-edit"
  })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  const _id = row?.voucherId || row?.voucher?.id || ids.value
  if (!_id) {
    return
  }
  router.push({
    path: "/voucher/voucher-edit",
    query: { id: String(_id) },
  })
}

/** 查看操作 */
function handlePreview(row) {
  const _id = row?.voucherId || row?.voucher?.id || ids.value
  if (!_id) {
    return
  }
  router.push({
    path: "/voucher/voucher-edit",
    query: { id: String(_id), readonly: '1' },
  })
}

/** 修改按钮操作 */
function handleCancel(row) {
  const voucherId = row?.voucherId || row?.voucher?.id || row?.id
  if (!voucherId) {
    return
  }
  proxy.$modal.confirm('确认撤回该凭证的审核申请？').then(() => {
    return voucherApis.cancelVoucherByIds(voucherId);
  }).then(() => {
    proxy.$modal.msgSuccess("已取消");
    getList()
  }).catch(() => {
  });
}

function getSelectedVouchers(row) {
  if (row?.voucherId) {
    return booksVoucherList.value.filter(item => item.id === row.voucherId)
  }
  if (row?.voucher?.id) {
    return booksVoucherList.value.filter(item => item.id === row.voucher.id)
  }
  if (row?.id) {
    return booksVoucherList.value.filter(item => item.id === row.id)
  }
  const selected = new Set(ids.value)
  return booksVoucherList.value.filter(item => selected.has(item.id))
}

function filterVoucherIdsByStatus(status, row) {
  return getSelectedVouchers(row)
      .filter(item => item.status === status)
      .map(item => item.id)
}

function filterPostableVoucherIds(row) {
  return getSelectedVouchers(row)
      .filter(item => item.status === 'completed' && !item.senderId)
      .map(item => item.id)
}

function filterUnauditVoucherIds(row) {
  return getSelectedVouchers(row)
      .filter(item => item.status === 'completed' && !item.senderId && isEditable(item.voucherDate))
      .map(item => item.id)
}

function filterUnsenderVoucherIds(row) {
  return getSelectedVouchers(row)
      .filter(item => !!item.senderId && isEditable(item.voucherDate))
      .map(item => item.id)
}

function canEditVoucher(voucher) {
  if (!voucher?.voucherDate || voucher.status === 'cancelled') {
    return false
  }
  if (voucher.senderId) {
    return false
  }
  return isEditable(voucher.voucherDate)
}

function isDeletable(voucher) {
  if (!voucher?.voucherDate || voucher.status !== 'draft' || voucher.senderId) {
    return false
  }
  return currBookStore.termCurrent <= voucher.voucherDate.substring(0, 7)
}

function showActionResult(res, fallback = "操作成功") {
  proxy.$modal.msgSuccess(res?.message || fallback)
}

function showActionError(err, fallback = "操作失败") {
  if (proxy.$modal.isCancel(err)) {
    return
  }
  proxy.$modal.msgError(err?.message || fallback)
}

function handleSubmit(row) {
  const submitIds = filterVoucherIdsByStatus('draft', row)
  if (!submitIds.length) {
    proxy.$modal.msgError("没有可以提交的凭证项。");
    return
  }
  voucherApis.submitBatch(submitIds.join(",")).then(res => {
    getList();
    showActionResult(res)
  }).catch((err) => {
    showActionError(err)
  })
}

function handleAudit(row) {
  const auditIds = filterVoucherIdsByStatus('reviewing', row)
  if (!auditIds.length) {
    proxy.$modal.msgError("没有可以审核的凭证项。");
    return
  }
  proxy.$modal.confirm(`确认审核 ${auditIds.length} 条凭证？`).then(function () {
    return voucherApis.auditBatch(auditIds.join(","));
  }).then((res) => {
    getList();
    showActionResult(res)
  }).catch((err) => {
    showActionError(err)
  });
}

function handleUnaudit(row) {
  const unauditIds = filterUnauditVoucherIds(row)
  if (!unauditIds.length) {
    proxy.$modal.msgError("没有可以反审核的凭证项（需为已审核且未过账）。");
    return
  }
  proxy.$modal.confirm(`确认反审核 ${unauditIds.length} 条凭证？`).then(function () {
    return voucherApis.unauditBatch(unauditIds.join(","));
  }).then((res) => {
    getList();
    showActionResult(res)
  }).catch((err) => {
    showActionError(err)
  });
}

function handleSender(row) {
  const senderIds = filterPostableVoucherIds(row)
  if (!senderIds.length) {
    proxy.$modal.msgError("没有可以过账的凭证项（需为已审核且未过账）。");
    return
  }
  proxy.$modal.confirm(`确认过账 ${senderIds.length} 条凭证？`).then(function () {
    return voucherApis.senderBatch(senderIds.join(","));
  }).then((res) => {
    getList();
    showActionResult(res)
  }).catch((err) => {
    showActionError(err)
  });
}

function handleUnsender(row) {
  const unsenderIds = filterUnsenderVoucherIds(row)
  if (!unsenderIds.length) {
    proxy.$modal.msgError("没有可以反过账的凭证项（需为已过账且所在期间未结账）。");
    return
  }
  proxy.$modal.confirm(`确认反过账 ${unsenderIds.length} 条凭证？`).then(function () {
    return voucherApis.unsenderBatch(unsenderIds.join(","));
  }).then((res) => {
    getList();
    showActionResult(res)
  }).catch((err) => {
    showActionError(err)
  });
}

function handleManager(row) {
  const managerIds = filterVoucherIdsByStatus('completed', row)
  if (!managerIds.length) {
    proxy.$modal.msgError("没有可以主管复核的凭证项（需为已完成状态）。");
    return
  }
  proxy.$modal.confirm(`确认主管复核 ${managerIds.length} 条凭证？`).then(function () {
    return voucherApis.manageBatch(managerIds.join(","));
  }).then((res) => {
    getList();
    showActionResult(res)
  }).catch((err) => {
    showActionError(err)
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  let deleteIds = []
  if (row?.voucherId || row?.voucher?.id) {
    const voucher = row.voucher || booksVoucherList.value.find(item => item.id === (row.voucherId || row.voucher?.id))
    if (voucher && isDeletable(voucher)) {
      deleteIds = [voucher.id]
    }
  } else {
    deleteIds = getSelectedVouchers().filter(isDeletable).map(item => item.id)
  }
  if (!deleteIds.length) {
    proxy.$modal.msgError("没有可以删除的凭证项（仅暂存且当期及以后凭证可删）。");
    return
  }
  proxy.$modal.confirm(`删除凭证可能导致不连号，确认删除 ${deleteIds.length} 条凭证？`).then(function () {
    return voucherApis.deleteBatch(deleteIds.join(","));
  }).then(() => {
    getList();image.png
    proxy.$modal.msgSuccess("删除成功");
  }).catch((err) => {
    showActionError(err)
  });
}

/**
 * 凭证连号检查
 */
function handleShowVoucherSuccessive() {
  dialogVoucherSuccessive.visible = true
  dialogVoucherSuccessive.isNumber = false
  voucherSuccessiveList.value = []
  handleVoucherSuccessiveQuery()
}

/**
 * 更新凭证为连号
 */
function handleVoucherSuccessiveUpdate() {
  dialogVoucherSuccessive.btnLoading = true
  voucherApis.updateVoucherSuccessive(voucherSuccessiveList.value).then(res => {
    proxy.$modal.msgSuccess("凭证更新成功")
    dialogVoucherSuccessive.visible = false
    getList()
  }).finally(() => {
    dialogVoucherSuccessive.btnLoading = false
  })

  dialogVoucherSuccessive.visible = false
}

function handleVoucherSuccessiveQuery() {
  dialogVoucherSuccessive.loading = true
  voucherApis.getVoucherSuccessiveList(dialogVoucherSuccessive).then(res => {
    voucherSuccessiveList.value = res.data
    if (res.data.length === 0) {
      dialogVoucherSuccessive.isNumber = true
    }
  }).finally(() => {
    dialogVoucherSuccessive.loading = false
  })
}

/**
 * 导出
 */
function handleExport() {
  voucherApis.exportVouchers(queryParams.value).then(data => {
    downloadData(data, "凭证 " + parseTime(new Date()) + ".xlsx")
  })
}

function isEditable(date) {
  const now = new Date(currBookStore.termCurrent + "-01")
  return parseTime(new Date(date), "{y}-{m}-{d}") >= parseTime(now, "{y}-{m}-{d}")
}

getList();
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.voucher-list-page {
  :deep(.el-table) {
    .voucher-group-even td {
      background-color: #fff;
    }

    .voucher-group-odd td {
      background-color: #f0f7ff;
    }

    .voucher-subtotal-row td {
      background-color: #fafafa !important;
      font-weight: 600;
    }
  }
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }

  .toolbar-label {
    color: #606266;
    font-size: 14px;
  }

  .toolbar-split-btn {
    width: auto;

    :deep(.el-button-group) {
      display: inline-flex;
      width: auto;
    }

    :deep(.el-button) {
      min-width: unset;
      padding-left: 12px;
      padding-right: 12px;
    }

    :deep(.el-dropdown__caret-button) {
      padding-left: 6px;
      padding-right: 6px;
    }
  }
}

.subtotal-label {
  font-weight: 600;
  color: #606266;
}

.attachment-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
  border-radius: 10px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
}

.common-card {
  margin-bottom: 15px;
}
</style>

<template>
  <div class="app-container" @keydown="handleKeydown">
    <!--  功能区左侧  -->
    <div v-if="!isPrintMode" class="top-funs top-funs-left" :class="{topFunsUpdate: !!formData.id && props.dialog}">
      <span class="bottom-counts-item" :class="{isNotPh: !loanBalance()}">
        借贷平衡：{{ loanBalance() ? "是" : "否" }}</span>
    </div>
    <!--  功能区  -->
    <div v-if="!isPrintMode" class="top-funs" :style="{top: auto? '204px' : '80px'}">
      <el-button v-if="props.edit" @click="onAddItem">
        添加一项
      </el-button>
      <el-tooltip v-if="props.edit" content="确保总账科目对应的金额已正确录入！">
        <el-button v-loading="submitButtonLoading" @click="onSubmitDraft">
          暂存
        </el-button>
      </el-tooltip>
      <el-tooltip
          v-if="props.edit && formData.voucherDate && formData.voucherDate.startsWith(currBookStore.termCurrent)"
          content="确保总账科目对应的金额已正确录入！">
        <el-button v-loading="submitButtonLoading" @click="onSubmit">
          提交
        </el-button>
      </el-tooltip>
      <el-tooltip v-if="props.edit && !props.dialog" content="清空当前数据，建立新的凭证信息！">
        <el-button @click="onReset">
          新增凭证
        </el-button>
      </el-tooltip>
      <el-button @click="onPrint">
        打印
      </el-button>
    </div>
    <div ref="printMe" class="printable-content" id="printable-content"
         @click="closeOverlayOnly"
         :style="printContentStyle">
      <div v-for="(sheetData, sheetIndex) in tableSheets"
           :key="sheetIndex"
           class="voucher-print-sheet">
      <!--   标题头   -->
      <div class="header-title">
        <span class="header-title-text">
          <span class="text">记&nbsp;&nbsp;&nbsp;账&nbsp;&nbsp;&nbsp;凭&nbsp;&nbsp;&nbsp;证</span>
        </span>
        <span class="header-title-time">
          <el-date-picker v-if="!isReadonlyDisplay"
                          class="header-title-date"
                          :clearable="false"
                          :disabled-date="isCurrentOrFutureMonth"
                          v-model="formData.voucherDate"
                          type="date" placeholder="选择日期"
                          value-format="YYYY-MM-DD" format="YYYY年MM月DD日"
                          :shortcuts="shortcuts"
                          @change="handleVoucherDate"></el-date-picker>
          <span v-else>{{ formatVoucherDateChinese() }}</span>
        </span>
      </div>
      <!-- 公司信息部分 -->
      <div class="company-info">
        <div class="company-info-left company-info-item no-border-input">
          <div class="company-info-item">
            <span>公司名称：</span>
            <!--            <el-input v-if="!isPrintMode"-->
            <!--                      style="width: 300px" :input-style="{}"-->
            <!--                      v-model="formData.companyName"/>-->
            <span>{{ formData.companyName }}</span>
          </div>
        </div>
        <div class="company-info-right">
          <div class="company-info-item">
            <div v-if="!isReadonlyDisplay" class="no-border-input input-number voucher-word-num">
              <span>凭证编号：</span>
              <el-select v-model="formData.wordHead" placeholder="字头"
                         style="width: 50px" size="small" @change="handleWordHead">
                <el-option label="记" value="记"/>
                <el-option label="收" value="收"/>
                <el-option label="付" value="付"/>
                <el-option label="转" value="转"/>
              </el-select>
              <el-input-number style="width: 90px"
                               v-model="formData.wordNum"
                               :min="1"
                               :max="9999"
                               size="small"/>
              <span>号</span>
              <span class="voucher-page-indicator">{{ formatVoucherPageIndicator(sheetIndex) }}</span>
            </div>
            <span v-if="isReadonlyDisplay">
               {{ formatVoucherWordNum() }} {{ formatVoucherPageIndicator(sheetIndex) }}
            </span>
          </div>
        </div>
      </div>
      <!--  中间表格区域  -->
      <div class="voucher-sheet" @click.stop>
        <table v-if="isPrintMode" class="rv-table rv-print-table">
          <colgroup>
            <col class="rv-col-summary" style="width:20%"/>
            <template v-if="bookAuxiliaryEnabled">
              <col class="rv-col-subject" style="width:30%"/>
              <col class="rv-col-auxiliary" style="width:16%"/>
            </template>
            <col v-else class="rv-col-subject" style="width:44%"/>
            <col class="rv-col-amount" style="width:18%"/>
            <col class="rv-col-amount" style="width:18%"/>
          </colgroup>
          <thead>
            <template v-if="bookAuxiliaryEnabled">
              <tr class="rv-table-header-row">
                <th class="rv-table-header-cell rv-col-summary" rowspan="2">摘要</th>
                <th class="rv-table-header-cell" colspan="2">会计科目</th>
                <th class="rv-table-header-cell rv-col-amount" rowspan="2">借方金额</th>
                <th class="rv-table-header-cell rv-col-amount" rowspan="2">贷方金额</th>
              </tr>
              <tr class="rv-table-header-row">
                <th class="rv-table-header-cell rv-col-subject">科目</th>
                <th class="rv-table-header-cell rv-col-auxiliary">辅助核算</th>
              </tr>
            </template>
            <tr v-else class="rv-table-header-row">
              <th class="rv-table-header-cell rv-col-summary">摘要</th>
              <th class="rv-table-header-cell rv-col-subject">会计科目</th>
              <th class="rv-table-header-cell rv-col-amount">借方金额</th>
              <th class="rv-table-header-cell rv-col-amount">贷方金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in sheetData"
                :key="`${sheetIndex}-${rowIndex}-${row.id}-${row.summary}`"
                :class="tableRowClassName({ row })">
              <td class="rv-table-cell rv-col-summary">
                <template v-if="isGrandTotalRow(row)">
                  <span class="voucher-footer-label">{{ formatReceiptNumText() }}</span>
                </template>
                <span v-else-if="isTotalRow(row) || isCarryForwardRow(row)">{{ row.summary }}</span>
                <span v-else class="voucher-cell-text">{{ row.summary }}</span>
              </td>
              <template v-if="bookAuxiliaryEnabled">
                <td class="rv-table-cell rv-col-subject">
                  <span v-if="isGrandTotalRow(row)" class="voucher-grand-total-label">
                    <span class="voucher-grand-total-fixed">合计</span>
                    <span v-if="amountInChineseUpper" class="voucher-grand-total-amount">{{ amountInChineseUpper }}</span>
                  </span>
                  <span v-else-if="!isTotalRow(row)" class="voucher-cell-text">{{ getSubjectNameByRow(row) }}</span>
                </td>
                <td class="rv-table-cell rv-col-auxiliary">
                  <span v-if="!isTotalRow(row)" class="voucher-cell-text">{{ getSubjectDetailNameByRow(row) }}</span>
                </td>
              </template>
              <td v-else class="rv-table-cell rv-col-subject">
                <span v-if="isGrandTotalRow(row)" class="voucher-grand-total-label">
                  <span class="voucher-grand-total-fixed">合计</span>
                  <span v-if="amountInChineseUpper" class="voucher-grand-total-amount">{{ amountInChineseUpper }}</span>
                </span>
                <span v-else-if="!isTotalRow(row)" class="voucher-cell-text">{{ getSubjectNameByRow(row) }}</span>
              </td>
              <td class="rv-table-cell rv-col-amount">
                <span v-if="isTotalRow(row)" class="voucher-cell-amount-text">{{ formatAmount(row.debitAmount) }}</span>
                <span v-else class="voucher-cell-amount-text" :class="{ redWord: isRedWord(row.debitAmount) }">
                  {{ formatAmountRed(row.debitAmount) }}
                </span>
              </td>
              <td class="rv-table-cell rv-col-amount">
                <span v-if="isTotalRow(row)" class="voucher-cell-amount-text">{{ formatAmount(row.creditAmount) }}</span>
                <span v-else class="voucher-cell-amount-text" :class="{ redWord: isRedWord(row.creditAmount) }">
                  {{ formatAmountRed(row.creditAmount) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <el-table v-else :ref="sheetIndex === 0 ? 'rvTableRef' : undefined"
                    :data="sheetData"
                    @row-contextmenu="rowContextmenu"
                    @cell-click="cellClick"
                    @header-click="headerClick"
                    :row-class-name="tableRowClassName"
                    header-row-class-name="rv-table-header-row"
                    header-cell-class-name="rv-table-header-cell"
                    :cell-class-name="tableCellClassName"
                    class="rv-table">
            <el-table-column prop="summary" align="left" header-align="center" label="摘要"
                             class-name="rv-col-summary"
                             label-class-name="rv-col-summary"
                             :min-width="isPrintMode ? SUMMARY_COL_PRINT_WIDTH : 192"
                             :width="isPrintMode ? SUMMARY_COL_PRINT_WIDTH : undefined">
              <template #default="scope">
                <template v-if="isGrandTotalRow(scope.row)">
                  <div v-if="props.edit && !isPrintMode" class="no-border-input receipt-num-cell voucher-footer-label">
                    <span>附件 </span>
                    <el-input style="width: 40px" :input-style="{textAlign: 'center'}"
                              v-model="formData.receiptNum"/>
                    <span> 张</span>
                  </div>
                  <span v-else class="voucher-footer-label">{{ formatReceiptNumText() }}</span>
                </template>
                <span v-else-if="isTotalRow(scope.row)">{{ scope.row.summary }}</span>
                <el-input v-else-if="showCellInput(scope.row)"
                          type="textarea"
                          :rows="2"
                          resize="none"
                          class="voucher-cell-input"
                          :class="{ 'voucher-cell-readonly': !isCellEditable(scope.row) }"
                          v-model="scope.row.summary"
                          :readonly="!isCellEditable(scope.row)"
                          :input-style="voucherCellInputStyle"
                          @keydown="isCellEditable(scope.row) && handleInputKeydown($event, scope, 0)"
                          :ref="(el) => isCellEditable(scope.row) && setRef(el, `input-${scope.$index}-0`)"></el-input>
                <span v-else class="voucher-cell-text">{{ scope.row.summary }}</span>
              </template>
            </el-table-column>
            <template v-if="bookAuxiliaryEnabled">
              <el-table-column align="left" header-align="center" label="会计科目">
                <el-table-column prop="subjectId" align="left" header-align="center" label="科目"
                                 class-name="rv-col-subject"
                                 :min-width="isPrintMode ? SUBJECT_WITH_AUX_COL_PRINT_WIDTH : 238"
                                 :width="isPrintMode ? SUBJECT_WITH_AUX_COL_PRINT_WIDTH : undefined">
                  <template #default="scope">
                    <span v-if="isGrandTotalRow(scope.row)" class="voucher-grand-total-label">
                      <span class="voucher-grand-total-fixed">合计</span>
                      <span v-if="amountInChineseUpper" class="voucher-grand-total-amount">{{ amountInChineseUpper }}</span>
                    </span>
                    <el-cascader v-else-if="isCellEditable(scope.row)"
                                 class="voucher-cell-input voucher-cell-cascader"
                                 style="width: 100%" filterable clearable
                                 placeholder=""
                                 :show-all-levels="false"
                                 :model-value="resolveSubjectCascaderValue(scope.row)"
                                 :options="subjectList"
                                 :props="cascaderSubjectProps"
                                 @change="handleSubjectChange(scope, $event)"
                                 @clear="handleSubjectClear(scope)"
                                 @mousedown.capture="handleSubjectCascaderMouseDown"
                                 @keydown="handleCascaderKeydown($event, scope, 1)"
                                 :ref="(el) => setRef(el, `cascader-${scope.$index}-1`)"
                                 :filter-method="cascaderSubjectProps.filterMethod"/>
                    <el-input v-else-if="showCellInput(scope.row)"
                              type="textarea"
                              :rows="2"
                              resize="none"
                              readonly
                              class="voucher-cell-input voucher-cell-readonly"
                              :model-value="getSubjectName(scope)"
                              :input-style="voucherCellInputStyle"/>
                    <span v-else class="voucher-cell-text">{{ getSubjectName(scope) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="auxiliary" align="left" header-align="center" label="辅助核算"
                                 :min-width="isPrintMode ? AUX_COL_PRINT_WIDTH : 180"
                                 :width="isPrintMode ? AUX_COL_PRINT_WIDTH : undefined">
                  <template #default="scope">
                    <template v-if="!isTotalRow(scope.row)">
                      <el-popover v-if="isCellEditable(scope.row)" title="辅助核算" :width="400" trigger="click"
                                  :visible="auxiliaryVisible[scope.$index]">
                        <template #reference>
                          <el-input readonly
                                    type="textarea"
                                    :rows="2"
                                    resize="none"
                                    class="voucher-cell-input"
                                    :input-style="voucherCellInputStyle"
                                    :value="getSubjectDetailName(scope)"
                                    @click="handleAuxiliaryShow(scope)"
                                    @keydown="handleInputKeydown($event, scope, 2)"
                                    :ref="(el) => setRef(el, `input-${scope.$index}-2`)"></el-input>
                        </template>
                        <template #default>
                          <div>
                            <select-auxiliary
                                :show="auxiliaryVisible[scope.$index]"
                                :subjectId="scope.row.subjectId"
                                :auxiliary="subjectKeyItem[scope.row.subjectCode]?.auxiliary"
                                v-model="scope.row.auxiliary"></select-auxiliary>
                          </div>
                        </template>
                      </el-popover>
                      <el-input v-else-if="showCellInput(scope.row)"
                                type="textarea"
                                :rows="2"
                                resize="none"
                                readonly
                                class="voucher-cell-input voucher-cell-readonly"
                                :model-value="getSubjectDetailName(scope)"
                                :input-style="voucherCellInputStyle"/>
                      <span v-else class="voucher-cell-text">{{ getSubjectDetailName(scope) }}</span>
                    </template>
                  </template>
                </el-table-column>
              </el-table-column>
            </template>
            <el-table-column v-else prop="subjectId" align="left" header-align="center" label="会计科目"
                             class-name="rv-col-subject"
                             :min-width="isPrintMode ? SUBJECT_COL_PRINT_WIDTH : 238"
                             :width="isPrintMode ? SUBJECT_COL_PRINT_WIDTH : undefined">
              <template #default="scope">
                <span v-if="isGrandTotalRow(scope.row)" class="voucher-grand-total-label">
                  <span class="voucher-grand-total-fixed">合计</span>
                  <span v-if="amountInChineseUpper" class="voucher-grand-total-amount">{{ amountInChineseUpper }}</span>
                </span>
                <el-cascader v-else-if="isCellEditable(scope.row)"
                             class="voucher-cell-input voucher-cell-cascader"
                             style="width: 100%" filterable clearable
                             placeholder=""
                             :show-all-levels="false"
                             :model-value="resolveSubjectCascaderValue(scope.row)"
                             :options="subjectList"
                             :props="cascaderSubjectProps"
                             @change="handleSubjectChange(scope, $event)"
                             @clear="handleSubjectClear(scope)"
                             @mousedown.capture="handleSubjectCascaderMouseDown"
                             @keydown="handleCascaderKeydown($event, scope, 1)"
                             :ref="(el) => setRef(el, `cascader-${scope.$index}-1`)"
                             :filter-method="cascaderSubjectProps.filterMethod"/>
                <el-input v-else-if="showCellInput(scope.row)"
                          type="textarea"
                          :rows="2"
                          resize="none"
                          readonly
                          class="voucher-cell-input voucher-cell-readonly"
                          :model-value="getSubjectName(scope)"
                          :input-style="voucherCellInputStyle"/>
                <span v-else class="voucher-cell-text">{{ getSubjectName(scope) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="debitAmount" align="right" header-align="center" label="借方金额"
                             class-name="rv-col-amount"
                             :min-width="isPrintMode ? AMOUNT_COL_PRINT_WIDTH : 130"
                             :width="isPrintMode ? AMOUNT_COL_PRINT_WIDTH : undefined">
              <template #default="scope">
                <span v-if="isTotalRow(scope.row)" class="voucher-cell-amount-text">{{ formatAmount(scope.row.debitAmount) }}</span>
                <el-input v-else-if="showCellInput(scope.row)"
                          class="voucher-cell-input voucher-cell-amount"
                          :class="{redWord: isRedWord(scope.row.debitAmount), 'voucher-cell-readonly': !isCellEditable(scope.row)}"
                          v-model="scope.row.debitAmount"
                          :readonly="!isCellEditable(scope.row)"
                          :input-style="voucherCellInputStyle"
                          :formatter="amountInputFormatter"
                          :parser="amountInputParser"
                          @input="isCellEditable(scope.row) && recalculateTotals()"
                          @change="isCellEditable(scope.row) && createTableData(scope.row, 1)"
                          @keydown="isCellEditable(scope.row) && handleInputKeydown($event, scope, bookAuxiliaryEnabled ? 3 : 2)"
                          :ref="(el) => isCellEditable(scope.row) && setRef(el, `input-${scope.$index}-${bookAuxiliaryEnabled ? 3 : 2}`)"></el-input>
                <span v-else class="voucher-cell-amount-text" :class="{redWord:isRedWord(scope.row.debitAmount)}">
                  {{ formatAmountRed(scope.row.debitAmount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="creditAmount" align="right" header-align="center" label="贷方金额"
                             class-name="rv-col-amount"
                             :min-width="isPrintMode ? AMOUNT_COL_PRINT_WIDTH : 130"
                             :width="isPrintMode ? AMOUNT_COL_PRINT_WIDTH : undefined">
              <template #default="scope">
                <span v-if="isTotalRow(scope.row)" class="voucher-cell-amount-text">{{ formatAmount(scope.row.creditAmount) }}</span>
                <el-input v-else-if="showCellInput(scope.row)"
                          class="voucher-cell-input voucher-cell-amount"
                          :class="{redWord: isRedWord(scope.row.creditAmount), 'voucher-cell-readonly': !isCellEditable(scope.row)}"
                          v-model="scope.row.creditAmount"
                          :readonly="!isCellEditable(scope.row)"
                          :input-style="voucherCellInputStyle"
                          :formatter="amountInputFormatter"
                          :parser="amountInputParser"
                          @input="isCellEditable(scope.row) && recalculateTotals()"
                          @change="isCellEditable(scope.row) && createTableData(scope.row, 2)"
                          @keydown="isCellEditable(scope.row) && handleInputKeydown($event, scope, bookAuxiliaryEnabled ? 4 : 3)"
                          :ref="(el) => isCellEditable(scope.row) && setRef(el, `input-${scope.$index}-${bookAuxiliaryEnabled ? 4 : 3}`)"></el-input>
                <span v-else class="voucher-cell-amount-text" :class="{redWord:isRedWord(scope.row.creditAmount)}">
                  {{ formatAmountRed(scope.row.creditAmount) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
      </div>
      <!--   审核人信息   -->
      <div class="apply-info">
        <div class="apply-info-item">
          <span>会计主管：</span>
          <span>{{ formData.managerName }}</span>
        </div>
        <div class="apply-info-item">
          <span>过账：</span>
          <span>{{ formData.senderName }}</span>
        </div>
        <div class="apply-info-item">
          <span>复核：</span>
          <span>{{ formData.auditMemberName }}</span>
        </div>
        <div class="apply-info-item">
          <span>制单：</span>
          <span>{{ formData.createdName }}</span>
        </div>
      </div>
      </div>
    </div>
    <!-- 右键功能区域 -->
    <div v-if="visibleContextmenu && !isPrintMode" class="contextmenu"
         :style="{ left: leftMenu + 'px', top: topMenu + 'px' }">
      <el-button :disabled="currentRow.cfg_index === 0" plain @click="handleMoveUp">向上移动</el-button>
      <el-button :disabled="currentRow.cfg_index === formData.items.length - 1" plain @click="handleMoveDown">向下移动
      </el-button>
      <el-button :disabled="currentRow.cfg_index === 0" plain @click="handleMoveToStart">移至首位</el-button>
      <el-button :disabled="currentRow.cfg_index === formData.items.length - 1" plain @click="handleMoveToEnd">移至末尾
      </el-button>
      <el-button plain @click="handleDelete">删除本项</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, nextTick, onBeforeUpdate, onMounted, reactive, ref, watch} from 'vue'
import SelectAuxiliary from "./SelectAuxiliary/index.vue"
import {ElLoading, ElMessage, ElMessageBox, ElSelect, TableColumnCtx} from 'element-plus'
import {parseTime} from "@/utils/Jinbooks";
import * as subjectApi from "@/api/standard/standard-subject"
import {draftVoucher, getOneVoucher, getVoucherAbleWordNum, submitVoucher} from "@/api/voucher/voucher";
import {validateForm} from "@/utils"
import {useRoute} from "vue-router";
import bookStore from "@/store/modules/bookStore";
import Decimal from 'decimal.js'
import {getSubjectDisplayName, cascaderSubjectProps as baseCascaderSubjectProps} from "@/utils/Subjects";

interface RecordingVoucher {
  id: any,
  summary: string,
  subjectId: string,
  detailedSubjectCode: string,
  detailedAccounts: string,
  debitAmount: number | string,
  creditAmount: number | string,
  subjectBalance: number | undefined,
  auxiliary: Array<any>,
  editing: boolean,
  columnIndex: number,
}

const currBookStore = bookStore()
let voucherDate = parseTime(new Date(), "{y}-{m}") === currBookStore.termCurrent
    ? parseTime(new Date(), "{y}-{m}-{d}")
    : currBookStore.termCurrent ? currBookStore.termCurrent + "-01" : parseTime(new Date(), "{y}-{m}-{d}")
const props: any = defineProps({
  // 自动提交，内部直接调用接口提交
  auto: {
    type: Boolean,
    default: true
  },
  dialog: {
    type: Boolean,
    default: false
  },
  edit: {
    type: Boolean,
    default: true
  },
  // 数据
  modelValue: {
    type: Object,
    default: {
      id: null,
      word: null,
      bookId: null,
      wordHead: '记',
      voucherWordNumber: '',
      wordNum: null,
      companyId: null,
      companyName: null,
      remark: '',
      receiptNum: 0,
      debitAmount: null,
      creditAmount: null,
      voucherYear: null,
      voucherMonth: null,
      voucherDate: null,
      carryForward: null,
      auditMemberId: null,
      auditMemberName: null,
      auditDate: null,
      status: null,
      items: []
    },
  }
})

const auxiliaryVisible = ref<Array<boolean>>([])
const route: any = useRoute()
// 定义回调接口，提交按钮触发
const emit: any = defineEmits(['submit', "update:modelValue"])
const visibleContextmenu = ref(false)
const submitButtonLoading = ref(false)
const leftMenu = ref(0)
const topMenu = ref(0)
const currentRow = ref<any>(null)
const printMe = ref(null)
const printing = ref(false)
const printSheets = ref<RecordingVoucher[][]>([])
// 会计科目数据
const subjectList = ref<any>([])
const subjectKeyItem = ref<any>({})
const subjectKeyIdItem = ref<any>({})
const bookAuxiliaryEnabled = computed(() => currBookStore.assistAccEnabled)
const lastInputColumnIndex = computed(() => bookAuxiliaryEnabled.value ? 4 : 3)
const DEFAULT_ENTRY_ROWS = 6

const isTotalRow = (row: RecordingVoucher) => row.id === -1

const isGrandTotalRow = (row: RecordingVoucher) => {
  return isTotalRow(row) && String(row.summary).includes('合')
}

const isCarryForwardRow = (row: RecordingVoucher) => row.id === -2

const isCellEditable = (row: RecordingVoucher) => {
  return props.edit && !isPrintMode.value && !isTotalRow(row) && row.id > 0
}

const showCellInput = (row: RecordingVoucher) => {
  return !isPrintMode.value && !isTotalRow(row) && !isCarryForwardRow(row) && row.id > 0
}

const tableRowClassName = ({row}: { row: RecordingVoucher }) => {
  if (isTotalRow(row)) {
    return 'rv-table-count-row'
  }
  if (isCarryForwardRow(row)) {
    return 'rv-table-carry-row'
  }
  return 'rv-table-entry-row'
}
const shortcuts = [
  {
    text: '当天',
    value: parseTime(new Date(), '{y}-{m}-{d}'),
  },
  {
    text: '当期第一天',
    value: () => {
      return currBookStore.termCurrent + '-01'
    },
  },
  {
    text: '当期最后一天',
    value: () => {
      const parts = currBookStore.termCurrent.split('-')
      const year = parseInt(parts[0], 10)
      const month = parseInt(parts[1], 10)
      const date = new Date(year, month, 0) // 下月的第0天 = 本月最后一天
      return parseTime(date, '{y}-{m}-{d}')
    },
  },
  {
    text: '上一个工作日',
    value: () => {
      const date = new Date()
      const day = date.getDay()
      if (day === 1) {
        // 周一 -> 周五
        date.setDate(date.getDate() - 3)
      } else if (day === 0) {
        // 周日 -> 周五
        date.setDate(date.getDate() - 2)
      } else {
        // 其他工作日 -> 前一天
        date.setDate(date.getDate() - 1)
      }
      return parseTime(date, '{y}-{m}-{d}')
    },
  },
  {
    text: '本月第一天',
    value: () => {
      const date = new Date()
      date.setDate(1)
      return parseTime(date, '{y}-{m}-{d}')
    },
  },
  {
    text: '本月最后一天',
    value: () => {
      const date = new Date()
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      const lastDay = new Date(year, month, 0) // 下月0号 = 本月最后一天
      return parseTime(lastDay, '{y}-{m}-{d}')
    },
  },
  {
    text: '上月第一天',
    value: () => {
      const date = new Date()
      date.setMonth(date.getMonth() - 1)
      date.setDate(1)
      return parseTime(date, '{y}-{m}-{d}')
    },
  },
  {
    text: '上月最后一天',
    value: () => {
      const date = new Date()
      date.setDate(0) // 当前月的第0天即上月最后一天
      return parseTime(date, '{y}-{m}-{d}')
    },
  },
  {
    text: '本季度第一天',
    value: () => {
      const date = new Date()
      const currentMonth = date.getMonth()
      const quarterStartMonth = currentMonth - (currentMonth % 3)
      date.setMonth(quarterStartMonth)
      date.setDate(1)
      return parseTime(date, '{y}-{m}-{d}')
    },
  },
  {
    text: '今年第一天',
    value: () => {
      const date = new Date()
      date.setMonth(0)
      date.setDate(1)
      return parseTime(date, '{y}-{m}-{d}')
    },
  }
]

// 凭证明细数据-列表展示
const tableSumData = ref<RecordingVoucher[]>([]);
const rvTableRef = ref()

const tableSheets = computed(() => {
  if (printing.value && printSheets.value.length > 0) {
    return printSheets.value
  }
  return [tableSumData.value]
})

const countRow = ref<RecordingVoucher>({
  columnIndex: 0,
  editing: false,
  id: -1,
  summary: `合  计`,
  subjectId: '',
  detailedSubjectCode: "",
  detailedAccounts: '',
  debitAmount: 0,
  creditAmount: 0,
  subjectBalance: undefined,
  auxiliary: []
});
const formData = ref<any>({...props.modelValue})
const isPrintMode = computed(() => {
  return route.query.mode === 'print' || printing.value
})
const isReadonlyDisplay = computed(() => {
  return !props.edit || isPrintMode.value
})
const SUMMARY_COL_PRINT_WIDTH = 180
const SUBJECT_COL_PRINT_WIDTH = 360
const SUBJECT_WITH_AUX_COL_PRINT_WIDTH = 260
const AUX_COL_PRINT_WIDTH = 140
const AMOUNT_COL_PRINT_WIDTH = 120
const printContentStyle = computed(() => ({
  margin: !isPrintMode.value ? '65px 0 0 0' : '0',
  width: '100%',
}))
const cascaderSubjectProps = {
  ...baseCascaderSubjectProps,
  expandTrigger: 'click',
  label: 'cascaderLabel',
  showAllLevels: false,
  clearable: true,
}
const voucherCellInputStyle = {
  boxShadow: 'none',
  border: 'none',
  background: 'transparent',
  padding: '0 4px',
}
// 创建一个响应式的对象来存储 refs
const refs = reactive<{ [key: string]: any }>({});

// 创建一个响应式的对象来存储 refs
const setRef = (el: any, key: string) => {
  if (el) {
    refs[key] = el;
    // 当元素被设置时，如果它应该被聚焦，则聚焦它
    if (shouldFocusRef(key)) {
      setTimeout(() => {
        focusRefElement(el);
      }, 0);
    }
  }
};
// 存储应该聚焦的元素的 key
const focusedRefKey = ref('');
// 判断是否应该聚焦某个 ref 元素
const shouldFocusRef = (key: string) => {
  return key === focusedRefKey.value;
};
// 聚焦到 ref 元素
const focusRefElement = (el: any) => {
  if (!el) {
    return
  }
  if (typeof el.focus === 'function') {
    el.focus()
    return
  }
  const root = el.$el || el
  const input = root.querySelector?.('input:not([type="hidden"]), textarea')
  if (input) {
    input.focus()
    if (typeof input.select === 'function' && input.type !== 'textarea') {
      input.select()
    }
    return
  }
  if (el.togglePopperVisible) {
    el.togglePopperVisible(true)
    root.querySelector?.('input')?.focus()
  }
}

const focusCell = (rowIndex: number, columnIndex: number) => {
  focusedRefKey.value = getColumnRefKey(rowIndex, columnIndex)
  nextTick(() => {
    const ref = refs[focusedRefKey.value]
    if (ref) {
      focusRefElement(ref)
    }
  })
}

// 新增一项
const onAddItem = () => {
  formData.value.items.push({
    id: new Date().getTime(),
    summary: '',
    subjectId: '',
    subjectName: '',
    subjectCode: '',
    detailedAccounts: '',
    debitAmount: "",
    creditAmount: "",
    subjectBalance: undefined,
    auxiliary: [],
  })
  createTableData()
}

const parseAmountValue = (value: any) => {
  if (value === '' || value === null || value === undefined) {
    return new Decimal(0)
  }
  try {
    const num = String(value).replace(/,/g, '')
    if (!num || num === '.' || num === '-') {
      return new Decimal(0)
    }
    return new Decimal(num)
  } catch {
    return new Decimal(0)
  }
}

/**
 * 重算借贷合计
 */
const recalculateTotals = (row?: RecordingVoucher, direction?: number) => {
  if (row && direction === 1) {
    row.creditAmount = ""
  } else if (row && direction === 2) {
    row.debitAmount = ""
  }
  const debitAmount = formData.value.items.reduce((total: Decimal, item: any) => {
    return total.plus(parseAmountValue(item.debitAmount))
  }, new Decimal(0))
  const creditAmount = formData.value.items.reduce((total: Decimal, item: any) => {
    return total.plus(parseAmountValue(item.creditAmount))
  }, new Decimal(0))
  countRow.value.debitAmount = debitAmount
  countRow.value.creditAmount = creditAmount
  formData.value.debitAmount = debitAmount
  formData.value.creditAmount = creditAmount
  const totalRow = tableSumData.value.find((item) => item.id === -1)
  if (totalRow && totalRow !== countRow.value) {
    totalRow.debitAmount = debitAmount
    totalRow.creditAmount = creditAmount
  }
}

/**
 * 刷新table，建立汇总行
 */
const createTableData = (row?: RecordingVoucher, direction?: any) => {
  auxiliaryVisible.value.length = 0
  const data = formData.value.items.map((t: any) => {
    auxiliaryVisible.value.push(false)
    return t
  })
  // 初始数据量不够则用空行填充
  if (data.length < DEFAULT_ENTRY_ROWS) {
    const maxRow = DEFAULT_ENTRY_ROWS - data.length
    for (let i = 0; i < maxRow; i++) {
      data.push({
        id: 0,
        summary: '',
        subjectId: '',
        subjectName: '',
        subjectCode: '',
        detailedAccounts: '',
        debitAmount: '',
        creditAmount: '',
        subjectBalance: undefined,
        auxiliary: [],
      })
    }
  }
  recalculateTotals(row, direction)
  data.push(countRow.value)
  tableSumData.value = data
}

const createEmptyEntryRow = (): RecordingVoucher => ({
  id: 0,
  summary: '',
  subjectId: '',
  subjectName: '',
  subjectCode: '',
  detailedAccounts: '',
  debitAmount: '',
  creditAmount: '',
  subjectBalance: undefined,
  auxiliary: [],
})

const sumEntryAmounts = (rows: RecordingVoucher[]) => {
  let debit = new Decimal(0)
  let credit = new Decimal(0)
  for (const row of rows) {
    if (row.id <= 0) {
      continue
    }
    if (row.debitAmount !== '' && row.debitAmount !== undefined && row.debitAmount !== null) {
      debit = debit.plus(row.debitAmount)
    }
    if (row.creditAmount !== '' && row.creditAmount !== undefined && row.creditAmount !== null) {
      credit = credit.plus(row.creditAmount)
    }
  }
  return {debit, credit}
}

const createPrintFooterRow = (summary: string, debit: Decimal, credit: Decimal): RecordingVoucher => ({
  columnIndex: 0,
  editing: false,
  id: -1,
  summary,
  subjectId: '',
  detailedSubjectCode: '',
  detailedAccounts: '',
  debitAmount: debit.toNumber(),
  creditAmount: credit.toNumber(),
  subjectBalance: undefined,
  auxiliary: [],
})

const createCarryForwardRow = (debit: Decimal, credit: Decimal): RecordingVoucher => ({
  id: -2,
  summary: '承上页',
  subjectId: '',
  subjectCode: '',
  detailedAccounts: '',
  debitAmount: debit.toNumber(),
  creditAmount: credit.toNumber(),
  subjectBalance: undefined,
  auxiliary: [],
})

/**
 * 分录超过 6 行时拆分为多张凭证页，非末页「过次页」，续页首行「承上页」，末页「合计」
 */
const buildPrintSheets = (items: RecordingVoucher[]): RecordingVoucher[][] => {
  if (items.length <= DEFAULT_ENTRY_ROWS) {
    return []
  }

  const sheets: RecordingVoucher[][] = []
  let index = 0
  let previousPageDebit = new Decimal(0)
  let previousPageCredit = new Decimal(0)
  const grandTotal = sumEntryAmounts(items)

  while (index < items.length) {
    const pageEntries: RecordingVoucher[] = []
    const isFirstPage = sheets.length === 0

    if (!isFirstPage) {
      pageEntries.push(createCarryForwardRow(previousPageDebit, previousPageCredit))
    }

    const maxEntries = isFirstPage ? DEFAULT_ENTRY_ROWS : DEFAULT_ENTRY_ROWS - 1
    let added = 0
    while (added < maxEntries && index < items.length) {
      pageEntries.push({...items[index]})
      index++
      added++
    }

    while (pageEntries.length < DEFAULT_ENTRY_ROWS) {
      pageEntries.push(createEmptyEntryRow())
    }

    const pageSum = sumEntryAmounts(pageEntries)
    previousPageDebit = pageSum.debit
    previousPageCredit = pageSum.credit

    const isLastPage = index >= items.length
    const footerRow = isLastPage
        ? createPrintFooterRow(countRow.value.summary, grandTotal.debit, grandTotal.credit)
        : createPrintFooterRow('过次页', pageSum.debit, pageSum.credit)

    sheets.push([...pageEntries, footerRow])
  }

  return sheets
}

const getValidVoucherEntryItems = () => {
  return pruneOrphanVoucherItems(formData.value.items.filter((item: any) =>
      item.subjectId || item.subjectCode || item.creditAmount || item.debitAmount
  ))
}

const voucherPageTotal = computed(() => {
  if (printing.value && printSheets.value.length > 0) {
    return printSheets.value.length
  }
  const sheets = buildPrintSheets(getValidVoucherEntryItems())
  return sheets.length > 0 ? sheets.length : 1
})

const formatVoucherPageIndicator = (sheetIndex: number) => {
  return `${sheetIndex + 1}/${voucherPageTotal.value}`
}

/**
 * 金额转中文大写（如 肆万叁仟陆佰元整 / 肆万叁仟陆佰元伍角陆分）
 */
const convertToChineseUppercase = (num: number): string => {
  const CN_NUM = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const CN_DIGIT_UNIT = ['', '拾', '佰', '仟']
  const CN_SECTION_UNIT = ['', '万', '亿', '兆', '京']
  const CN_DECIMAL_UNIT = ['角', '分']

  if (num < 0) {
    return '负' + convertToChineseUppercase(-num)
  }
  if (num === 0) {
    return '零元整'
  }

  const [integerNum, decimalNum = '00'] = num.toFixed(2).split('.')

  let integerStr = ''
  if (parseInt(integerNum, 10) > 0) {
    let zeroCount = 0
    const intLen = integerNum.length
    for (let i = 0; i < intLen; i++) {
      const digitChar = integerNum.charAt(i)
      const position = intLen - i - 1
      const sectionIndex = Math.floor(position / 4)
      const digitIndex = position % 4
      if (digitChar === '0') {
        zeroCount++
      } else {
        if (zeroCount > 0) {
          integerStr += CN_NUM[0]
        }
        zeroCount = 0
        integerStr += CN_NUM[parseInt(digitChar, 10)] + CN_DIGIT_UNIT[digitIndex]
      }
      if (digitIndex === 0 && zeroCount < 4) {
        integerStr += CN_SECTION_UNIT[sectionIndex]
      }
    }
  } else {
    integerStr = CN_NUM[0]
  }

  integerStr = integerStr.replace(/零+/g, '零').replace(/零$/, '')

  if (parseInt(decimalNum, 10) === 0) {
    return `${integerStr}元整`
  }

  let decimalStr = ''
  const jiao = parseInt(decimalNum.charAt(0), 10)
  const fen = parseInt(decimalNum.charAt(1), 10)
  if (jiao > 0) {
    decimalStr += CN_NUM[jiao] + CN_DECIMAL_UNIT[0]
  } else if (fen > 0) {
    decimalStr += CN_NUM[0]
  }
  if (fen > 0) {
    decimalStr += CN_NUM[fen] + CN_DECIMAL_UNIT[1]
  }
  return `${integerStr}元${decimalStr}`
}

const amountInChineseUpper = computed(() => {
  const amount = countRow.value?.debitAmount
  if (amount === '' || amount === undefined || amount === null) {
    return ''
  }
  try {
    const decimal = new Decimal(amount)
    if (decimal.isZero()) {
      return '零元整'
    }
    return convertToChineseUppercase(Number(decimal.toFixed(2)))
  } catch {
    return ''
  }
})

const formatReceiptNumText = () => {
  const num = formData.value.receiptNum ?? 0
  return `附件 ${num} 张`
}

/**
 * 格式化金额到千分位
 * @param value
 */
const formatAmount = (value: any) => {
  if (value === '' || value === null || value === undefined) return '';
  const num = String(value).replace(/,/g, '')
  if (!num || isNaN(Number(num))) return '';
  const parts = num.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const formatted = parts.length > 1 ? parts.join('.') : parts[0]
  return formatted
}

const amountInputFormatter = (value: string) => {
  if (!value && value !== '0') return ''
  const num = String(value).replace(/,/g, '')
  if (!num) return ''
  const parts = num.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.length > 1 ? parts.join('.') : parts[0]
}

const amountInputParser = (value: string) => value.replace(/,/g, '').replace(/[^\d.-]/g, '')

const formatAmountRed = (value: any) => {
  if (!value && value !== 0) return '';
  if (new Decimal(value).lt(0)) {
    value = new Decimal(0).minus(new Decimal(value))
  }
  return formatAmount(value)
}

const isRedWord = (value: any) => {
  if (!value || value == "") {
    return false;
  }
  return new Decimal(value).lt(0);
}

function tableCellClassName({row, column, rowIndex, columnIndex}: any) {
  row.index = rowIndex;
  column.index = columnIndex;
  return isTotalRow(row) ? "rv-table-cell rv-table-count-cell" : "rv-table-cell"
}

// 行点击，聚焦对应单元格
const cellClick = (row: any, column: any, _cell: HTMLTableCellElement, event: Event) => {
  if (route.query.mode === 'print' || !props.edit || isTotalRow(row)) {
    return
  }
  if (row.id === 0) {
    onAddItem()
    nextTick(() => {
      focusCell(tableSumData.value.length - 2, column.index ?? 0)
    })
    event.stopPropagation()
    return
  }
  row.columnIndex = column.index
  focusCell(tableSumData.value.indexOf(row), column.index ?? 0)
  event.stopPropagation()
}

const headerClick = (_column: any, _event: Event) => {
  closeOverlayOnly()
}

const closeOverlayOnly = () => {
  visibleContextmenu.value = false
  auxiliaryVisible.value = auxiliaryVisible.value.map(() => false)
  Object.keys(refs).forEach((key) => {
    if (key.startsWith('cascader-') && refs[key]?.togglePopperVisible) {
      refs[key].togglePopperVisible(false)
    }
  })
}

const closeEditAll = () => {
  closeOverlayOnly()
}

// 右键
const rowContextmenu = (row: any, column: any, event: Event) => {
  if (row.id > 0) {
    leftMenu.value = event?.clientX - 20
    topMenu.value = event?.clientY
    currentRow.value = JSON.parse(JSON.stringify(row))
    currentRow.value.cfg_index = tableSumData.value.indexOf(row);
    visibleContextmenu.value = true
    event.preventDefault();
  }
}
const moveItemInArray = (arr: any, fromIndex: any, toIndex: any) => {
  const item = arr.splice(fromIndex, 1)[0];
  arr.splice(toIndex, 0, item);
}
const handleMoveUp = () => {
  if (currentRow.value.cfg_index > 0) {
    moveItemInArray(formData.value.items, currentRow.value.cfg_index, currentRow.value.cfg_index - 1);
    createTableData();
  }
  visibleContextmenu.value = false;
};

const handleMoveDown = () => {
  if (currentRow.value.cfg_index < formData.value.items.length - 1) {
    moveItemInArray(formData.value.items, currentRow.value.cfg_index, currentRow.value.cfg_index + 1);
    createTableData();
  }
  visibleContextmenu.value = false;
};

const handleMoveToStart = () => {
  if (currentRow.value.cfg_index !== 0) {
    moveItemInArray(formData.value.items, currentRow.value.cfg_index, 0);
    createTableData();
  }
  visibleContextmenu.value = false;
};

const handleMoveToEnd = () => {
  if (currentRow.value.cfg_index !== formData.value.items.length - 1) {
    moveItemInArray(formData.value.items, currentRow.value.cfg_index, formData.value.items.length);
    createTableData();
  }
  visibleContextmenu.value = false;
};
const handleDelete = () => {
  const idx = formData.value.items.findIndex((item: any) => item.id === currentRow.value.id)
  if (idx >= 0) {
    formData.value.items.splice(idx, 1)
  }
  visibleContextmenu.value = false
  createTableData()
}

function isCurrentOrFutureMonth(date: Date) {
  const now = new Date(currBookStore.termCurrent + "-01")
  return parseTime(date, "{y}-{m}-{d}") < parseTime(now, "{y}-{m}-{d}")
}

function buildPrintFrameHtml(contentHtml: string) {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8"/>
  <title>凭证打印</title>
  <style>
    @page { size: A4 landscape; margin: 8mm 10mm; }
    * { box-sizing: border-box; }
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      background: #fff;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .voucher-print-sheet {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: stretch;
      width: 100%;
      min-height: 100vh;
      height: 100vh;
      padding: 0 2mm;
      page-break-inside: avoid;
      break-inside: avoid-page;
      page-break-after: always;
      break-after: page;
    }
    .voucher-print-sheet:last-child {
      page-break-after: auto;
      break-after: auto;
    }
    .header-title { margin-bottom: 4px; text-align: center; }
    .header-title-text {
      display: inline-block;
      font-size: 28px;
      font-weight: bold;
      color: #983400;
      border-bottom: 3px double #800100;
    }
    .header-title-time {
      display: block;
      padding-top: 6px;
      color: #983400;
      font-size: 16px;
    }
    .company-info {
      margin: 0 0 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: #983400;
      font-size: 16px;
    }
    .company-info-right {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }
    .company-info-item { white-space: nowrap; }
    .apply-info {
      margin: 8px 20px 0;
      display: flex;
      justify-content: space-between;
      color: #983400;
      font-size: 16px;
    }
    .rv-print-table {
      width: 100%;
      border-collapse: collapse;
      table-layout: fixed;
      border: 1px solid #800100;
      font-size: 16px;
    }
    .rv-print-table th,
    .rv-print-table td {
      border: 1px dashed #800100;
      color: #983400;
      padding: 2px 4px;
      font-weight: normal;
      vertical-align: middle;
      word-break: break-word;
      white-space: normal;
      line-height: 1.35;
    }
    .rv-print-table thead th {
      height: 30px;
      text-align: center;
    }
    .rv-print-table tbody .rv-table-entry-row td,
    .rv-print-table tbody .rv-table-carry-row td {
      height: 42px;
      font-size: 14px;
    }
    .rv-print-table tbody .rv-table-count-row td {
      height: 32px;
    }
    .rv-print-table tbody .rv-table-count-row .rv-col-summary,
    .rv-print-table tbody .rv-table-carry-row .rv-col-summary {
      text-align: center;
    }
    .rv-print-table tr > :first-child { border-left: none; }
    .rv-print-table tr > :last-child { border-right: none; }
    .rv-print-table thead tr:first-child th { border-top: none; }
    .rv-print-table tbody tr:last-child td { border-bottom: none; }
    .rv-print-table thead th.rv-col-amount { text-align: center; }
    .rv-print-table tbody td.rv-col-amount { text-align: right; }
    .voucher-grand-total-label {
      display: flex;
      align-items: flex-start;
      width: 100%;
      color: #983400;
      font-size: var(--el-font-size-large);
      line-height: 1.4;
      text-align: left;
      padding-left: 0.5em;
      gap: 0.15em;
    }
    .voucher-grand-total-fixed {
      flex: 0 0 auto;
      text-align: left;
    }
    .voucher-grand-total-amount {
      flex: 1 1 auto;
      text-align: left;
      word-break: break-word;
    }
    .voucher-footer-label {
      display: block;
      width: 100%;
      text-align: center;
      color: #983400;
      font-size: var(--el-font-size-large);
      line-height: 1.4;
    }
    .voucher-cell-amount-text {
      display: block;
      width: 100%;
      text-align: right;
      padding-right: 6px;
      line-height: 1.4;
      white-space: nowrap;
    }
    .amount-chinese-text { font-size: 14px; line-height: 1.3; }
    .voucher-cell-text {
      display: block;
      word-break: break-word;
      line-height: 1.35;
    }
    .redWord { color: red; }
    @media print {
      .voucher-print-sheet {
        min-height: 194mm;
        height: 194mm;
      }
    }
  </style>
</head>
<body>${contentHtml}</body>
</html>`
}

function printContentInIframe(onDone: () => void) {
  const source = printMe.value as HTMLElement | null
  if (!source) {
    window.print()
    onDone()
    return
  }

  const iframe = document.createElement('iframe')
  iframe.setAttribute('style', 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;')
  document.body.appendChild(iframe)

  const win = iframe.contentWindow
  const doc = iframe.contentDocument
  if (!win || !doc) {
    document.body.removeChild(iframe)
    window.print()
    onDone()
    return
  }

  doc.open()
  doc.write(buildPrintFrameHtml(source.innerHTML))
  doc.close()

  let finished = false
  const finish = () => {
    if (finished) {
      return
    }
    finished = true
    if (iframe.parentNode) {
      document.body.removeChild(iframe)
    }
    onDone()
  }

  win.onafterprint = finish
  setTimeout(finish, 120000)

  setTimeout(() => {
    win.focus()
    win.print()
  }, 200)
}

// 打印
const onPrint = async () => {
  closeEditAll()
  const backupItems = [...formData.value.items]
  const validItems = pruneOrphanVoucherItems(formData.value.items.filter((item: any) =>
      item.subjectId || item.subjectCode || item.creditAmount || item.debitAmount
  ))
  formData.value.items = validItems
  recalculateTotals()
  printSheets.value = buildPrintSheets(validItems)
  if (printSheets.value.length === 0) {
    createTableData()
  }

  printing.value = true
  await nextTick()
  await nextTick()

  const originalTitle = document.title
  document.title = `凭证打印-${formatVoucherWordNum()}`

  let restored = false
  const restore = () => {
    if (restored) {
      return
    }
    restored = true
    printing.value = false
    printSheets.value = []
    document.title = originalTitle
    formData.value.items = backupItems
    createTableData()
  }

  printContentInIframe(restore)
}

function printSpecificDiv(printcontent: any) {
  const restorepage = document.body.innerHTML; // 保存当前页面的HTML结构
  document.body.innerHTML = printcontent   // 将body的内容替换为要打印的div内容
  window.print(); // 打印当前页面（即现在的body内容）
  document.body.innerHTML = restorepage; // 恢复页面内容
  window.close();
}

function resetList() {
  formData.value = {...props.modelValue}
  formData.value.id = null
  formData.value.bookId = currBookStore.bookId
  formData.value.companyName = currBookStore.getBookItem().companyName
  formData.value.items = []
  // 获取可用凭证子号
  const now = new Date(voucherDate)
  getVoucherAbleWordNum(formData.value.wordHead, parseTime(now, "{y}"), parseTime(now, "{m}")
  ).then((res: any) => {
    formData.value.wordNum = res.data
  })
  createTableData()
}

const normalizeSubjectLeafName = (item: any) => {
  if (!item) {
    return ''
  }
  let label = getSubjectDisplayName(item)
  if (label.includes('/')) {
    label = label.split('/').pop()?.trim() || label
  }
  if (label.includes('／')) {
    label = label.split('／').pop()?.trim() || label
  }
  const dashIndex = label.indexOf('-')
  if (dashIndex > -1 && /^\d/.test(label)) {
    label = label.slice(dashIndex + 1)
  }
  return label.trim()
}

const decorateSubjectTree = (items: any[]) => {
  items.forEach((item) => {
    const leafName = normalizeSubjectLeafName(item)
    item.leafName = leafName
    item.cascaderLabel = item.code && leafName ? `${item.code} ${leafName}` : (item.code || leafName || '')
    if (item.children?.length) {
      decorateSubjectTree(item.children)
    }
  })
}

// 更新会计科目ID关联
const updateSubjectKeys = (items: any) => {
  decorateSubjectTree(items)
  for (let valueKey in items) {
    const item = items[valueKey]
    item.auxiliary = item.auxiliary && item.auxiliary.startsWith("[") ? JSON.parse(item.auxiliary) : []
    subjectKeyItem.value[item.code] = item
    subjectKeyIdItem.value[item.id] = item
    if (item.children && item.children.length > 0) {
      updateSubjectKeys(item.children)
    }
  }
}

const onSubmitDraft = () => {
  if (submitButtonLoading.value) {
    return
  }
  closeOverlayOnly()
  const preparedItems = prepareVoucherItemsForSave(formData.value.items)
  if (!preparedItems) {
    return
  }

  submitButtonLoading.value = true
  draftVoucher({
    ...formData.value,
    items: preparedItems,
  }).then((res: any) => {
    ElMessage.info(res.message || `暂存成功`)
    getOneVoucher(res.data).then((voucherRes: any) => {
      applyLoadedVoucherData(voucherRes.data)
    })
  }).finally(() => {
    submitButtonLoading.value = false
  })
}

const onReset = () => {
  let bl = false
  formData.value.items.forEach((item: any, index: number) => {
    if (item.subjectId && !formData.value.id) {
      bl = true
    }
  })
  if (bl) {
    ElMessageBox.confirm(
        '尚未保存当前已录入凭证，确认要清空吗？',
        '系统提示',
        {confirmButtonText: '清空并重新录入', cancelButtonText: '继续录入', type: 'warning'}
    )
        .then(() => {
          resetList()
        })
        .catch(() => {
        });
  } else {
    resetList()
  }
}

// 提交数据
const onSubmit = () => {
  if (submitButtonLoading.value) {
    return false
  }
  if (!formData.value.id) {
    ElMessage.error(`请先暂存后提交`)
    return false;
  }

  validateForm(formData.value, {
    bookId: [
      {required: true, message: '所属账套不能为空', trigger: 'blur'},
    ],
    word: [
      {required: true, message: '凭证字不能为空', trigger: 'blur'},
    ],
    companyName: [
      {required: true, message: '公司名称不能为空', trigger: 'blur'},
    ],
    items: [
      {required: true, message: '凭证明细不能为空', trigger: 'blur'},
      {min: 1, message: '请添加凭证明细项', trigger: 'blur'},
    ],
  }).then(() => {
    const preparedItems = prepareVoucherItemsForSave(formData.value.items, {checkAuxiliary: true})
    if (!preparedItems) {
      return false
    }

    submitButtonLoading.value = true
    submitVoucher({
      ...formData.value,
      items: preparedItems,
    }).then((res: any) => {
      ElMessage.info(res.message || `提交成功`)
      resetList()
      emit("submit", res)
    }).catch((err: any) => {
      ElMessage.error(err.message)
      emit("submit", err)
    }).finally(() => {
      submitButtonLoading.value = false
    })
  }).catch((err: any) => {
    console.error(err)
    for (let errKey in err) {
      ElMessage.error(err[errKey])
      break
    }
  })
}

const hasAnySummary = (items: any[]) => {
  return items.some((item) => !!String(item.summary || '').trim())
}

const prepareVoucherItemsForSave = (items: any[] = [], options?: { checkAuxiliary?: boolean }) => {
  const workingItems = items
      .filter((item: any) => !isOrphanSubjectItem(item))
      .filter((item: any) => {
        return item.subjectId || item.creditAmount || item.debitAmount || (item.auxiliary && item.auxiliary.length > 0)
      })
      .map((item: any) => ({
        ...item,
        auxiliary: Array.isArray(item.auxiliary) ? item.auxiliary.map((aux: any) => ({...aux})) : [],
      }))

  if (workingItems.length < 2) {
    ElMessage.error('至少需要两条分录')
    return null
  }

  if (!hasAnySummary(workingItems)) {
    ElMessage.error('请至少输入一项摘要')
    return null
  }

  for (let index = 0; index < workingItems.length; index++) {
    const item = workingItems[index]
    if (!item.subjectId) {
      ElMessage.error(`第${index + 1}项没有选择总账科目`)
      return null
    }
    if (!item.creditAmount && !item.debitAmount) {
      ElMessage.error(`第${index + 1}项没有输入借方/贷方金额`)
      return null
    }

    if (options?.checkAuxiliary && bookAuxiliaryEnabled.value) {
      const subject = subjectKeyItem.value[item.subjectCode]
      const auxiliary = subject?.auxiliary || []
      for (const aux of auxiliary) {
        const auxItems = (item.auxiliary || []).filter((auxItem: any) => auxItem.id === aux.value)
        if (aux.must && (auxItems.length === 0 || auxItems[0].value.length === 0)) {
          ElMessage.error(`第${index + 1}项没有选择辅助核算项目`)
          return null
        }
      }
    }
  }

  const debitTotal = workingItems.reduce((total: Decimal, item: any) => {
    return total.plus(parseAmountValue(item.debitAmount))
  }, new Decimal(0))
  const creditTotal = workingItems.reduce((total: Decimal, item: any) => {
    return total.plus(parseAmountValue(item.creditAmount))
  }, new Decimal(0))
  if (!debitTotal.eq(creditTotal) || debitTotal.isZero()) {
    ElMessage.error('借贷不平衡，请检查分录金额')
    return null
  }

  return workingItems
}

const isOrphanSubjectItem = (item: any) => {
  const hasSubject = item.subjectCode || item.subjectId
  if (!hasSubject) {
    return false
  }
  const hasAmount = !parseAmountValue(item.debitAmount).isZero()
      || !parseAmountValue(item.creditAmount).isZero()
  const hasSummary = !!String(item.summary || '').trim()
  return !hasAmount && !hasSummary
}

const pruneOrphanVoucherItems = (items: any[] = []) => {
  return items.filter((item) => !isOrphanSubjectItem(item))
}

const applyLoadedVoucherData = (data: any) => {
  formData.value = data
  normalizeVoucherItemsSubject(formData.value.items)
  createTableData()
}

const normalizeVoucherItemsSubject = (items: any[] = []) => {
  items.forEach((item) => {
    if (item.subjectId && subjectKeyIdItem.value[item.subjectId]) {
      const subject = subjectKeyIdItem.value[item.subjectId]
      item.subjectCode = String(subject.code)
      if (!item.subjectName) {
        item.subjectName = subject.leafName || normalizeSubjectLeafName(subject)
      }
      return
    }
    if (item.subjectCode != null && item.subjectCode !== '') {
      item.subjectCode = String(item.subjectCode)
      const subject = subjectKeyItem.value[item.subjectCode]
      if (subject && !item.subjectId) {
        item.subjectId = subject.id
      }
    }
  })
}

const resolveSubjectCascaderValue = (row: any) => {
  const code = row.subjectCode != null ? String(row.subjectCode) : ''
  if (code && subjectKeyItem.value[code]) {
    return code
  }
  if (row.subjectId && subjectKeyIdItem.value[row.subjectId]) {
    return subjectKeyIdItem.value[row.subjectId].code
  }
  return code || undefined
}

let subjectClearIntent = false

const isBlankVoucherItem = (item: any) => {
  return !String(item.summary || '').trim()
      && !item.subjectId
      && !item.subjectCode
      && parseAmountValue(item.debitAmount).isZero()
      && parseAmountValue(item.creditAmount).isZero()
      && (!item.auxiliary || item.auxiliary.length === 0)
}

const clearSubjectOnRow = (scope: any) => {
  scope.row.subjectCode = ''
  scope.row.subjectId = ''
  scope.row.subjectName = ''
  scope.row.detailedAccounts = ''
  scope.row.auxiliary = []
  const idx = formData.value.items.indexOf(scope.row)
  if (idx >= 0 && isBlankVoucherItem(scope.row)) {
    formData.value.items.splice(idx, 1)
  }
  createTableData()
  if (scope.$index >= 0 && scope.$index < auxiliaryVisible.value.length) {
    auxiliaryVisible.value[scope.$index] = false
  }
}

const handleSubjectClear = (scope: any) => {
  clearSubjectOnRow(scope)
}

const handleSubjectCascaderMouseDown = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (target?.closest('.el-input__clear, .el-icon-circle-close')) {
    subjectClearIntent = true
  }
}

const handleSubjectChange = (scope: any, value: any) => {
  if (!value) {
    if (!subjectClearIntent) {
      return
    }
    subjectClearIntent = false
    clearSubjectOnRow(scope)
    return
  }
  subjectClearIntent = false
  const subject = subjectKeyItem.value[value]
  if (!subject) {
    return
  }
  scope.row.subjectCode = String(value)
  scope.row.subjectId = subject.id
  scope.row.subjectName = subject.leafName || normalizeSubjectLeafName(subject)
  scope.row.auxiliary = []
  createTableData()
  auxiliaryVisible.value[scope.$index] = false
}

const tryClearSubjectIfInputEmpty = (scope: any, input: HTMLInputElement | null | undefined) => {
  if (!input || input.tagName !== 'INPUT' || !scope.row.subjectCode) {
    return
  }
  nextTick(() => {
    if (!input.value.trim()) {
      clearSubjectOnRow(scope)
    }
  })
}

function getSubjectName(scope: any) {
  return getSubjectNameByRow(scope.row)
}

function getSubjectNameByRow(row: any) {
  const code = resolveSubjectCascaderValue(row)
  if (!code) {
    return ''
  }
  const item = subjectKeyItem.value[code]
  if (!item) {
    return code
  }
  const leafName = item.leafName || normalizeSubjectLeafName(item)
  return leafName ? `${code} ${leafName}` : code
}

function getSubjectDetailName(scope: any) {
  return getSubjectDetailNameByRow(scope.row)
}

function getSubjectDetailNameByRow(row: any) {
  if (row.auxiliary && row.auxiliary.length > 0) {
    const labels = row.auxiliary.map((item: any) => {
      const values = item.value.map((v: any) => {
        return v.label
      })
      return item.label + ":" + values.join(",")
    })
    return `${labels.join("；")}`
  }
  return ''
}

const handleAuxiliaryShow = (scope: any) => {
  if (!bookAuxiliaryEnabled.value || !scope.row.subjectCode) {
    return
  }
  if (subjectKeyItem.value[scope.row.subjectCode].auxiliary
      && subjectKeyItem.value[scope.row.subjectCode]?.auxiliary.length > 0) {
    scope.row.columnIndex = 2
    auxiliaryVisible.value[scope.$index] = !auxiliaryVisible.value[scope.$index]
  } else {
    auxiliaryVisible.value[scope.$index] = false
  }
  auxiliaryVisible.value = [...auxiliaryVisible.value]; // 触发响应式
}

function handleWordHead(v: any) {
  getVoucherAbleWordNum(
      formData.value.wordHead,
      parseTime(voucherDate, "{y}"),
      parseTime(voucherDate, "{m}")
  ).then((res: any) => {
    formData.value.wordNum = res.data
  })
}

function handleVoucherDate(v: any) {
  voucherDate = v
  // getVoucherAbleWordNum(
  //     formData.value.wordHead,
  //     parseTime(voucherDate, "{y}"),
  //     parseTime(voucherDate, "{m}")
  // )
  //     .then((res: any) => {
  //       formData.value.wordNum = res.data
  //     })
}


// ******************键盘监听支持 开始******************
const handleKeydown = (_event: KeyboardEvent) => {
  // Tab 键在单元格 input 内由 handleInputKeydown 处理
};

// 处理 input 元素的键盘事件
const handleInputKeydown = (event: KeyboardEvent, scope: any, columnIndex: number) => {
  const key = event.key;
  const isAuxiliaryColumn = bookAuxiliaryEnabled.value && columnIndex === 2
  if (key === 'Tab' || (key === 'Enter' && columnIndex !== 1 && !isAuxiliaryColumn)) {
    event.preventDefault();

    // 阻止默认行为
    if (event.shiftKey && key === 'Tab') {
      // Shift + Tab 向前移动
      moveToPreviousCell(scope, columnIndex);
    } else if (key === 'Tab' || key === 'Enter') {
      // Tab 或 Enter 向后移动
      moveToNextCell(scope, columnIndex);
    }
    if (focusedRefKey.value.indexOf('cascader') > -1) {
      refs[focusedRefKey.value].togglePopperVisible(true)
    }
  } else if (key === 'Enter' && isAuxiliaryColumn) {
    event.preventDefault();
    handleAuxiliaryShow(scope)
  }
};

// 处理 cascader 组件的键盘事件
const handleCascaderKeydown = (event: KeyboardEvent, scope: any, columnIndex: number) => {
  const key = event.key;
  const input = event.target as HTMLInputElement

  if (columnIndex === 1 && (key === 'Backspace' || key === 'Delete')) {
    tryClearSubjectIfInputEmpty(scope, input)
  }

  if (key === 'Tab') {
    event.preventDefault();

    if (event.shiftKey && key === 'Tab') {
      // Shift + Tab 向前移动
      moveToPreviousCell(scope, columnIndex);
    } else if (key === 'Tab' || key === 'Enter') {
      // Tab 或 Enter 向后移动
      moveToNextCell(scope, columnIndex);
    }
  }
};

const moveToNextCell = (scope: any, columnIndex: number) => {
  const row = scope.row;
  const rowIndex = tableSumData.value.indexOf(row);

  if (columnIndex === lastInputColumnIndex.value) {
    if (rowIndex < tableSumData.value.length - 2) {
      const nextRowIndex = rowIndex + 1
      const nextRow = tableSumData.value[nextRowIndex];
      if (nextRow.id <= 0) {
        onAddItem()
        nextTick(() => focusCell(tableSumData.value.length - 2, 0))
        return
      }
      focusCell(nextRowIndex, 0)
    } else {
      onAddItem()
      nextTick(() => focusCell(tableSumData.value.length - 2, 0))
    }
  } else {
    const nextCol = columnIndex + 1
    focusCell(rowIndex, nextCol)
    if (nextCol === 2 && bookAuxiliaryEnabled.value) {
      setTimeout(() => {
        handleAuxiliaryShow(scope);
      }, 0);
    }
  }
};

const moveToPreviousCell = (scope: any, columnIndex: number) => {
  const row = scope.row;
  const rowIndexOld = tableSumData.value.indexOf(row);

  if (columnIndex === 0) {
    let rowIndex = rowIndexOld
    if (rowIndex === 0) {
      rowIndex += 1
    }
    if (rowIndex > 0) {
      const prevCol = rowIndexOld === 0 ? 0 : lastInputColumnIndex.value
      focusCell(rowIndex - 1, prevCol)
    }
  } else {
    focusCell(rowIndexOld, columnIndex - 1)
  }
};

// 获取列对应的 ref key
const getColumnRefKey = (rowIndex: number, columnIndex: number) => {
  // 根据列索引确定组件类型
  if (columnIndex === 1) { // 科目列使用 cascader
    return `cascader-${rowIndex}-1`;
  } else { // 其他列使用 input
    return `input-${rowIndex}-${columnIndex}`;
  }
};

// ******************键盘监听支持 结束******************

watch(
    () => props.modelValue,
    (newVal: any) => {
      if (newVal && newVal.id) {
        applyLoadedVoucherData({...newVal})
      }
    },
    {immediate: true}
);

function loanBalance() {
  //console.log("debitAmount "+countRow.value.debitAmount);
  return new Decimal(countRow.value.debitAmount).eq(new Decimal(countRow.value.creditAmount));
  //return false;
}

function formatVoucherWordNum() {
  return `凭证编号：${formData.value.wordHead} ${formData.value.wordNum} 号`;
}

function formatVoucherDateChinese() {
  const dateStr = formData.value.voucherDate
  if (!dateStr) {
    return ''
  }
  const parts = dateStr.split('-')
  if (parts.length < 3) {
    return dateStr
  }
  const year = Number(parts[0])
  const month = Number(parts[1])
  const day = Number(parts[2])
  if (!year || !month || !day) {
    return dateStr
  }
  return `${year} 年 ${month} 月 ${day} 日`
}

onMounted(() => {
  props.modelValue.voucherDate = voucherDate
  if (!formData.value.voucherDate) {
    formData.value.voucherDate = voucherDate
  }
  if (!props.dialog && route.query.mode !== 'print') {
    resetList()
  }
  if (!formData.value.companyName) {
    formData.value.companyName = currBookStore.getBookItem().companyName
  }
  if (!formData.value.bookId) {
    formData.value.bookId = currBookStore.bookId
  }
  if (!formData.value.wordHead) {
    formData.value.wordHead = '收'
  }
  if (!formData.value.wordNum) {
    const now = new Date(voucherDate)
    // 获取可用凭证子号
    getVoucherAbleWordNum(formData.value.wordHead, parseTime(now, "{y}"), parseTime(now, "{m}"))
        .then((res: any) => {
          formData.value.wordNum = res.data
        })
  }

  if (route.query.mode === 'print') {
    const tempFormData = JSON.parse(window.localStorage.getItem("voucher-print-data") || 'null')
    if (tempFormData) {
      formData.value = tempFormData
    }
  }

  //传入当前账套ID
  subjectApi.getTree({
    bookId: currBookStore.bookId
  }).then((res: any) => {
    subjectList.value = res.data
    updateSubjectKeys(subjectList.value)
    if (route.query.mode === 'print') {
      applyLoadedVoucherData({...formData.value})
      const loadingInstance = ElLoading.service({fullscreen: true})
      nextTick(() => {
        loadingInstance.close()
        setTimeout(() => {
          document.title = `凭证打印-${formatVoucherWordNum()}`
          onPrint()
        }, 300)
      })
      return
    }
    if (formData.value.id) {
      normalizeVoucherItemsSubject(formData.value.items)
    }
    createTableData()
  })
})

watch(bookAuxiliaryEnabled, () => {
  createTableData()
})

// 清空 refs 对象以防止重复引用
onBeforeUpdate(() => {
  Object.keys(refs).forEach(key => {
    delete refs[key];
  });
  focusedRefKey.value = '';
});
</script>

<style scoped lang="scss">
@import "../../assets/styles/recording-voucher";

@media print and (orientation: landscape) {
  .app-container {
    width: 100%;
    max-width: 100%;
  }
}

@media print and (orientation: portrait) {
  .app-container {
    width: 100%;
    max-width: 100%;
  }
}

.redWord {
  color: red;
}

.app-container {
  background-color: #FFFFFF !important;
  padding: 30px 20px;

  .top-funs {
    position: fixed;
    top: calc(64px + 100px + 40px);
    right: 50px;
    z-index: 1000;

    &.top-funs-left {
      top: calc(64px + 100px + 46px);
      left: 230px;
      right: auto;

      display: flex;
      justify-content: space-between;
      align-items: center;
      text-align: center;
      color: $primary-color;
      font-size: 0.9em;

      .bottom-counts-item {
        width: 200px;
        color: green;

        &.isNotPh {
          color: red;
        }
      }
    }

    &.topFunsUpdate {
      top: 82px;
      left: auto;
      right: 1000px;
    }
  }

  .printable-content {
    margin-top: 65px;
    font-size: var(--el-font-size-large);

    .voucher-print-sheet + .voucher-print-sheet {
      margin-top: 32px;
    }

    :deep(.el-input__inner),
    :deep(.el-textarea__inner) {
      font-size: var(--el-font-size-large);
    }

    .header-title {
      margin-bottom: 6px;
      text-align: center;

      &-text {
        text-align: justify;
        text-align-last: justify;
        font-size: 2em;
        font-weight: bold;
        color: $primary-color;
        border-bottom: 4px double $border-color;

        .text {
          font-weight: bold;
        }
      }

      &-time {
        padding-top: 14px;
        margin-top: 4px;
        color: $primary-color;
        font-size: 1em;
        display: block;

        :deep(.header-title-date.el-date-editor) {
          width: 210px !important;
        }

        :deep(.header-title-date .el-input__wrapper) {
          width: 100%;
        }

        :deep(.header-title-date .el-input__inner) {
          text-align: center;
        }
      }
    }

    .company-info {
      margin-top: -4px;
      margin-bottom: 10px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      &-left {

      }

      &-right {
        display: flex;
        justify-content: flex-end;
        align-items: center;
      }

      .company-info-item {
        margin-right: 15px;
        font-size: inherit;
        font-weight: normal;
        color: $primary-color !important;
      }
    }

    .voucher-sheet {
      width: 100%;

      .rv-table {

        :deep(.el-table__body-wrapper),
        :deep(.el-scrollbar__wrap) {
          overflow: hidden !important;
        }

        :deep(.el-scrollbar__bar) {
          display: none !important;
        }

        :deep(.el-table__inner-wrapper::before) {
          display: none !important;
        }

        :deep(.voucher-cell-input .el-input__wrapper),
        :deep(.voucher-cell-cascader .el-input__wrapper) {
          box-shadow: none !important;
          border: none !important;
          background: transparent !important;
        }

        :deep(.voucher-cell-input .el-input__wrapper.is-focus),
        :deep(.voucher-cell-input .el-input__wrapper:hover),
        :deep(.voucher-cell-cascader .el-input__wrapper.is-focus),
        :deep(.voucher-cell-cascader .el-input__wrapper:hover) {
          box-shadow: none !important;
          height: 48px !important;
          min-height: 48px !important;
          max-height: 48px !important;
        }

        :deep(.voucher-cell-input .el-textarea__inner),
        :deep(.voucher-cell-input .el-input__inner),
        :deep(.voucher-cell-cascader .el-input__inner) {
          box-shadow: none !important;
          border: none !important;
          background: transparent !important;
        }
      }
    }

    .apply-info {
      font-size: 1em;
      margin: 8px 100px 8px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: $primary-color;
    }
  }

  // 右键
  .contextmenu {
    position: fixed;
    z-index: 1000;
    text-align: left;
    display: flex;
    justify-content: flex-start;
    flex-direction: column;
    border: 1px solid #8c939d;
    border-radius: 5px;

    .el-button {
      width: 100px;
      margin-left: 0 !important;
    }
  }

  .no-border-input {
    :deep(.el-input__wrapper) {
      padding: 0 3px; /* 减少顶部和底部的内边距 */
      margin-top: -3px; /* 使用负的外边距使元素上移 */
      box-shadow: none;
      border-bottom: 1px solid $border-color;
      border-radius: 0;

      input {
        color: $primary-color !important;
      }
    }

    &.input-number {
      display: flex;
      justify-content: flex-end;
      align-items: center;

      &.voucher-word-num {
        gap: 8px;
      }

      .voucher-page-indicator {
        margin-left: 4px;
        white-space: nowrap;
      }

      :deep(.el-input__wrapper) {
        input {
          text-align: center;
        }
      }
    }
  }

  .bottom-counts {
    position: fixed;
    bottom: 40px;
    right: 100px;
    z-index: 1000;
    display: flex;
    justify-content: space-between;
    align-items: center;
    text-align: center;
    color: $primary-color;
    font-size: 0.9em;

    .bottom-counts-item {
      width: 200px;
      color: green;

      &.isNotPh {
        color: red;
      }
    }
  }
}
</style>
<style lang="scss">
@import "../../assets/styles/recording-voucher";

.rv-print-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 1px solid $border-color;
  outline: none;
  font-size: var(--el-font-size-large);

  th,
  td {
    border: 1px dashed $border-color;
    color: $primary-color;
    padding: 2px 4px;
    font-weight: normal;
    vertical-align: middle;
    word-break: break-word;
    white-space: normal;
    line-height: 1.35;
  }

  thead th {
    height: 32px;
    text-align: center;
  }

  thead th.rv-col-amount {
    text-align: center;
  }

  tbody td.rv-col-amount {
    text-align: right;
  }

  .rv-col-summary {
    width: 20%;
  }

  tbody .rv-table-entry-row td,
  tbody .rv-table-carry-row td {
    height: 44px;
    font-size: var(--el-font-size-base);
  }

  tbody .rv-table-count-row td {
    height: 34px;
  }

  tbody .rv-table-count-row .rv-col-summary,
  tbody .rv-table-carry-row .rv-col-summary {
    text-align: center;
  }

  tr > :first-child {
    border-left: none;
  }

  tr > :last-child {
    border-right: none;
  }

  thead tr:first-child th {
    border-top: none;
  }

  tbody tr:last-child td {
    border-bottom: none;
  }

  .rv-col-amount {
    text-align: right;
  }

  thead th.rv-col-amount {
    text-align: center;
  }

  .voucher-grand-total-label {
    display: flex;
    align-items: flex-start;
    width: 100%;
    color: $primary-color;
    font-size: var(--el-font-size-large);
    line-height: 1.4;
    text-align: left;
    padding-left: 0.5em;
    gap: 0.15em;
  }

  .voucher-grand-total-fixed {
    flex: 0 0 auto;
    text-align: left;
  }

  .voucher-grand-total-amount {
    flex: 1 1 auto;
    text-align: left;
    word-break: break-word;
  }

  .voucher-footer-label {
    display: block;
    width: 100%;
    text-align: center;
    color: $primary-color;
    font-size: var(--el-font-size-large);
    line-height: 1.4;
  }

  .voucher-cell-amount-text {
    display: block;
    width: 100%;
    text-align: right;
    padding-right: 6px;
    line-height: 1.4;
    white-space: nowrap;
  }
}

@media print {
  @page {
    size: A4 landscape;
    margin: 8mm 12mm;
  }

  html, body {
    width: 100% !important;
    height: auto !important;
    min-height: 0 !important;
    overflow: visible !important;
  }

  body.voucher-printing .top-funs,
  body.voucher-printing .contextmenu {
    display: none !important;
  }

  body.voucher-printing * {
    visibility: hidden;
  }

  body.voucher-printing #printable-content,
  body.voucher-printing #printable-content * {
    visibility: visible;
  }

  body.voucher-printing .app-container {
    padding: 0 !important;
    margin: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    background: #fff !important;
  }

  #printable-content {
    position: static;
    left: auto;
    top: auto;
    transform: none;
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    box-sizing: border-box;
    font-size: var(--el-font-size-large) !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  #printable-content .voucher-print-sheet {
    display: block;
    box-sizing: border-box;
    page-break-inside: avoid;
    break-inside: avoid-page;
    page-break-after: always;
    break-after: page;
  }

  #printable-content .voucher-print-sheet + .voucher-print-sheet {
    page-break-before: always;
    break-before: page;
    margin-top: 0 !important;
    padding-top: 0 !important;
  }

  #printable-content .voucher-print-sheet:last-child {
    page-break-after: auto;
    break-after: auto;
  }

  #printable-content .voucher-print-sheet .header-title {
    margin-bottom: 2px !important;
  }

  #printable-content .voucher-print-sheet .header-title-text {
    font-size: 1.6em !important;
    border-bottom-width: 3px !important;
  }

  #printable-content .voucher-print-sheet .header-title-time {
    padding-top: 4px !important;
    margin-top: 0 !important;
  }

  #printable-content .company-info {
    width: 100% !important;
    margin-top: -4px !important;
    margin-bottom: 10px !important;
  }

  #printable-content .company-info .company-info-item {
    font-size: inherit !important;
  }

  #printable-content .voucher-sheet {
    width: 100% !important;
    box-sizing: border-box !important;
    overflow: visible !important;
  }

  #printable-content .el-table.rv-table {
    outline: none !important;
    border: 1px solid $border-color !important;
    box-sizing: border-box !important;
  }

  #printable-content .el-table__cell {
    border-color: $border-color !important;
    color: $primary-color !important;
  }

  #printable-content .rv-table .el-table__body-wrapper .el-table__row:last-child .el-table__cell {
    border-bottom: 1px solid $border-color !important;
  }

  #printable-content .rv-table-entry-row .rv-table-cell {
    height: 44px !important;
    max-height: 44px !important;
  }

  #printable-content .rv-table-entry-row .rv-table-cell .cell {
    max-height: 44px !important;
    line-height: 1.35 !important;
  }

  #printable-content .rv-table-count-row .rv-table-count-cell {
    height: 34px !important;
  }

  #printable-content .rv-table-carry-row .rv-table-cell {
    height: 44px !important;
  }

  #printable-content .rv-table-carry-row .rv-col-summary .cell {
    text-align: center !important;
  }

  #printable-content .el-table {
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
  }

  #printable-content .el-table__inner-wrapper {
    height: auto !important;
  }

  #printable-content .el-table__header-wrapper,
  #printable-content .el-table__body-wrapper {
    overflow: visible !important;
    height: auto !important;
    max-height: none !important;
  }

  #printable-content .el-scrollbar__wrap {
    overflow: visible !important;
    height: auto !important;
    max-height: none !important;
  }

  #printable-content .el-table__inner-wrapper::before,
  #printable-content .el-table__inner-wrapper::after,
  #printable-content .el-scrollbar__bar {
    display: none !important;
  }

  #printable-content .el-table__header-wrapper table,
  #printable-content .el-table__body-wrapper table {
    width: 100% !important;
    table-layout: fixed !important;
  }

  #printable-content .rv-col-summary,
  #printable-content th.rv-col-summary,
  #printable-content td.rv-col-summary {
    width: 180px !important;
    min-width: 180px !important;
    max-width: 180px !important;
  }

  #printable-content .rv-col-subject,
  #printable-content .rv-col-amount,
  .rv-table .rv-col-subject,
  .rv-table .rv-col-amount {
    min-width: 0 !important;
    max-width: none !important;
  }

  #printable-content .rv-table-count-row .rv-col-summary .cell {
    text-align: center !important;
  }

  #printable-content .el-table colgroup col[name="gutter"] {
    width: 0 !important;
  }

  #printable-content .el-table .gutter {
    display: none !important;
  }

  #printable-content .el-table .cell {
    padding: 2px 4px !important;
    white-space: normal !important;
    word-break: break-word;
    line-height: 1.4;
  }

  #printable-content .rv-table-entry-row .cell {
    font-size: var(--el-font-size-base) !important;
  }

  #printable-content .rv-table-count-row .cell {
    font-size: var(--el-font-size-large) !important;
  }

  #printable-content .apply-info {
    width: 100% !important;
    margin: 6px 0 0 !important;
    padding: 0 !important;
    box-sizing: border-box;
  }

  #printable-content .rv-print-table {
    width: 100% !important;
    border-collapse: collapse !important;
    table-layout: fixed !important;
    border: 1px solid $border-color !important;
  }

  #printable-content .rv-print-table th,
  #printable-content .rv-print-table td {
    border: 1px dashed $border-color !important;
    border-color: $border-color !important;
    color: $primary-color !important;
    padding: 2px 4px !important;
    font-weight: normal !important;
    vertical-align: middle !important;
    word-break: break-word;
    white-space: normal;
    line-height: 1.35;
  }

  #printable-content .rv-print-table thead th {
    height: 32px !important;
    text-align: center !important;
    font-size: var(--el-font-size-large) !important;
  }

  #printable-content .rv-print-table .rv-col-summary {
    width: 180px !important;
  }

  #printable-content .rv-print-table tbody .rv-table-entry-row td,
  #printable-content .rv-print-table tbody .rv-table-carry-row td {
    height: 44px !important;
    font-size: var(--el-font-size-base) !important;
  }

  #printable-content .rv-print-table tbody .rv-table-count-row td {
    height: 34px !important;
    font-size: var(--el-font-size-large) !important;
  }

  #printable-content .rv-print-table tbody .rv-table-count-row .rv-col-summary,
  #printable-content .rv-print-table tbody .rv-table-carry-row .rv-col-summary {
    text-align: center !important;
  }

  #printable-content .rv-print-table tr > :first-child {
    border-left: none !important;
  }

  #printable-content .rv-print-table tr > :last-child {
    border-right: none !important;
  }

  #printable-content .rv-print-table thead tr:first-child th {
    border-top: none !important;
  }

  #printable-content .rv-print-table tbody tr:last-child td {
    border-bottom: none !important;
  }
}

.rv-table, .rv-table-count {
  border: 1px solid $border-color;
  outline: none;
  font-size: var(--el-font-size-large);

  &.el-table .el-table__header-wrapper th {
    height: 32px !important;
  }

  &.el-table .el-table__cell .cell {
    font-size: var(--el-font-size-large);
  }
}

.rv-table-count-row {
  .rv-table-count-cell {
    font-weight: normal;
    height: 38px !important;
  }

  .rv-table-count-cell:first-child {
    color: $primary-color;
    text-align: center;
  }
}

.receipt-num-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 0;
  color: $primary-color;
}

.rv-table-entry-row {
  .rv-table-cell {
    height: 56px !important;
    max-height: 56px !important;
    overflow: hidden;
  }

  .rv-table-cell .cell {
    font-size: var(--el-font-size-base);
    white-space: normal;
    word-break: break-word;
    line-height: 1.4;
    height: 100%;
    max-height: 56px;
    overflow: hidden;
  }
}

.rv-table-header-row {
  border: none !important;
}

.rv-table-header-cell, .rv-table-cell {
  border: 1px dashed $border-color;
}

.rv-table .el-table__cell:first-child {
  border-left: none !important;
}

.rv-table .el-table__cell:last-child {
  border-right: none !important;
}

.rv-table .el-table__header-wrapper .el-table__cell {
  border-top: none !important;
}

.rv-table .el-table__body-wrapper .el-table__row:last-child .el-table__cell {
  border-bottom: none !important;
}

.rv-table-header-cell {
  color: $primary-color !important;
  font-weight: normal !important;
  padding: 0 !important;
}

.rv-table .rv-col-subject {
  min-width: 238px !important;
}

.rv-table .rv-col-amount {
  min-width: 130px !important;
  max-width: 180px;
}

.rv-table .el-table__header-wrapper .rv-col-amount .cell {
  text-align: center !important;
}

.rv-table .el-table__body-wrapper .rv-col-amount .cell {
  text-align: right !important;
}

.voucher-grand-total-label {
  display: flex;
  align-items: flex-start;
  width: 100%;
  color: $primary-color;
  font-size: var(--el-font-size-large);
  line-height: 1.4;
  text-align: left;
  padding-left: 0.5em;
  gap: 0.15em;
}

.voucher-grand-total-fixed {
  flex: 0 0 auto;
  text-align: left;
}

.voucher-grand-total-amount {
  flex: 1 1 auto;
  text-align: left;
  word-break: break-word;
}

.voucher-footer-label {
  display: block;
  width: 100%;
  text-align: center;
  color: $primary-color;
  font-size: var(--el-font-size-large);
  line-height: 1.4;
}

.voucher-cell-amount-text {
  display: block;
  width: 100%;
  text-align: right;
  padding-right: 6px;
  line-height: 1.4;
  white-space: nowrap;
}

.rv-table-cell {
  height: 38px !important;
  padding: 0 !important;

  .cell {
    padding: 0 4px !important;
    line-height: 1.4;
  }
}

.voucher-cell-text {
  display: block;
  white-space: normal;
  word-break: break-word;
  line-height: 1.4;
  max-height: calc(1.4em * 2);
  overflow: hidden;
}

.voucher-cell-input {
  width: 100%;
  --el-input-border-color: transparent;
  --el-input-hover-border-color: transparent;
  --el-input-focus-border-color: transparent;
  --el-input-bg-color: transparent;
  --el-fill-color-blank: transparent;
}

.rv-table-entry-row .voucher-cell-input .el-input__wrapper,
.rv-table-entry-row .voucher-cell-cascader .el-input__wrapper {
  height: 48px !important;
  min-height: 48px !important;
  max-height: 48px !important;
  overflow: hidden;
  box-sizing: border-box;
}

.rv-table-entry-row .voucher-cell-cascader {
  height: 48px;
  overflow: hidden;

  .el-input {
    height: 48px !important;
  }
}

.rv-table .voucher-cell-input .el-input__wrapper,
.rv-table .voucher-cell-cascader .el-input__wrapper {
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  padding: 0 4px !important;
  min-height: 34px;
}

.rv-table .voucher-cell-input .el-input__wrapper:hover,
.rv-table .voucher-cell-input .el-input__wrapper.is-focus,
.rv-table .voucher-cell-cascader .el-input__wrapper:hover,
.rv-table .voucher-cell-cascader .el-input__wrapper.is-focus {
  box-shadow: none !important;
  background: transparent !important;
}

.rv-table-entry-row .voucher-cell-input .el-input__inner,
.rv-table-entry-row .voucher-cell-cascader .el-input__inner {
  height: 48px !important;
  min-height: unset !important;
  max-height: 48px !important;
  line-height: 1.4;
  font-size: var(--el-font-size-base);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.rv-table-entry-row .voucher-cell-cascader .el-input__wrapper.is-focus,
.rv-table-entry-row .voucher-cell-cascader .el-input__wrapper:hover {
  height: 48px !important;
  min-height: 48px !important;
  max-height: 48px !important;
}

.rv-table .voucher-cell-input .el-input__inner,
.rv-table .voucher-cell-cascader .el-input__inner {
  height: 34px;
  line-height: 34px;
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  color: inherit;
}

.rv-table-entry-row .voucher-cell-input .el-textarea__inner {
  height: 48px !important;
  min-height: 48px !important;
  max-height: 48px !important;
  font-size: var(--el-font-size-base);
  line-height: 1.4;
  overflow: hidden;
}

.rv-table .voucher-cell-input .el-textarea__inner {
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  padding: 4px !important;
  min-height: 34px !important;
  resize: none !important;
  overflow: hidden;
  line-height: 1.4;
  color: inherit;
}

.rv-table .voucher-cell-amount .el-input__inner {
  text-align: right;
}

.rv-table .voucher-cell-readonly .el-input__wrapper,
.rv-table .voucher-cell-readonly .el-input__wrapper:hover,
.rv-table .voucher-cell-readonly .el-input__wrapper.is-focus {
  cursor: default;
  box-shadow: none !important;
  background: transparent !important;
}

.rv-table .voucher-cell-readonly .el-input__inner,
.rv-table .voucher-cell-readonly .el-textarea__inner {
  cursor: default;
  color: inherit;
  -webkit-text-fill-color: inherit;
}

.rv-table .voucher-cell-input.redWord .el-input__inner,
.rv-table .voucher-cell-input.redWord .el-textarea__inner {
  color: red;
}

.rv-table .voucher-cell-cascader .el-input__suffix {
  opacity: 0.45;
}

.rv-table .voucher-cell-cascader .el-input__wrapper.is-focus .el-input__suffix,
.rv-table .voucher-cell-cascader:hover .el-input__suffix {
  opacity: 1;
}

.rv-table .voucher-cell-cascader .el-input__inner::placeholder {
  color: transparent;
}

.rv-table-entry-row:first-child {
  .rv-table-cell {
    border-top: 1px solid $border-color;
  }
}

.amount-chinese-text {
  padding: 0 8px;
  letter-spacing: 1px;
}

</style>

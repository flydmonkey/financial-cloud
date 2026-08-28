<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <div class="queryForm">
        <el-form
          v-show="showSearch"
          ref="queryRef"
          :model="queryParams"
          :inline="true"
          label-width="68px"
        >
          <el-form-item
            label=""
            prop="periodType"
          >
            <el-radio-group
              v-model="queryParams.periodType"
              @change="handlePeriodType"
            >
              <el-radio-button
                label="月度"
                value="month"
              />
              <el-radio-button
                label="季度"
                value="quarter"
              />
              <el-radio-button
                label="年度"
                value="year"
              />
            </el-radio-group>
          </el-form-item>

          <el-form-item
            v-if="queryParams.periodType === 'month'"
            label="选择月度"
            prop="reportDate"
          >
            <el-date-picker
              v-model="queryParams.date"
              style="width: 130px"
              type="month"
              :clearable="false"
              value-format="YYYY-MM"
              format="YYYY年MM期"
              :disabled-date="disabledDate"
              placeholder="选择月"
              @change="handleQuery"
            />
          </el-form-item>

          <el-form-item
            v-if="queryParams.periodType === 'quarter'"
            label="选择季度"
            prop="reportDate"
          >
            <el-date-picker
              v-model="queryParams.date"
              style="width: 100px"
              :clearable="false"
              type="year"
              format="YYYY"
              value-format="YYYY"
              :disabled-date="disabledDate"
              :prefix-icon="customPrefix"
              placeholder="选择年"
              @change="handleQuery"
            />
            <el-radio-group
              v-model="queryParams.reportQuarter"
              style="margin-left: 10px"
              @change="handleQuery"
            >
              <el-radio-button
                label="第一季度"
                value="Q1"
              />
              <el-radio-button
                label="第二季度"
                value="Q2"
              />
              <el-radio-button
                label="第三季度"
                value="Q3"
              />
              <el-radio-button
                label="第四季度"
                value="Q4"
              />
            </el-radio-group>
          </el-form-item>

          <el-form-item
            v-if="queryParams.periodType === 'year'"
            label="选择年度"
            prop="reportDate"
          >
            <el-date-picker
              v-model="queryParams.date"
              type="year"
              style="width: 100px"
              :clearable="false"
              :disabled-date="disabledDate"
              value-format="YYYY"
              placeholder="选择年"
              @change="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button @click="handleQuery">
              刷新
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="common-card">
      <div class="btn-form">
        <!--        <el-button type="primary" @click="handleExport">导出</el-button>-->
      </div>
      <div style="display: flex;justify-content: flex-start">
        <div style="width: 300px;display: inline-block">
          <el-input
            v-model="filterSubject"
            style="width: 95%;margin-bottom: 10px"
            placeholder="快速搜索"
            suffix-icon="Search"
          />
          <el-tree
            v-if="subjectList.length > 0"
            ref="treeRef"
            style="height: 60vh;overflow-y: auto"
            :data="subjectList"
            :props="defaultProps"
            node-key="code"
            highlight-current
            :current-node-key="currentSubjectKey"
            :default-expanded-keys="expandedKeys"
            :filter-node-method="filterNodeMethod"
            :expand-on-click-node="false"
            @node-click="handleTreeNodeClick"
          />
        </div>
        <div style="width: calc(100% - 320px);margin-left: 20px;display: inline-block">
          <el-table
            v-loading="loading"
            :data="recordsList"
            height="570"
            row-key="id"
            show-summary
            :summary-method="handleSummaryMethod2"
            :span-method="objectSpanMethod"
            border
          >
            <el-table-column
              label="日期"
              align="center"
              prop="voucherDate"
            />
            <el-table-column
              label="凭证字号"
              align="left"
              header-align="center"
              prop="word"
            />
            <el-table-column
              label="摘要"
              align="left"
              header-align="center"
              prop="summary"
            />
            <el-table-column
              label="借方金额"
              align="right"
              prop="debitAmount"
            >
              <template #default="scope">
                {{ formatAmount(scope.row.debitAmount, '') }}
              </template>
            </el-table-column>
            <el-table-column
              label="贷方金额"
              align="right"
              prop="creditAmount"
            >
              <template #default="scope">
                {{ formatAmount(scope.row.creditAmount, '') }}
              </template>
            </el-table-column>
            <el-table-column
              label="余额"
              align="right"
              prop="subjectBalance"
            >
              <template #default="scope">
                {{ formatAmount(scope.row.subjectBalance, '') }}
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
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="VoucherItems" lang="ts">
import * as subjectApi from "@/api/standard/standard-subject"
import * as apis from "@/api/voucher/voucher";
import {parseTime, getCurrentQuarter, handleTree} from '@/utils/Jinbooks'
import {h, ref, shallowRef, reactive, toRefs, watch, nextTick, onMounted, onActivated} from 'vue'
import {formatAmount} from "@/utils"
import {useRouter, useRoute} from "vue-router";
import booksSetStore from "@/store/modules/bookStore";
import Template from "@/views/hr/salary-voucher-rules/template.vue";
import {TableColumnCtx, TreeInstance} from "element-plus";
import {handleSummaryMethod, subjectMatchesKeyword, SummaryMethodProps} from "@/utils/Subjects";

const router = useRouter();
const route = useRoute();
const currBookStore = booksSetStore()
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const recordsList = ref<any>([]);
const filterSubject = ref<any>("");
// 会计科目数据
const subjectList = ref<any>([])
const spanDataMap = ref<any>({})
const treeRef = ref<TreeInstance>()
/** 左侧树当前选中的科目编码（空字符串表示「所有科目」） */
const currentSubjectKey = ref<string>('')
const expandedKeys = ref<string[]>([])
const treeReady = ref(false)

const defaultProps = {
  children: 'children',
  label: 'name',
}

interface Tree {
  [key: string]: any
}

interface SpanMethodProps {
  row: any
  column: TableColumnCtx<any>
  rowIndex: number
  columnIndex: number
}

const data = reactive({
  form: {},
  queryParams: {
    pageNumber: 1,
    pageSize: 20,
    orderByColumn: "voucherDate,wordHead,wordNum",
    isAsc: "desc,desc,desc",
    periodType: 'month',
    date: currBookStore.termCurrent,
    reportQuarter: getCurrentQuarter(),
    reportDate: currBookStore.termCurrent,
    subjectCode: normalizeQuery(route.query.subjectCode),
  },
});

const {queryParams} = toRefs(data);

function normalizeQuery(value: unknown): string {
  if (Array.isArray(value)) {
    return value[0] != null ? String(value[0]) : ''
  }
  return value != null && value !== '' ? String(value) : ''
}

/** 在科目树中查找目标编码的祖先路径（含自身） */
function findSubjectPath(nodes: any[], code: string, path: any[] = []): any[] | null {
  for (const node of nodes || []) {
    const next = [...path, node]
    if (node.code === code) {
      return next
    }
    if (node.children?.length) {
      const found = findSubjectPath(node.children, code, next)
      if (found) {
        return found
      }
    }
  }
  return null
}

/** 根据路由/查询参数选中左侧科目并刷新列表 */
function applySubjectFromRoute(triggerQuery = true) {
  const code = normalizeQuery(route.query.subjectCode)
  queryParams.value.subjectCode = code
  currentSubjectKey.value = code || ''

  const date = normalizeQuery(route.query.date)
  if (date) {
    if (date.length === 4) {
      queryParams.value.periodType = 'year'
      queryParams.value.date = date
    } else if (date.length >= 7) {
      queryParams.value.periodType = 'month'
      queryParams.value.date = date.substring(0, 7)
    }
  }

  if (!treeReady.value) {
    return
  }

  nextTick(() => {
    const tree = treeRef.value
    if (!tree) {
      return
    }
    if (code) {
      const path = findSubjectPath(subjectList.value, code)
      if (path?.length) {
        // 展开祖先节点，保证目标科目可见
        path.slice(0, -1).forEach((n: any) => {
          if (n?.code == null || n.code === '') {
            return
          }
          const node = tree.getNode(n.code)
          if (node) {
            node.expanded = true
          }
        })
        expandedKeys.value = path
            .slice(0, -1)
            .map((n: any) => n.code)
            .filter((c: string) => c != null && c !== '')
        currentSubjectKey.value = code
        tree.setCurrentKey(code)
      } else {
        tree.setCurrentKey(null as any)
        currentSubjectKey.value = ''
      }
    } else {
      expandedKeys.value = []
      currentSubjectKey.value = ''
      tree.setCurrentKey('')
    }
    if (triggerQuery) {
      handleQuery()
    }
  })
}

const customPrefix = shallowRef({
  render() {
    return h('p', '年')
  },
})
const handlePeriodType = (value: string) => {
  if (value === 'month' && queryParams.value.date.length < 7) {
    queryParams.value.date = queryParams.value.date + "-01"
  }
  handleQuery()
}
const disabledDate = (time: any) => {
  const now = new Date(currBookStore.termCurrent + "-01")
  const start = new Date(currBookStore.termStart + "-01")
  return start.getTime() > time.getTime() || time.getTime() > now.getTime();
}

/** 查询凭证记录列表 */
function getList() {
  loading.value = true;
  apis.listVoucherSubLedger(queryParams.value).then((response: any) => {
    recordsList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
    // 初始化合并数据（首次渲染或数据变化时调用一次）
    spanDataMap.value = getSpanMap(recordsList.value, ['voucherDate', 'word'])
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  if (queryParams.value.periodType === 'quarter') {
    queryParams.value.reportDate = queryParams.value.date.substring(0, 4) + ' ' + queryParams.value.reportQuarter
  } else {
    queryParams.value.reportDate = queryParams.value.date
  }
  getList();
}

function handleExport() {

}

function handleSummaryMethod2(param: SummaryMethodProps) {
  return handleSummaryMethod(param, recordsList.value, 0, [3, 4, 5])
}

const filterNodeMethod = (value: string, data: Tree) => {
  return subjectMatchesKeyword(data, value)
}

const handleTreeNodeClick = (data: Tree) => {
  // 「所有科目」无 code
  const code = data?.code != null ? String(data.code) : ''
  queryParams.value.subjectCode = code
  currentSubjectKey.value = code
  handleQuery()
}

/**
 * 合并单元格
 * @param row
 * @param column
 * @param rowIndex
 * @param columnIndex
 */
const objectSpanMethod = ({
                            row,
                            column,
                            rowIndex,
                            columnIndex,
                          }: SpanMethodProps) => {
  const colProp = column.property
  if (['voucherDate', 'word'].includes(colProp)) {
    const colSpanInfo = spanDataMap.value.get(colProp)
    const rowspan = colSpanInfo?.get(rowIndex) ?? 1
    return {
      rowspan: rowspan,
      colspan: 1,
    }
  }

}

// 处理合并信息
const getSpanMap = (data: any[], keys: string[]) => {
  const spanMap = new Map<string, Map<number, number>>()

  keys.forEach(key => {
    const map = new Map<number, number>()
    let prev: any = null
    let spanCount = 0
    let start = 0

    data.forEach((item, index) => {
      if (item[key] === prev) {
        spanCount++
      } else {
        if (spanCount > 0) {
          map.set(start, spanCount + 1)
          for (let i = start + 1; i < start + 1 + spanCount; i++) {
            map.set(i, 0)
          }
        }
        start = index
        spanCount = 0
        prev = item[key]
      }
    })

    // 处理最后一组
    if (spanCount > 0) {
      map.set(start, spanCount + 1)
      for (let i = start + 1; i < start + 1 + spanCount; i++) {
        map.set(i, 0)
      }
    } else {
      map.set(start, 1)
    }

    spanMap.set(key, map)
  })

  return spanMap
}

watch(filterSubject, (val) => {
  treeRef.value!.filter(val)
})

watch(
  () => [route.query.subjectCode, route.query.date],
  () => {
    if (treeReady.value) {
      applySubjectFromRoute(true)
    }
  }
)

function loadSubjectTree() {
  return subjectApi.getTree({
    bookId: currBookStore.bookId
  }).then((res: any) => {
    subjectList.value = [{
      "id": "-1",
      "parentId": null,
      "code": "",
      "name": "所有科目",
      "displayName": "所有科目",
    }, ...res.data]
    treeReady.value = true
    applySubjectFromRoute(true)
  })
}

onMounted(() => {
  loadSubjectTree()
})

onActivated(() => {
  // keep-alive 场景：再次进入时按最新 query 选中科目
  if (treeReady.value) {
    applySubjectFromRoute(true)
  }
})
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

</style>

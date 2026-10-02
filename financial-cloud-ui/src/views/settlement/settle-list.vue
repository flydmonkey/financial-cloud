<!--年度结账列表-->
<template>
  <div class="app-container">
    <el-card class="common-card">
      <el-tabs
        v-model="activeName"
        type="card"
        class="demo-tabs"
        @tab-click="handleClick"
      >
        <el-tab-pane
          label="期末处理"
          name="carry-forward"
        >
          carry
        </el-tab-pane>
        <el-tab-pane
          label="结账"
          name="settle-period"
        >
          settle
        </el-tab-pane>
        <el-tab-pane
          label="结账列表"
          name="settle-list"
        >
          <div class="queryForm">
            <el-form
              ref="queryRef"
              :model="queryParams"
              :inline="true"
              label-width="68px"
            >
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
                  value-format="YYYY"
                  placeholder="选择年"
                  @change="handleQuery"
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
                  type="success"
                  :loading="booksPackLoading"
                  @click="openBooksPackDialog"
                >
                  导出本月账本包
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table
            v-loading="loading"
            :data="list"
            @selection-change="handleSelectionChange"
          >
            <el-table-column
              type="selection"
              width="55"
              align="center"
            />
            <el-table-column
              label="月份"
              align="center"
              prop="period"
            >
              <template #default="scope">
                {{ scope.row.period }} 月
              </template>
            </el-table-column>
            <el-table-column
              :label="$t('jbx.text.status.status')"
              align="center"
              prop="status"
            >
              <template #default="scope">
                <span v-if="scope.row.status == 6"><el-icon
                  color="green"
                  size="24"
                ><CircleCheck /></el-icon></span>
                <span v-if="scope.row.status == 4"><el-icon
                  color="#808080"
                  size="24"
                ><WarningFilled /></el-icon></span>
                <span v-if="scope.row.status == 1"><el-icon
                  color="#67C23A"
                  size="24"
                ><Promotion /></el-icon></span>
                <span v-if="scope.row.status == 2"><el-icon
                  color="#E6A23C"
                  size="24"
                ><Clock /></el-icon></span>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="120"
              align="center"
              class-name="small-padding fixed-width"
            >
              <template #default="scope">
                <el-button
                  v-if="canUncheckout(scope.row)"
                  link
                  type="danger"
                  @click="handleUncheckout(scope.row)"
                >
                  反结账
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
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-dialog v-model="booksPackDialogVisible" title="导出本月账本包" width="420px">
      <el-form label-width="110px">
        <el-form-item label="交付账期" required>
          <el-date-picker
            v-model="booksPackForm.yearPeriod"
            type="month"
            value-format="YYYY-MM"
            :clearable="false"
            placeholder="选择账期"
          />
        </el-form-item>
        <el-form-item label="交付内容">
          <el-checkbox v-model="booksPackForm.includeVoucherList">含凭证清单</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="booksPackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="booksPackLoading" @click="exportBooksPack">确认导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {getCurrentInstance, ref, toRefs, reactive, watch} from 'vue'
import { previousYearPeriod, validYearPeriod } from '@/utils/accountingGuide'
import type {TabsPaneContext} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useRoute, useRouter} from "vue-router";
import * as settlementApi from "@/api/book/settlement";
import {exportMonthlyBooksPack} from "@/api/statement/statement";
import bookStore from "@/store/modules/bookStore";
import {parseTime} from "@/utils/financialCloud";
import {downloadData} from "@/utils";

const {proxy} = getCurrentInstance()!;
const currBookStore = bookStore()
const currentTerm = ref(currBookStore.termCurrent || parseTime(new Date(), "{y}-{m}"));

const list: any = ref<any>([]);
const open: any = ref(false);
const loading: any = ref(false);
const showSearch: any = ref(true);
const ids: any = ref<any>([]);
const single: any = ref(true);
const multiple: any = ref(true);
const total: any = ref(0);
const title: any = ref("");
const uncheckoutLoading = ref(false)
const booksPackLoading = ref(false)
const booksPackDialogVisible = ref(false)
const booksPackForm = reactive({
  yearPeriod: String(currBookStore.termCurrent || parseTime(new Date(), "{y}-{m}")),
  includeVoucherList: true,
})

const activeName = ref('settle-list')
const router: any = useRouter();
const route = useRoute();

const data = reactive({
  form: {} as Record<string, any>,
  queryParams: {
    periodType: 'year',
    date: currentTerm,
    year: (currentTerm.value + "").substring(0, 4),
    pageNumber: 1,
    pageSize: 10,
    providerName: undefined

  },
  rules: {
    yearPeriod: [
      {required: true, message: '期间不能为空', trigger: 'blur'}
    ],
  }
});

const {queryParams, form, rules} = toRefs(data);

/** 当前账期的上一月 YYYY-MM */
function prevYearPeriod(term: string): string {
  const parts = String(term || '').split('-')
  const y = Number(parts[0])
  const m = Number(parts[1])
  if (!y || !m) return ''
  if (m === 1) return `${y - 1}-12`
  return `${y}-${String(m - 1).padStart(2, '0')}`
}

function rowYearPeriod(row: any): string {
  if (row?.yearPeriod) return String(row.yearPeriod)
  const y = row?.year
  const p = row?.period
  if (y == null || p == null) return ''
  return `${y}-${String(p).padStart(2, '0')}`
}

/** 仅「当前账期上一月」且已结（status=6）可反结账 */
function canUncheckout(row: any): boolean {
  const term = currBookStore.termCurrent || currentTerm.value
  if (!term || Number(row?.status) !== 6) return false
  return rowYearPeriod(row) === prevYearPeriod(String(term))
}

async function handleUncheckout(row: any) {
  const target = rowYearPeriod(row)
  if (!target || !canUncheckout(row)) {
    ElMessage.warning('只能反结账最近已结期间')
    return
  }
  try {
    const {value} = await ElMessageBox.prompt(
      `将重新打开账期 ${target}。下期若已有凭证或日记账会被拒绝。\n请输入账期 ${target} 以确认：`,
      '反结账确认',
      {
        confirmButtonText: '反结账',
        cancelButtonText: '取消',
        inputPattern: new RegExp(`^${target.replace('-', '\\-')}$`),
        inputErrorMessage: `请精确输入 ${target}`,
        type: 'warning',
      }
    )
    if (value !== target) {
      ElMessage.warning(`请精确输入 ${target}`)
      return
    }
  } catch {
    return
  }

  uncheckoutLoading.value = true
  try {
    const res: any = await settlementApi.uncheckout(target)
    if (res.code === 0) {
      ElMessage.success(res.message || '反结账完成')
      await currBookStore.refreshData()
      currentTerm.value = currBookStore.termCurrent || currentTerm.value
      getList()
    } else {
      ElMessage.error(res.message || '反结账失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '反结账失败')
  } finally {
    uncheckoutLoading.value = false
  }
}

function openBooksPackDialog() {
  const previous = previousYearPeriod(currBookStore.termCurrent || currentTerm.value)
  const start = validYearPeriod(currBookStore.termStart)
  booksPackForm.yearPeriod = validYearPeriod(route.query.yearPeriod) || (start && previous < start ? '' : previous)
  booksPackForm.includeVoucherList = true
  booksPackDialogVisible.value = true
}

watch(() => [route.query.yearPeriod, route.query.deliver], ([term, deliver]) => {
  if (route.path === '/settlement/settle-list' && deliver === '1' && validYearPeriod(term)) {
    openBooksPackDialog()
  }
}, { immediate: true })

async function exportBooksPack() {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(booksPackForm.yearPeriod)) {
    ElMessage.warning('请选择有效账期')
    return
  }
  booksPackLoading.value = true
  try {
    const blob = await exportMonthlyBooksPack({
      yearPeriod: booksPackForm.yearPeriod,
      includeVoucherList: booksPackForm.includeVoucherList,
    })
    downloadData(blob, `本月账本包_${booksPackForm.yearPeriod}.zip`)
    booksPackDialogVisible.value = false
    ElMessage.success('账本包已生成')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '账本包导出失败')
  } finally {
    booksPackLoading.value = false
  }
}

/** 分页列表 */
function getList(): any {
  loading.value = true;
  queryParams.value.year = (queryParams.value.date + "").substring(0, 4);
  settlementApi.fetch(queryParams.value).then((res: any) => {
    loading.value = false;
    if (res.code === 0) {
      list.value = res.data.records;
      total.value = res.data.total;
    } else {
      //proxy?.$modal.msgSuccess(res.message);
    }
  });
}

/** 多选框选中数据 */
function handleSelectionChange(selection: any): any {
  ids.value = selection.map((item: any) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}


/** 添加分组 */
function handleAdd(): any {
  open.value = true;
  // title.value = t('jbx.text.add');
}

/** 修改按钮操作 */
function handleUpdate(row: any): any {

  const id: any = row.id || ids.value;
  /*
  get(id).then((res: any) =>  {
    form.value = res.data;
    open.value = true;
    title.value = t('jbx.text.edit');
  });
  */
};

/** 删除按钮操作 */
function handleDelete(row: any): any {
  const id: any = row.id || ids.value;
  /* modal.confirm(t('jbx.confirm.text.delete')).then(function () {
     return del(id);
   }).then(() => {
     getList();
     modal.msgSuccess(t('jbx.alert.operate.success'));
   }).catch(() => {});
   */
}


/** 搜索按钮操作 */
function handleQuery() {

  getList();
}

const handleClick = (tab: TabsPaneContext, event: Event) => {
  proxy?.$tab.openPage('/settlement/' + tab.paneName)
}

getList();
</script>

<style>
.demo-tabs > .el-tabs__content {

}
</style>

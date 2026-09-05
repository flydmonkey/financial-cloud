<template>
  <div
    class="app-container"
    :class="{ 'salary-scope-page': salaryScope }"
  >
    <el-alert
      v-if="salaryScope"
      class="salary-scope-alert"
      type="info"
      show-icon
      :closable="false"
      title="工资凭证规则"
      description="以下为当前账套中与工资/劳务相关的凭证模板（如计提工资 jt_gz、劳务计提/发放等）。修改后将影响工资明细中的「生成凭证」。完整模板库请到「凭证模板」维护。"
    />
    <el-card
      v-if="!salaryScope"
      class="common-card query-box"
    >
      <div class="queryForm">
        <el-form
          v-show="showSearch"
          ref="queryRef"
          :model="queryParams"
          :inline="true"
          label-width="68px"
        >
          <el-form-item label="会计准则">
            <el-select
              v-model="queryParams.standardId"
              style="width: 200px"
              @change="handleQuery"
            >
              <el-option
                v-for="item in standardList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="类型：">
            <el-radio-group
              v-model="queryParams.category"
              @change="getList"
            >
              <el-radio-button :value="1">
                期末
              </el-radio-button>
              <el-radio-button :value="2">
                计提
              </el-radio-button>
              <el-radio-button :value="3">
                支付
              </el-radio-button>
              <el-radio-button :value="4">
                常规
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="common-card">
      <div
        v-if="salaryScope"
        class="salary-scope-toolbar"
      >
        <el-button
          type="primary"
          plain
          @click="goFullTemplate"
        >
          打开完整凭证模板
        </el-button>
        <el-button @click="getList">
          刷新
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="vouchertemplateList"
        border
        :tree-props="salaryScope ? undefined : { children: 'children', hasChildren: 'hasChildren' }"
        row-key="id"
        :default-expand-all="!salaryScope"
        :height="salaryScope ? undefined : 650"
        :empty-text="salaryScope ? '当前账套暂无工资/劳务相关凭证模板（常见编码：jt_gz、zf_gz、fp_lwf、zf_lwf）。可打开完整凭证模板查看或补齐。' : '暂无数据'"
        @cell-mouse-enter="cellMouseEnter"
        @cell-mouse-leave="cellMouseLeave"
      >
        <el-table-column
          label="编码"
          align="left"
          header-align="left"
          prop="code"
          min-width="120"
        />
        <el-table-column
          label="名称"
          align="left"
          header-align="left"
          prop="name"
          min-width="140"
        />
        <el-table-column
          label="字头"
          align="center"
          header-align="center"
          prop="wordHead"
          width="80"
        />
        <el-table-column
          label="备注"
          align="left"
          header-align="left"
          prop="remark"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          label="排序"
          align="center"
          header-align="center"
          prop="sortIndex"
          width="80"
        />
        <el-table-column
          label="操作"
          align="center"
          header-align="center"
          width="120"
          fixed="right"
        >
          <template #default="scope">
            <el-tooltip content="新增">
              <el-button
                type="primary"
                link
                :icon="Plus"
                @click="handleAdd(scope.row)"
              />
            </el-tooltip>
            <el-tooltip content="编辑">
              <el-button
                type="primary"
                link
                :icon="Edit"
                @click="handleEdit(scope.row)"
              />
            </el-tooltip>
            <el-tooltip content="移除">
              <el-button
                type="primary"
                link
                :icon="Delete"
                @click="handleDel(scope.row, 'asset')"
              />
            </el-tooltip>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-text">
            暂无数据
          </div>
          <el-button @click="handleAdd(null)">
            立即添加
          </el-button>
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :close-on-click-modal="false"
      width="800"
      style="margin-top: 30vh !important;"
    >
      <template #default>
        <el-form
          ref="voucherTemplateRef"
          :model="form"
          :items="items"
          label-width="68px"
          inline-message
        >
          <el-form-item
            label="ID"
            prop="id"
            style="display:none"
          >
            <el-input
              v-model="form.id"
              style="width: 300px"
              placeholder="请输入id"
            />
          </el-form-item>
          <el-form-item
            label="编码"
            prop="code"
            :required="true"
          >
            <el-input
              v-model="form.code"
              style="width: 300px"
              placeholder="请输入编码"
            />
          </el-form-item>
          <el-form-item
            label="名称"
            prop="name"
            :required="true"
          >
            <el-input
              v-model="form.name"
              style="width: 300px"
              placeholder="请输入名称"
            />
          </el-form-item>
          <el-form-item
            label="分类"
            prop="category"
            :required="true"
          >
            <el-select
              v-model="form.category"
              placeholder="选择"
              style="width: 300px"
            >
              <el-option
                label="期末"
                value="1"
              />
              <el-option
                label="计提"
                value="2"
              />
              <el-option
                label="支付"
                value="3"
              />
              <el-option
                label="常规"
                value="4"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="字头"
            prop="wordHead"
            :required="true"
          >
            <el-select
              v-model="form.wordHead"
              placeholder="选择"
              style="width: 300px"
            >
              <el-option
                label="记"
                value="记"
              />
              <el-option
                label="收"
                value="收"
              />
              <el-option
                label="付"
                value="付"
              />
              <el-option
                label="转"
                value="转"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="备注"
            prop="remark"
            :required="true"
          >
            <el-input
              v-model="form.remark"
              style="width: 300px"
              placeholder="请输入备注"
            />
          </el-form-item>
          <el-form-item
            label="排序"
            prop="sortIndex"
            :required="true"
          >
            <el-input
              v-model="form.sortIndex"
              style="width: 300px"
              placeholder="请输入排序"
            />
          </el-form-item>
          <el-table
            v-loading="loading"
            :data="form.items"
            border
            size="small"
            :cell-class-name="tableCellClassName"
            :row-style="{height: '46px'}"
            @cell-click="cellMouseEnter"
          >
            <el-table-column
              label="摘要"
              align="left"
              header-align="center"
              prop="summary"
            >
              <template #default="scope">
                <span v-if="!scope.row.editing || scope.row.columnIndex !== 0">
                  {{ scope.row.summary }}
                </span>
                <el-input
                  v-else
                  v-model="scope.row.summary"
                  placeholder="请输入备注"
                />
              </template>
            </el-table-column>      
            <el-table-column
              label="科目"
              align="left"
              header-align="center"
              prop="subjectCode"
            >
              <template #default="scope">
                <span v-if="!scope.row.editing || scope.row.columnIndex !== 1">
                  {{ formatSubjectLabel(scope.row.subjectCode, subjectKeyIdItem) }}
                </span>
                <el-cascader
                  v-else
                  v-model="scope.row.subjectCode"
                  style="width: 100%"
                  filterable
                  :options="subjectList"
                  :props="cascaderSubjectPropsOwn"
                  :filter-method="cascaderSubjectPropsOwn.filterMethod"
                  @change="handleSubjectChange(scope, $event)"
                  @visible-change="handleSubjectVisibleChange"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="借/贷"
              align="center"
              prop="direction"
              width="100"
            >
              <template #default="scope">
                <span v-if="!scope.row.editing || scope.row.columnIndex !== 2">
                  {{ scope.row.direction == "1"?"借":"贷" }}
                </span>
                <el-select
                  v-else
                  v-model="scope.row.direction"
                  placeholder="借/贷"
                  @blur="closeEditAll"
                >
                  <el-option
                    label="借"
                    value="1"
                  />
                  <el-option
                    label="贷"
                    value="2"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column
              label="取数规则"
              align="center"
              prop="rule"
            >
              <template #default="scope">
                <span v-if="!scope.row.editing || scope.row.columnIndex !== 3">
                  <dict-tag
                    :options="account_income_balance_type"
                    :value="scope.row.rule"
                  />
                </span>
                <el-select
                  v-else
                  v-model="scope.row.rule"
                  placeholder="选择"
                  @blur="closeEditAll"
                >
                  <el-option
                    v-for="dict in account_income_balance_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              align="center"
              width="110"
            >
              <template #default="scope">
                <el-popconfirm
                  title="确认删除吗？"
                  @confirm="form.items.splice(scope.$index, 1)"
                >
                  <template #reference>
                    <el-button
                      size="small"
                      type="danger"
                      link
                      :icon="Delete"
                    />
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-button
            :icon="Plus"
            style="width: 100%"
            @click="addTemplateItem"
          />
        </el-form>
      </template>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="dialog.visible = false">
            {{ t('org.cancel') }}
          </el-button>
          <el-button
            :loading="buttonLoading"
            type="primary"
            @click="submitForm"
          >
            {{ t('org.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReportBalanceSheet" lang="ts">
import {getCurrentQuarter, parseTime} from '@/utils/financialCloud'
import {getCurrentInstance, h, ref, shallowRef, reactive, toRefs, computed, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import bookStore from "@/store/modules/bookStore";
import {ElForm, FormInstance} from "element-plus";
import {cascaderSubjectProps, formatSubjectLabel, indexSubjectTree} from "@/utils/Subjects"
import * as voucherTemplateService from "@/api/voucher/voucher-template";
import {useI18n} from "vue-i18n";
import DictTag from "@/components/DictTag/index.vue";
import * as subjectApi from "@/api/standard/standard-subject";
import {Delete, Edit, Plus} from '@element-plus/icons-vue'
import {listStandardsAll} from "@/api/standard/standard";

/** 工资模块实际用到的模板编码 + 名称启发 */
const SALARY_TEMPLATE_CODES = new Set(['jt_gz', 'zf_gz', 'fp_lwf', 'zf_lwf'])

function isSalaryTemplate(row: any): boolean {
  if (!row) {
    return false
  }
  if (row.code && SALARY_TEMPLATE_CODES.has(String(row.code))) {
    return true
  }
  const name = String(row.name || '')
  return name.includes('工资') || name.includes('劳务') || name.includes('薪资')
}

const {t} = useI18n()
const route = useRoute()
const router = useRouter()
const {proxy} = getCurrentInstance();
const {account_income_balance_type} = proxy?.useDict("account_income_balance_type");
const currBookStore = bookStore()
const salaryScope = computed(() =>
  route.path.includes('salary-voucher-rules') || route.query.scope === 'salary'
)
const cascaderSubjectPropsOwn = ref<any>({...cascaderSubjectProps})
cascaderSubjectPropsOwn.value.checkStrictly = true
// 会计科目数据
const subjectList = ref<any>([])
const vouchertemplateList = ref<any>([]);
const subjectKeyIdItem = ref<any>({})
const loading = ref(true);
const buttonLoading = ref(false);
const showSearch = ref(true);
const level = ref(1);
const visibleSubjectStatus = ref(false);
//会计准则
const standardList: any = ref<any>([]);
const dialog = reactive<any>({
  visible: false,
  title: ''
});
const voucherTemplateRef = ref<FormInstance>();
const initFormData: any = {
  bookId: currBookStore.bookId,
  parentCode: undefined,
  sortIndex: 1,
  itemName: "",
  items: [{
    direction: 1,
    subjectCode: '',
    summary: '',
  }]
}
const data = reactive({
  form: {...initFormData},
  queryParams: {
    periodType: 'month',
    standardId:'',
    category:1,
    date: parseTime(new Date(), "{y}-{m}"),
    reportQuarter: getCurrentQuarter(),
    reportDate: parseTime(new Date(), "{y}-{m}"),
  },
  items: {
    itemCode: [
      {required: true, message: '编码不能为空', trigger: 'blur'}
    ],
    itemName: [
      {required: true, message: '名称不能为空', trigger: 'blur'}
    ],
  }
});

const {queryParams, form, items} = toRefs(data);

const customPrefix = shallowRef({
  render() {
    return h('p', '年')
  },
})

const disabledDate = (time: any) => {
  const now = new Date();
  return time.getTime() > now.getTime(); // 禁用过去的日期
}

function resolveTemplateRelatedId(): string {
  if (salaryScope.value) {
    return currBookStore.bookId || ''
  }
  return queryParams.value.standardId
}

function resolveSubjectTreeQuery(): Record<string, string> | null {
  if (salaryScope.value) {
    const bookId = currBookStore.bookId
    return bookId ? { bookId } : null
  }
  const standardId = queryParams.value.standardId
  return standardId ? { standardId } : null
}

function getSubjectList() {
  const query = resolveSubjectTreeQuery()
  if (!query) {
    subjectList.value = []
    subjectKeyIdItem.value = {}
    return
  }
  subjectApi.getTree(query).then((res: any) => {
    subjectList.value = res.data || []
    subjectKeyIdItem.value = {}
    updateSubjectKeys(subjectList.value)
  }).catch(() => {
    subjectList.value = []
    subjectKeyIdItem.value = {}
  })
}

/** 查询列表 */
function getList() {
  loading.value = true;
  const relatedId = resolveTemplateRelatedId()
  if (!relatedId) {
    vouchertemplateList.value = []
    loading.value = false
    return
  }
  // salary scope: category=0 表示不按类型过滤，再在前端筛工资相关
  const category = salaryScope.value ? 0 : queryParams.value.category
  voucherTemplateService.list(relatedId, category).then((response: any) => {
    let rows = response.data?.records || response.data || []
    if (!Array.isArray(rows)) {
      rows = []
    }
    if (salaryScope.value) {
      rows = rows.filter(isSalaryTemplate)
    }
    vouchertemplateList.value = rows
    loading.value = false
  }).catch(() => {
    vouchertemplateList.value = []
    loading.value = false
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

function goFullTemplate() {
  router.push({ path: '/voucher/voucher-template' })
}

function handleExport() {

}

function closeEditAll() {
  form.value.items.forEach((item: any) => {
    item.editing = false
  })
}

const cellMouseEnter = (row: any, column: any, cell: HTMLTableCellElement, event: Event) => {
    closeEditAll()
    row.columnIndex = column.index
    row.editing = true

  event.stopPropagation()
}

const cellMouseLeave = (row: any, column: any, cell: HTMLTableCellElement, event: Event) => {
  row.editing = false
}

// 更新会计科目ID关联
const updateSubjectKeys = (items: any) => {
  indexSubjectTree(items, subjectKeyIdItem.value)
}

const handleSubjectChange = (scope: any, value: any) => {
  const code = value != null ? String(value) : String(scope.row.subjectCode || '')
  const subject = subjectKeyIdItem.value[code]
  if (subject) {
    scope.row.subjectId = subject.id
    scope.row.subjectCode = code
  }
}

function addTemplateItem() {
  form.value.items.push({
    direction: 1,
    subjectCode: '',
    summary: '',
  })
}

const handleSubjectVisibleChange = (show: any) => {
  if (show) {
    visibleSubjectStatus.value = true
    setTimeout(() => {
      visibleSubjectStatus.value = false
    }, 500)
  }
}

/** 表单重置 */
const reset = () => {
  form.value = {...initFormData};
  voucherTemplateRef.value?.resetFields();
};

function handleAdd(row?: any) {
  reset();
  form.value.relatedId = resolveTemplateRelatedId();
  dialog.visible = true;
  dialog.title = "添加";
}

function handleEdit(row: any) {
  reset();
  const relatedId = resolveTemplateRelatedId();
  voucherTemplateService.get(relatedId,row.id).then((res: any) => {
    form.value = res.data
    if (!form.value.items || form.value.items.length === 0) {
      form.value.items = [{}]
    }
    dialog.visible = true;
    dialog.title = "修改";
  })
}

/**
 * 删除
 * @param row
 * @param assetOrLiability
 */
function handleDel(row: any, assetOrLiability?: string) {
  voucherTemplateService.del(row.id).then(() => {
    proxy?.$modal.msgSuccess('删除成功');
    getList();
  })
}

/** 提交按钮 */
const submitForm = () => {
  voucherTemplateRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.items = form.value.items.filter((item: any) => {
        return item.subjectCode && item.summary && item.direction
      })
      form.value.relatedId =form.value.relatedId|| resolveTemplateRelatedId();

      buttonLoading.value = true;
      await voucherTemplateService.save(form.value).finally(() => buttonLoading.value = false);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

function tableCellClassName({row, column, rowIndex, columnIndex}: any) {
  //注意这里是解构
  //利用单元格的 className 的回调方法，给行列索引赋值
  row.index = rowIndex;
  column.index = columnIndex;
  return ""
}

/*获取准则列表*/
function getStandardList() {
  if (salaryScope.value) {
    const book = currBookStore.getBookItem?.()
    if (book?.standardId) {
      queryParams.value.standardId = book.standardId
    }
    getSubjectList();
    getList();
    return
  }
  listStandardsAll({}).then((res: any) => {
    if (res.code === 0) {
      if (Array.isArray(res.data) && res.data.length > 0) {
        standardList.value = res.data;
        queryParams.value.standardId = standardList.value[0].id;
      } else {
        // 如果数据为空数组时，确保有默认处理逻辑
        standardList.value = [];
        queryParams.value.standardId = ''; // 或设置为适当的默认值
      }
      getSubjectList();
      getList(); // 确保在赋值完成后调用
    }
  });
}

getStandardList();

// 账套异步就绪后，薪资范围需重新拉账套科目与模板（否则科目只显示编码）
watch(
  () => currBookStore.bookId,
  (bookId, prev) => {
    if (!salaryScope.value || !bookId || bookId === prev) {
      return
    }
    getSubjectList()
    getList()
  }
)

</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.btn-form {
  margin-bottom: 10px;
}

.common-card {
  margin-bottom: 15px;
}

.salary-scope-alert {
  margin-bottom: 12px;
}

.salary-scope-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.salary-scope-page {
  .common-card {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  .el-table {
    width: 100%;
  }
}
</style>

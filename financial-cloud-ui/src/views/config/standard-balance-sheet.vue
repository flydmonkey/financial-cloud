<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <div class="queryForm" style="display: flex;justify-content: space-between;">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="会计准则">
            <el-select v-model="queryParams.standardId" @change="handleQuery" style="width: 200px">
              <el-option
                  v-for="item in standardList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id">
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
        <div class="btn-form-right">
          <el-button type="primary" @click="handleAdd(null, 'asset')">添加一级资产项</el-button>
          <el-button type="success" @click="handleAdd(null, 'liability')">添加一级负债和所有者权益项</el-button>
        </div>
      </div>
    </el-card>
    <el-card class="common-card">

      <el-table v-loading="loading" :data="balanceSheetList" border
                :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
                row-key="id" default-expand-all
                height="640">
        <el-table-column label="编码" align="left" prop="itemCode" width="120"/>
        <el-table-column label="资产" align="left" header-align="left" prop="itemName">
          <template #default="scope">
            <span :style="{'text-indent': scope.row.level + 'em',
             display: 'inline-block', 'margin-right': '30px', fontWeight: scope.row.level === 1 ? 'bold' : ''}">
              {{ scope.row.symbol === '-' ? '减：' : '' }}{{ scope.row.itemName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="行次" align="center" prop="sortIndex" width="100"/>
        <el-table-column label="操作" align="left" header-align="center" width="120" prop="itemName">
          <template #default="scope">
            <div v-if="scope.row.itemName != ''">
              <el-tooltip v-if="scope.row.level > 1 || (scope.row.itemCode && scope.row.itemCode.endsWith('00'))"
                          content="新增">
                <el-button type="primary"
                           link @click="handleAdd(scope.row, 'asset')" icon="Plus"></el-button>
              </el-tooltip>
              <el-tooltip v-if="scope.row.level > 1 || (scope.row.itemCode && !scope.row.itemCode.endsWith('00'))"
                          content="编辑">
                <el-button type="primary"
                           link @click="handleEdit(scope.row, 'asset')" icon="Edit"></el-button>
              </el-tooltip>
              <el-tooltip content="移除">
                <el-button type="primary"
                           link @click="handleDel(scope.row, 'asset')" icon="Delete"></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="编码" align="left" width="120" prop="liabilityItemCode"/>
        <el-table-column label="负债和所有者权益" header-align="left" align="left" prop="liabilityItemName">
          <template #default="scope">
            <span :style="{'text-indent': scope.row.liabilityLevel + 'em',
             display: 'inline-block', 'margin-right': '30px', fontWeight: scope.row.liabilityLevel === 1 ? 'bold' : ''}">
              {{ scope.row.liabilitySymbol === '-' ? '减：' : '' }}{{ scope.row.liabilityItemName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="行次" align="center" width="100" prop="liabilitySortIndex"/>
        <el-table-column label="操作" align="left" header-align="center" width="120"
                         prop="liabilityItemName">
          <template #default="scope">
            <div v-if="scope.row.liabilityItemName !='' ">
              <el-tooltip
                  v-if="scope.row.liabilityLevel > 1 || (scope.row.liabilityItemCode && scope.row.liabilityItemCode.endsWith('00'))"
                  content="新增">
                <el-button type="primary"
                           link @click="handleAdd(scope.row, 'liability')" icon="Plus"></el-button>
              </el-tooltip>
              <el-tooltip
                  v-if="scope.row.liabilityLevel > 1 || (scope.row.liabilityItemCode && !scope.row.liabilityItemCode.endsWith('00'))"
                  content="编辑">
                <el-button type="primary"
                           link @click="handleEdit(scope.row, 'liability')" icon="Edit"></el-button>
              </el-tooltip>
              <el-tooltip content="移除">
                <el-button type="primary"
                           link @click="handleDel(scope.row, 'liability')" icon="Delete"></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-text">暂无数据</div>
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :close-on-click-modal="false" width="800" style="margin-top: 30vh !important;">
      <template #default>
        <el-form :model="form" :rules="rules" ref="balanceSheetRef" label-width="80px"
                 inline-message>
          <el-form-item v-if="!form.id" label="级别" prop="level">
            <el-radio-group :disabled="!form.parentItemCode || level === 1" v-model="form.level">
              <el-radio-button label="本级" :value="level"/>
              <el-radio-button label="下级" :value="level + 1"/>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="编码" v-if="form.level > 1 || emptyPush" prop="itemCode">
            <div>
              <el-input style="width: 300px" v-model="form.itemCode" placeholder="请输入编码"/>
              <div style="line-height: 1.2em;color: #888888;">
                提示：编码长度4位，编码规则为：xxxx；<br />
                一级项末尾只允许以00/99结尾；<br />
                统计项编码规则：xx99_xx99_xx99。
              </div>
            </div>
          </el-form-item>
          <el-form-item label="名称" v-if="form.level > 1 || emptyPush" prop="itemName">
            <el-input style="width: 300px" v-model="form.itemName" placeholder="请输入名称"/>
          </el-form-item>
          <el-form-item v-if="form.level === 2" label="计算" prop="symbol">
            <el-radio-group v-model="form.symbol">
              <el-radio-button label="加" value="+"/>
              <el-radio-button label="减" value="-"/>
              <el-radio-button label="函数" value="f"/>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.level > 1" label="取数规则" prop="rule">
            <el-radio-group v-model="form.rule">
              <el-radio-button label="根据科目取数" value="1"/>
              <el-radio-button label="手动录入" value="2"/>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="行号" prop="sortIndex"
                        v-if="form.level > 1 || (form.level===1 && form.itemCode && form.itemCode.endsWith('99'))">
            <el-input-number :min="1" v-model="form.sortIndex" placeholder="请输入"/>
          </el-form-item>

          <el-table
              v-if="form.itemName && form.rule === '1' && form.level > 1"
              v-loading="loading" :data="form.rules" border size="small"
              :cell-class-name="tableCellClassName"
              @cell-click="cellMouseEnter"
              :row-style="{height: '46px'}">
            <el-table-column label="科目" align="left" header-align="center" prop="subjectCode" width="200">
              <template #default="scope">
                <span v-if="!scope.row.editing || scope.row.columnIndex !== 0">
                  {{ subjectKeyIdItem[scope.row.subjectCode]?.name }}
                </span>
                <el-cascader v-else style="width: 100%" filterable
                             v-model="scope.row.subjectCode"
                             :options="subjectList"
                             :props="cascaderSubjectPropsOwn"
                             @change="handleSubjectChange(scope, $event)"
                             :filter-method="cascaderSubjectPropsOwn.filterMethod"
                             @visible-change="handleSubjectVisibleChange"/>
              </template>
            </el-table-column>
            <el-table-column label="计算" align="center" prop="symbol" width="100">
              <template #default="scope">
                <span v-if="!scope.row.editing || scope.row.columnIndex !== 1">
                 {{ scope.row.symbol }}
                </span>
                <el-select v-else v-model="scope.row.symbol" placeholder="选择" @blur="closeEditAll">
                  <el-option label="+" value="+"></el-option>
                  <el-option label="-" value="-"></el-option>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="取数规则" align="center" prop="rule">
              <template #default="scope">
                 <span v-if="!scope.row.editing || scope.row.columnIndex !== 2">
                   <dict-tag :options="account_balance_type" :value="scope.row.rule"></dict-tag>
                </span>
                <el-select v-else v-model="scope.row.rule" placeholder="选择" @blur="closeEditAll">
                  <el-option
                      v-for="dict in account_balance_type"
                      :key="dict.value"
                      :label="dict.label"
                      :value="dict.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="110">
              <template #default="scope">
                <el-popconfirm title="确认删除吗？" @confirm="form.rules.splice(scope.$index, 1)">
                  <template #reference>
                    <el-button size="small" icon="Delete"></el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-button v-if="form.itemName && form.rule === '1' && form.level > 1"
                     icon="Plus" style="width: 100%" @click="handleFormRules"></el-button>
        </el-form>
      </template>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="dialog.visible = false">{{ t('org.cancel') }}</el-button>
          <el-button :loading="buttonLoading" type="primary" @click="submitForm">{{ t('org.confirm') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReportBalanceSheet" lang="ts">
import {getCurrentInstance, h, ref, reactive, toRefs} from 'vue'
import {ElForm, FormInstance} from "element-plus";
import {cascaderSubjectProps} from "@/utils/Subjects"
import {
  saveConfigBalanceSheet,
  getConfigBalanceSheet,
  delConfigBalanceSheet,
  listConfigBalanceSheet
} from "@/api/system/standard/standard-statement-balance-sheet";
import {useI18n} from "vue-i18n";
import DictTag from "@/components/DictTag/index.vue";
import * as subjectApi from "@/api/system/standard/standard-subject";
import {listStandardsAll} from "@/api/system/standard/standard";

const cascaderSubjectPropsOwn = ref<any>({...cascaderSubjectProps})
cascaderSubjectPropsOwn.value.checkStrictly = true

const {t} = useI18n()
const {proxy} = getCurrentInstance();
const {account_balance_type} = proxy?.useDict("account_balance_type");
// 会计科目数据
const subjectList = ref<any>([])
const balanceSheetList = ref<any>([]);
const assetsList = ref<Record<string, any>>({});
const liabilityList = ref<Record<string, any>>({});
const subjectKeyIdItem = ref<any>({})
const loading = ref(true);
const buttonLoading = ref(false);
const showSearch = ref(true);
const level = ref(1);
const emptyPush = ref(false);
const visibleSubjectStatus = ref(false);
//会计准则
const standardList: any = ref<any>([]);
const dialog = reactive<any>({
  visible: false,
  title: ''
});
const balanceSheetRef = ref<FormInstance>();

const initFormData: any = {
  standardId: '',
  itemCode: undefined,
  parentItemCode: undefined,
  assetOrLiability: "asset",
  sortIndex: 1,
  level: 1,
  itemName: "",
  rule: "1",
  symbol: "+",
  rules: []
}

const checkItemName = async (rule: any, value: any, callback: any) => {
  if (!value) {
    return callback(new Error('名称不能为空'));
  }
  if (form.value.itemCode && form.value.itemCode.endsWith('99')) {
    if (!value.startsWith('合计：')) {
      return callback(new Error('合计项名称必须以“合计：”开头'));
    }
    if (value.length < 4) {
      return callback(new Error('合计项名称长度不能低于4个字符'));
    }
  }
  callback();
}
const checkItemCode = async (rule: any, value: any, callback: any) => {
  if (!value) {
    return callback(new Error('编码不能为空'));
  }
  if (form.value.level === 1) {
    const codes = value.split('_');
    if (codes.length === 1) {
      if (!codes[0].endsWith('00') && !codes[0].endsWith('99')) {
        return callback(new Error('一级编码格式错误，必须以00/99结尾'));
      }
    } else {
      for (let i = 0; i < codes.length; i++) {
        if (!codes[i].endsWith('99')) {
          return callback(new Error('统计项一级编码格式错误，必须以99结尾'));
        }
      }
    }
  } else {
    const codes = value.split('_');
    if (codes.length > 1) {
      return callback(new Error('编码格式错误，只允许xxxx格式'));
    }
    if (codes[0].endsWith('99') || codes[0].endsWith('00')) {
      return callback(new Error('编码格式错误，不能以00/99结尾'));
    }
  }

  if (form.value.assetOrLiability === "asset") {
    const item = assetsList.value[value]
    if (item && item.id !== form.value.id) {
      return callback(new Error('编码已存在'));
    }
  } else {
    const item = liabilityList.value[value]
    if (item && item.id !== form.value.id) {
      return callback(new Error('编码已存在'));
    }
  }

  callback();
}
const data = reactive({
  form: {...initFormData},
  queryParams: {
    standardId: '',
  },
  rules: {
    itemCode: [
      {required: true, message: '编码不能为空', trigger: 'blur'},
      {
        pattern: /^(\d{4}|(\d{4}_\d{4})|(\d{4}_\d{4}_\d{4}))$/,
        message: '编码格式错误，应为xxxx或xxxx_xxxx格式',
        trigger: 'blur'
      },
      {validator: checkItemCode, trigger: 'blur'}
    ],
    itemName: [
      {required: true, message: '名称不能为空', trigger: 'blur'},
      {validator: checkItemName, trigger: 'blur'}
    ],
    assetOrLiability: [
      {required: true, message: '项目类型不能为空', trigger: 'blur'}
    ],
    level: [
      {required: true, message: '节点级别不能为空', trigger: 'blur'}
    ],
    rule: [
      {required: true, message: '取数规则不能为空', trigger: 'blur'}
    ],
    symbol: [
      {required: true, message: '合计规则不能为空', trigger: 'blur'}
    ],
  }
});

const {queryParams, form, rules} = toRefs(data);

/** 查询列表 */
function getList() {
  loading.value = true;
  listConfigBalanceSheet(queryParams.value.standardId).then((response: any) => {
    const list: any = []
    const mapList: any = {}
    assetsList.value = {}
    liabilityList.value = {}
    const assets = response.data.assets
    const liability = response.data.liability
    assets.forEach((item: any, index: number) => {
      assetsList.value[item.itemCode] = item
    })
    liability.forEach((item: any, index: number) => {
      liabilityList.value[item.itemCode] = item
    })

    const maxData = assets.length > liability.length ? assets : liability

    maxData.forEach((item: any, index: number) => {
      if (!mapList[index]) {
        mapList[index] = [assets[index] || undefined, liability[index] || undefined]
      }
    })
    for (let key in mapList) {
      const item = mapList[key]
      list.push({
        //资产
        id: key,
        itemCode: item[0]?.itemCode || '',
        itemName: item[0]?.itemName || '',
        sortIndex: item[0]?.sortIndex,
        level: item[0]?.level,
        symbol: item[0]?.symbol,
        //负债和所有者权益
        liabilityItemCode: item[1]?.itemCode || '',
        liabilityItemName: item[1]?.itemName || '',
        liabilitySortIndex: item[1]?.sortIndex,
        liabilityLevel: item[1]?.level,
        liabilitySymbol: item[1]?.symbol,
        item: item
      })
    }
    balanceSheetList.value = list
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

function closeEditAll() {
  form.value.rules.forEach((item: any) => {
    item.editing = false
  })
}

const cellMouseEnter = (row: any, column: any, cell: HTMLTableCellElement, event: Event) => {
  if (!visibleSubjectStatus.value) {
    closeEditAll()
    row.columnIndex = column.index
    row.editing = true
  }
  event.stopPropagation()
}

// 更新会计科目ID关联
const updateSubjectKeys = (items: any) => {
  for (let valueKey in items) {
    const item = items[valueKey]
    subjectKeyIdItem.value[item.code] = item
    if (item.children && item.children.length > 0) {
      updateSubjectKeys(item.children)
    }
  }
}

function getSubjectList() {
  //传入当前账套ID
  subjectApi.getTree({standardId: queryParams.value.standardId}).then((res: any) => {
    subjectList.value = res.data
    updateSubjectKeys(subjectList.value)
  })
}

const handleSubjectChange = (scope: any, value: any) => {
  const subject = subjectKeyIdItem.value[scope.row.subjectCode]
  scope.row.subjectId = subject.id
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
  balanceSheetRef.value?.resetFields();
};

/**
 * 新增节点，不可增加根节点
 * @param row
 * @param assetOrLiability
 */
function handleAdd(row?: any, assetOrLiability?: string) {
  reset();
  if (row) {
    if (assetOrLiability === 'asset') {
      form.value.parentItemCode = row.item[0]?.itemCode;
      form.value.level = row.item[0] ? row.item[0].level : 1;
      form.value.sortIndex = row.item[0] ? row.item[0].sortIndex + 1 : 1;
    } else {
      form.value.parentItemCode = row.item[1]?.itemCode;
      form.value.level = row.item[1] ? row.item[1].level : 1;
      form.value.sortIndex = row.item[1] ? row.item[1].sortIndex + 1 : 1;
    }
    level.value = form.value.level
    if (form.value.level === 1) {
      form.value.level = 2
    }
    form.value.standardId = queryParams.value.standardId
    form.value.assetOrLiability = assetOrLiability
    emptyPush.value = false
    getSubjectList()
    dialog.visible = true;
    dialog.title = "添加";
  } else {
    emptyPush.value = true
    getSubjectList()
    level.value = 1
    form.value.assetOrLiability = assetOrLiability
    form.value.standardId = queryParams.value.standardId
    form.value.level = level.value
    form.value.parentItemCode = null
    form.value.sortIndex = null
    form.value.rule = null
    form.value.symbol = null
    dialog.visible = true;
    dialog.title = "添加跟";
  }
}

function handleEdit(row: any, assetOrLiability?: string) {
  reset();
  getSubjectList()
  getConfigBalanceSheet(queryParams.value.standardId, assetOrLiability === 'asset' ? row.item[0]?.itemCode : row.item[1]?.itemCode).then((res: any) => {
    form.value = res.data
    if (!form.value.rules || form.value.rules.length === 0) {
      form.value.rules = []
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
  delConfigBalanceSheet(assetOrLiability === 'asset' ? row.item[0]?.id : row.item[1]?.id).then(() => {
    proxy?.$modal.msgSuccess('删除成功');
    getList();
  })
}

/** 提交按钮 */
const submitForm = () => {
  balanceSheetRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.rules = form.value.rules.filter((item: any) => {
        return item.subjectCode && item.symbol && item.rule
      })
      buttonLoading.value = true;
      await saveConfigBalanceSheet(form.value).finally(() => buttonLoading.value = false);
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

function handleFormRules() {
  form.value.rules.push({
    subjectCode: "",
    symbol: "+",
    rule: "BALANCE"
  })
}

getStandardList();

</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}
</style>

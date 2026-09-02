<template>
  <div class="app-container fixed-asset-card">
    <el-row :gutter="12">
      <el-col :span="4">
        <el-card class="common-card category-side">
          <div class="side-title">
            资产类别
          </div>
          <el-menu
            :default-active="queryParams.categoryId || 'ALL'"
            @select="onCategorySelect"
          >
            <el-menu-item index="ALL">
              全部
            </el-menu-item>
            <el-menu-item
              v-for="c in categories"
              :key="c.id"
              :index="c.id"
            >
              {{ c.name }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="20">
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
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="名称">
              <el-input
                v-model="queryParams.name"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="statusFilter"
                style="width: 140px"
                @change="onStatusFilter"
              >
                <el-option
                  label="使用中"
                  value="IN_USE"
                />
                <el-option
                  label="含暂停"
                  value="ACTIVE"
                />
                <el-option
                  label="已暂停"
                  value="SUSPENDED"
                />
                <el-option
                  label="含已清理"
                  value="ALL"
                />
                <el-option
                  label="已清理"
                  value="DISPOSED"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleQuery">
                查询
              </el-button>
              <el-button
                type="primary"
                @click="handleAdd"
              >
                新增
              </el-button>
              <el-button @click="handleExport">
                导出
              </el-button>
              <el-button @click="handleDownloadTemplate">
                下载模板
              </el-button>
              <el-upload
                :show-file-list="false"
                :http-request="importExcel"
                :before-upload="beforeImportUpload"
                accept=".xls,.xlsx"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button>
                  导入
                </el-button>
              </el-upload>
              <el-button @click="goDepreciation">
                生成凭证
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="common-card">
          <el-table
            v-loading="loading"
            border
            :data="list"
            show-summary
            :summary-method="summaryMethod"
          >
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
              prop="categoryName"
              label="类别"
              width="100"
            />
            <el-table-column
              prop="deptName"
              label="部门"
              width="100"
              show-overflow-tooltip
            />
            <el-table-column
              prop="methodLabel"
              label="折旧方法"
              width="110"
              align="center"
            />
            <el-table-column
              prop="originalValue"
              label="原值"
              width="110"
              align="right"
            />
            <el-table-column
              prop="accumDepr"
              label="累计折旧"
              width="110"
              align="right"
            />
            <el-table-column
              prop="endingNetValue"
              label="净值"
              width="110"
              align="right"
            />
            <el-table-column
              prop="monthlyDepr"
              label="月折旧"
              width="100"
              align="right"
            />
            <el-table-column
              label="状态"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                {{ statusLabel(row.status) }}
              </template>
            </el-table-column>
            <el-table-column
              label="购入凭证"
              width="110"
              align="center"
            >
              <template #default="{ row }">
                <el-link
                  v-if="row.purchaseVoucherId"
                  type="primary"
                  @click="goVoucher(row.purchaseVoucherId)"
                >
                  {{ row.purchaseVoucherWord || '查看' }}
                </el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column
              label="清理凭证"
              width="110"
              align="center"
            >
              <template #default="{ row }">
                <el-link
                  v-if="row.disposeVoucherId"
                  type="primary"
                  @click="goVoucher(row.disposeVoucherId)"
                >
                  {{ row.disposeVoucherWord || '查看' }}
                </el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="200"
              fixed="right"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  link
                  @click="handleUpdate(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 'IN_USE'"
                  link
                  type="warning"
                  @click="handleSuspend(row)"
                >
                  暂停
                </el-button>
                <el-button
                  v-if="row.status === 'SUSPENDED'"
                  link
                  type="success"
                  @click="handleResume(row)"
                >
                  恢复
                </el-button>
                <el-dropdown
                  trigger="click"
                  @command="(cmd: string) => onRowCommand(cmd, row)"
                >
                  <el-button link>
                    更多
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="copy">
                        复制
                      </el-dropdown-item>
                      <el-dropdown-item command="change">
                        变动
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="row.status !== 'DISPOSED'"
                        command="dispose"
                      >
                        清理
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="delete"
                        divided
                      >
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
      </el-col>
    </el-row>

    <el-drawer
      v-model="dialog.visible"
      :close-on-click-modal="false"
      size="720px"
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
        <el-divider content-position="left">
          基本信息
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item
              label="资产编码"
              prop="code"
            >
              <el-input v-model="form.code" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="资产名称"
              prop="name"
            >
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="资产类别"
              prop="categoryId"
            >
              <el-select
                v-model="form.categoryId"
                style="width: 100%"
                filterable
                @change="onCategoryChange"
              >
                <el-option
                  v-for="c in categories"
                  :key="c.id"
                  :label="c.name"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="使用部门"
              prop="deptId"
            >
              <el-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :props="deptProps"
                check-strictly
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="开始使用日期"
              prop="startUseDate"
            >
              <el-date-picker
                v-model="form.startUseDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量">
              <el-input-number
                v-model="form.quantity"
                :min="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="form.spec" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放地点">
              <el-input v-model="form.location" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          折旧方法
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item
              label="折旧方法"
              prop="depreciationMethod"
            >
              <el-select
                v-model="form.depreciationMethod"
                style="width: 100%"
                :disabled="form.calcFieldsLocked"
                @change="onDepreciationMethodChange"
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
          </el-col>
          <el-col
            v-if="form.depreciationMethod === 'STRAIGHT_LINE'
              || form.depreciationMethod === 'DOUBLE_DECLINING'
              || form.depreciationMethod === 'SUM_OF_YEARS'"
            :span="12"
          >
            <el-form-item
              label="预计使用期数(月)"
              prop="usefulLifeMonths"
            >
              <el-input-number
                v-model="form.usefulLifeMonths"
                :min="1"
                :disabled="form.calcFieldsLocked"
                style="width: 100%"
              />
              <div
                v-if="isAcceleratedMethod"
                class="form-tip"
              >
                加速折旧要求 ≥24 个月且为完整年数（12 的整数倍，如 24 / 36 / 60）
              </div>
            </el-form-item>
          </el-col>
          <el-col
            v-else-if="form.depreciationMethod === 'UNITS_OF_PRODUCTION'"
            :span="12"
          >
            <el-form-item label="预计总工作量">
              <el-input-number
                v-model="form.expectedTotalWork"
                :min="0"
                :precision="2"
                :disabled="form.calcFieldsLocked"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="净残值率%">
              <el-input-number
                v-model="form.residualRate"
                :min="0"
                :max="100"
                :precision="2"
                :disabled="form.calcFieldsLocked"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          原值 / 净值 / 累计
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item
              label="原值"
              prop="originalValue"
            >
              <el-input-number
                v-model="form.originalValue"
                :min="0"
                :precision="2"
                :disabled="form.calcFieldsLocked"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税额">
              <el-input-number
                v-model="form.taxAmount"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="减值准备">
              <el-input-number
                v-model="form.impairment"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期初累计折旧">
              <el-input-number
                v-model="form.openingAccumDepr"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已折旧期数">
              <el-input-number
                v-model="form.depreciatedPeriods"
                :min="0"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          凭证科目
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="固定资产科目">
              <el-tree-select
                v-model="form.fixedAssetSubjectId"
                :data="subjectOptions"
                :props="subjectProps"
                check-strictly
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="累计折旧科目">
              <el-tree-select
                v-model="form.accumDeprSubjectId"
                :data="subjectOptions"
                :props="subjectProps"
                check-strictly
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="折旧费用科目"
              prop="expenseSubjectId"
            >
              <el-tree-select
                v-model="form.expenseSubjectId"
                :data="subjectOptions"
                :props="subjectProps"
                check-strictly
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购入对方科目">
              <el-tree-select
                v-model="form.purchaseCounterpartSubjectId"
                :data="subjectOptions"
                :props="subjectProps"
                check-strictly
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税金科目">
              <el-tree-select
                v-model="form.taxSubjectId"
                :data="subjectOptions"
                :props="subjectProps"
                check-strictly
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="form.purchaseVoucherId || form.disposeVoucherId">
          <el-divider content-position="left">
            关联凭证
          </el-divider>
          <el-row :gutter="12">
            <el-col
              v-if="form.purchaseVoucherId"
              :span="12"
            >
              <el-form-item label="购入凭证">
                <el-link
                  type="primary"
                  @click="goVoucher(form.purchaseVoucherId)"
                >
                  {{ form.purchaseVoucherWord || form.purchaseVoucherId }}
                </el-link>
              </el-form-item>
            </el-col>
            <el-col
              v-if="form.disposeVoucherId"
              :span="12"
            >
              <el-form-item label="清理凭证">
                <el-link
                  type="primary"
                  @click="goVoucher(form.disposeVoucherId)"
                >
                  {{ form.disposeVoucherWord || form.disposeVoucherId }}
                </el-link>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-divider content-position="left">
          备注
        </el-divider>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
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

    <el-dialog
      v-model="changeDialog.visible"
      title="资产变动"
      width="560px"
    >
      <el-form label-width="120px">
        <el-form-item label="资产">
          {{ changeDialog.assetCode }} {{ changeDialog.assetName }}
        </el-form-item>
        <el-form-item label="变动项">
          <el-select
            v-model="changeForm.fieldCode"
            style="width: 100%"
            @change="onChangeField"
          >
            <el-option
              v-for="f in changeFields"
              :key="f.code"
              :label="f.label"
              :value="f.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变动前">
          <el-input
            v-model="changeForm.beforeValue"
            disabled
          />
        </el-form-item>
        <el-form-item label="变动后">
          <el-select
            v-if="changeForm.fieldCode === 'depreciationMethod'"
            v-model="changeForm.afterValue"
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
          <el-input
            v-else
            v-model="changeForm.afterValue"
            placeholder="请输入变动后内容"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="changeForm.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialog.visible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="changeLoading"
          @click="submitChange"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="disposeDialog.visible"
      title="资产清理"
      width="560px"
    >
      <el-form
        label-width="120px"
      >
        <el-form-item label="资产">
          {{ disposeDialog.assetCode }} {{ disposeDialog.assetName }}
        </el-form-item>
        <el-form-item label="账面价值">
          {{ formatMoney(disposeDialog.bookValue) }}
        </el-form-item>
        <el-form-item label="处置收入">
          <el-input-number
            v-model="disposeForm.disposeIncome"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="清理费用">
          <el-input-number
            v-model="disposeForm.disposeExpense"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="对方科目">
          <el-tree-select
            v-model="disposeForm.counterpartSubjectId"
            :data="subjectOptions"
            :props="subjectProps"
            check-strictly
            filterable
            clearable
            style="width: 100%"
            placeholder="银行存款等（有收入/费用时必填）"
          />
        </el-form-item>
        <el-form-item label="清理科目">
          <el-tree-select
            v-model="disposeForm.disposalSubjectId"
            :data="subjectOptions"
            :props="subjectProps"
            check-strictly
            filterable
            clearable
            style="width: 100%"
            placeholder="默认 1606/1701"
          />
        </el-form-item>
        <el-form-item label="净收益科目">
          <el-tree-select
            v-model="disposeForm.gainSubjectId"
            :data="subjectOptions"
            :props="subjectProps"
            check-strictly
            filterable
            clearable
            style="width: 100%"
            placeholder="默认 5301.01"
          />
        </el-form-item>
        <el-form-item label="净损失科目">
          <el-tree-select
            v-model="disposeForm.lossSubjectId"
            :data="subjectOptions"
            :props="subjectProps"
            check-strictly
            filterable
            clearable
            style="width: 100%"
            placeholder="默认 5711.02"
          />
        </el-form-item>
        <el-form-item label="凭证字">
          <el-input
            v-model="disposeForm.voucherWord"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="disposeForm.summary"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="将生成一张清理凭证，清理当月仍计提折旧、次月停提。"
        />
      </el-form>
      <template #footer>
        <el-button @click="disposeDialog.visible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="disposeLoading"
          @click="submitDispose"
        >
          确认清理
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importResult.visible"
      title="导入结果"
      width="640px"
    >
      <p class="import-summary">
        成功 {{ importResult.success }} 条，失败 {{ importResult.failed }} 条
      </p>
      <el-table
        v-if="importResult.errors.length"
        border
        :data="importResult.errors"
        max-height="420"
      >
        <el-table-column
          prop="row"
          label="行号"
          width="80"
          align="center"
        />
        <el-table-column
          prop="code"
          label="编码"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          prop="message"
          label="失败原因"
          min-width="240"
          show-overflow-tooltip
        />
      </el-table>
      <el-empty
        v-else
        description="全部导入成功"
      />
      <template #footer>
        <el-button
          type="primary"
          @click="importResult.visible = false"
        >
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="FixedAssetCard">
import {
  listFixedAsset,
  getFixedAsset,
  addFixedAsset,
  updateFixedAsset,
  delFixedAsset,
  disposeFixedAsset,
  copyFixedAsset,
  suspendFixedAsset,
  resumeFixedAsset,
  exportFixedAsset,
  downloadFixedAssetTemplate,
  importFixedAsset
} from '@/api/fixed-asset/card'
import { saveFixedAssetChange } from '@/api/fixed-asset/change'
import { listAllAssetCategory } from '@/api/fixed-asset/category'
import { getTree as getSubjectTree } from '@/api/standard/standard-subject'
import { getTree as getDeptTree } from '@/api/idm/dept'
import bookStore from '@/store/modules/bookStore'
import modal from '@/plugins/modal'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const currBookStore = bookStore()
const loading = ref(false)
const buttonLoading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const categories = ref<any[]>([])
const subjectOptions = ref<any[]>([])
const deptOptions = ref<any[]>([])
const formRef = ref<FormInstance>()
const statusFilter = ref('IN_USE')
const subjectProps = { value: 'id', label: 'name', children: 'children' }
const deptProps = { value: 'id', label: 'name', children: 'children' }

const dialog = reactive({ visible: false, title: '' })
const changeDialog = reactive({
  visible: false,
  assetId: '',
  assetCode: '',
  assetName: '',
  snapshot: null as any
})
const changeLoading = ref(false)
const disposeLoading = ref(false)
const disposeDialog = reactive({
  visible: false,
  assetId: '',
  assetCode: '',
  assetName: '',
  bookValue: 0
})
const disposeForm = reactive({
  disposeIncome: 0,
  disposeExpense: 0,
  counterpartSubjectId: '',
  disposalSubjectId: '',
  gainSubjectId: '',
  lossSubjectId: '',
  voucherWord: '记',
  summary: ''
})
const importResult = reactive({
  visible: false,
  success: 0,
  failed: 0,
  errors: [] as Array<{ row: number; code?: string; message?: string }>
})
const isAcceleratedMethod = computed(() =>
  form.depreciationMethod === 'DOUBLE_DECLINING' || form.depreciationMethod === 'SUM_OF_YEARS'
)
const changeFields = [
  { code: 'name', label: '资产名称' },
  { code: 'deptId', label: '使用部门' },
  { code: 'location', label: '存放地点' },
  { code: 'spec', label: '规格型号' },
  { code: 'quantity', label: '数量' },
  { code: 'originalValue', label: '原值' },
  { code: 'residualRate', label: '净残值率' },
  { code: 'usefulLifeMonths', label: '预计使用期数' },
  { code: 'expectedTotalWork', label: '预计总工作量' },
  { code: 'depreciationMethod', label: '折旧方法' },
  { code: 'impairment', label: '减值准备' }
]
const changeForm = reactive({
  fieldCode: 'originalValue',
  beforeValue: '',
  afterValue: '',
  remark: ''
})
const queryParams = reactive({
  bookId: currBookStore.bookId,
  pageNumber: 1,
  pageSize: 10,
  code: '',
  name: '',
  categoryId: '',
  status: 'IN_USE' as string | undefined,
  includeDisposed: false
})

const initForm = () => ({
  id: undefined as string | undefined,
  bookId: currBookStore.bookId,
  code: '',
  name: '',
  categoryId: '',
  deptId: '',
  startUseDate: '',
  quantity: 1,
  spec: '',
  location: '',
  depreciationMethod: 'STRAIGHT_LINE',
  usefulLifeMonths: 60,
  expectedTotalWork: undefined as number | undefined,
  residualRate: 5,
  originalValue: 0,
  taxAmount: 0,
  impairment: 0,
  depreciatedPeriods: 0,
  openingAccumDepr: 0,
  fixedAssetSubjectId: '',
  accumDeprSubjectId: '',
  expenseSubjectId: '',
  purchaseCounterpartSubjectId: '',
  taxSubjectId: '',
  remark: '',
  purchaseVoucherId: '',
  purchaseVoucherWord: '',
  disposeVoucherId: '',
  disposeVoucherWord: '',
  calcFieldsLocked: false
})
const form = reactive(initForm())
const rules = {
  code: [{ required: true, message: '资产编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '资产名称不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '资产类别不能为空', trigger: 'change' }],
  startUseDate: [{ required: true, message: '开始使用日期不能为空', trigger: 'change' }],
  originalValue: [{ required: true, message: '原值不能为空', trigger: 'blur' }],
  expenseSubjectId: [{
    validator: (_: any, value: string, callback: (e?: Error) => void) => {
      if (form.depreciationMethod !== 'NONE' && !value) {
        callback(new Error('折旧费用科目不能为空'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }],
  usefulLifeMonths: [{
    validator: (_: any, value: number, callback: (e?: Error) => void) => {
      if (isAcceleratedMethod.value) {
        if (value == null || value < 24 || value % 12 !== 0) {
          callback(new Error('加速折旧要求期数≥24且为12的整数倍'))
          return
        }
      }
      callback()
    },
    trigger: 'blur'
  }]
}

function onStatusFilter(v: string) {
  if (v === 'ALL') {
    queryParams.status = undefined
    queryParams.includeDisposed = true
  } else if (v === 'ACTIVE') {
    queryParams.status = undefined
    queryParams.includeDisposed = false
  } else if (v === 'DISPOSED') {
    queryParams.status = 'DISPOSED'
    queryParams.includeDisposed = true
  } else if (v === 'SUSPENDED') {
    queryParams.status = 'SUSPENDED'
    queryParams.includeDisposed = true
  } else {
    queryParams.status = 'IN_USE'
    queryParams.includeDisposed = false
  }
  handleQuery()
}

function statusLabel(status: string) {
  if (status === 'DISPOSED') return '已清理'
  if (status === 'SUSPENDED') return '暂停计提'
  return '使用中'
}

function onRowCommand(cmd: string, row: any) {
  if (cmd === 'copy') handleCopy(row)
  else if (cmd === 'change') openChange(row)
  else if (cmd === 'dispose') handleDispose(row)
  else if (cmd === 'delete') handleDelete(row)
}

function goVoucher(voucherId: string) {
  if (!voucherId) return
  router.push({ path: '/voucher/voucher-edit', query: { id: voucherId } })
}

function onCategorySelect(index: string) {
  queryParams.categoryId = index === 'ALL' ? '' : index
  handleQuery()
}

function onCategoryChange(id: string) {
  const c = categories.value.find((i) => i.id === id)
  if (!c || form.calcFieldsLocked) return
  form.depreciationMethod = c.depreciationMethod || form.depreciationMethod
  form.usefulLifeMonths = c.usefulLifeMonths || form.usefulLifeMonths
  form.residualRate = c.residualRate ?? form.residualRate
  form.fixedAssetSubjectId = c.fixedAssetSubjectId || form.fixedAssetSubjectId
  form.accumDeprSubjectId = c.accumDeprSubjectId || form.accumDeprSubjectId
  onDepreciationMethodChange()
}

function onDepreciationMethodChange() {
  formRef.value?.validateField('usefulLifeMonths').catch(() => undefined)
}

function getList() {
  loading.value = true
  listFixedAsset(queryParams).then((res: any) => {
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

function summaryMethod({ columns, data }: any) {
  const sums: string[] = []
  columns.forEach((col: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (['originalValue', 'accumDepr', 'endingNetValue', 'monthlyDepr'].includes(col.property)) {
      const totalVal = data.reduce((acc: number, row: any) => acc + Number(row[col.property] || 0), 0)
      sums[index] = totalVal.toFixed(2)
    } else {
      sums[index] = ''
    }
  })
  return sums
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
  if (!form.purchaseCounterpartSubjectId) {
    form.purchaseCounterpartSubjectId =
      findSubjectIdByCode(subjectOptions.value, '1002')
      || findSubjectIdByCode(subjectOptions.value, '1001')
      || ''
  }
  if (!form.taxSubjectId) {
    form.taxSubjectId =
      findSubjectIdByCode(subjectOptions.value, '2221.01.01')
      || findSubjectIdByCode(subjectOptions.value, '2221.01')
      || findSubjectIdByCode(subjectOptions.value, '2171.01.01')
      || ''
  }
}

function handleAdd() {
  Object.assign(form, initForm())
  applyDefaultSubjects()
  dialog.title = '新增资产卡片'
  dialog.visible = true
}

function handleUpdate(row: any) {
  getFixedAsset(row.id).then((res: any) => {
    Object.assign(form, initForm(), res.data || {})
    dialog.title = '编辑资产卡片'
    dialog.visible = true
  })
}

function handleCopy(row: any) {
  modal.confirm(`确认复制资产「${row.name}」？`).then(() => {
    return copyFixedAsset(row.id)
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '复制成功')
      getList()
      const newId = res.data
      if (newId) {
        handleUpdate({ id: newId })
      }
    }
  }).catch(() => undefined)
}

function handleSuspend(row: any) {
  modal.confirm(`确认暂停资产「${row.name}」计提？暂停所属期及之后将不再计提。`).then(() => {
    return suspendFixedAsset(row.id)
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '已暂停计提')
      getList()
    }
  }).catch(() => undefined)
}

function handleResume(row: any) {
  modal.confirm(`确认恢复资产「${row.name}」计提？`).then(() => {
    return resumeFixedAsset(row.id)
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '已恢复计提')
      getList()
    }
  }).catch(() => undefined)
}

function handleExport() {
  exportFixedAsset({ ...queryParams }).then((blob: any) => {
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = '固定资产卡片.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  })
}

function handleDownloadTemplate() {
  downloadFixedAssetTemplate().then((blob: any) => {
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = '固定资产卡片导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  })
}

function beforeImportUpload(file: any) {
  const ext = file.name.replace(/.+\./, '').toLowerCase()
  if (!['xls', 'xlsx'].includes(ext)) {
    modal.msgWarning('请上传 xls / xlsx 文件')
    return false
  }
  if (file.size / 1024 / 1024 >= 5) {
    modal.msgError('文件大小不能超过 5MB')
    return false
  }
  return true
}

function importExcel(item: any) {
  const formData = new FormData()
  formData.append('excelFile', item.file)
  importFixedAsset(formData).then((res: any) => {
    if (res.code === 0) {
      const data = res.data || {}
      importResult.success = data.success || 0
      importResult.failed = data.failed || 0
      importResult.errors = data.errors || []
      if (importResult.failed > 0) {
        importResult.visible = true
      } else {
        modal.msgSuccess(res.message || `导入完成，成功 ${importResult.success} 条`)
      }
      getList()
    } else {
      modal.msgError(res.message || '导入失败')
    }
  })
}

function submitForm() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    buttonLoading.value = true
    const isNew = !form.id
    const req = form.id ? updateFixedAsset(form) : addFixedAsset(form)
    req.then((res: any) => {
      if (res.code === 0) {
        modal.msgSuccess(res.message || '保存成功')
        dialog.visible = false
        getList()
        const voucherId = res.data?.purchaseVoucherId
        if (isNew && voucherId) {
          modal.confirm('已生成购入凭证，是否打开？').then(() => {
            goVoucher(voucherId)
          }).catch(() => undefined)
        }
      }
    }).finally(() => {
      buttonLoading.value = false
    })
  })
}

function formatMoney(v: any) {
  const n = Number(v || 0)
  return n.toFixed(2)
}

function handleDispose(row: any) {
  const original = Number(row.originalValue || 0)
  const accum = Number(row.accumDepr || 0)
  const impairment = Number(row.impairment || 0)
  disposeDialog.visible = true
  disposeDialog.assetId = row.id
  disposeDialog.assetCode = row.code
  disposeDialog.assetName = row.name
  disposeDialog.bookValue = Math.max(0, original - accum - impairment)
  disposeForm.disposeIncome = 0
  disposeForm.disposeExpense = 0
  disposeForm.counterpartSubjectId =
    row.purchaseCounterpartSubjectId || findSubjectIdByCode(subjectOptions.value, '1002') || ''
  disposeForm.disposalSubjectId =
    row.disposalSubjectId
    || findSubjectIdByCode(subjectOptions.value, '1606')
    || findSubjectIdByCode(subjectOptions.value, '1701')
    || ''
  disposeForm.gainSubjectId =
    findSubjectIdByCode(subjectOptions.value, '5301.01')
    || findSubjectIdByCode(subjectOptions.value, '5301')
    || ''
  disposeForm.lossSubjectId =
    findSubjectIdByCode(subjectOptions.value, '5711.02')
    || findSubjectIdByCode(subjectOptions.value, '5711')
    || findSubjectIdByCode(subjectOptions.value, '5601')
    || ''
  disposeForm.voucherWord = '记'
  disposeForm.summary = `固定资产清理：${row.code} ${row.name}`
}

function submitDispose() {
  const income = Number(disposeForm.disposeIncome || 0)
  const expense = Number(disposeForm.disposeExpense || 0)
  if ((income > 0 || expense > 0) && !disposeForm.counterpartSubjectId) {
    modal.msgWarning('有处置收入或清理费用时，请选择对方科目')
    return
  }
  disposeLoading.value = true
  disposeFixedAsset(disposeDialog.assetId, { ...disposeForm })
    .then((res: any) => {
      if (res.code === 0) {
        const word = res.data?.voucherWord
        const voucherId = res.data?.voucherId
        modal.msgSuccess(word ? `清理成功，已生成凭证 ${word}` : (res.message || '清理成功'))
        disposeDialog.visible = false
        getList()
        if (voucherId) {
          modal.confirm('是否打开清理凭证？').then(() => {
            goVoucher(voucherId)
          }).catch(() => undefined)
        }
      }
    })
    .finally(() => {
      disposeLoading.value = false
    })
}

function handleDelete(row: any) {
  modal.confirm(`确认删除资产「${row.name}」？`).then(() => {
    return delFixedAsset({ listIds: [row.id] })
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '删除成功')
      getList()
    }
  }).catch(() => undefined)
}

function goDepreciation() {
  router.push('/fixed-asset/depreciation')
}

function readSnap(field: string, snap: any) {
  const v = snap?.[field]
  return v == null ? '' : String(v)
}

function openChange(row: any) {
  getFixedAsset(row.id).then((res: any) => {
    const snap = res.data || row
    changeDialog.visible = true
    changeDialog.assetId = snap.id
    changeDialog.assetCode = snap.code
    changeDialog.assetName = snap.name
    changeDialog.snapshot = snap
    changeForm.fieldCode = 'originalValue'
    changeForm.beforeValue = readSnap('originalValue', snap)
    changeForm.afterValue = changeForm.beforeValue
    changeForm.remark = ''
  })
}

function onChangeField() {
  changeForm.beforeValue = readSnap(changeForm.fieldCode, changeDialog.snapshot)
  changeForm.afterValue = changeForm.beforeValue
}

function submitChange() {
  if (!changeForm.fieldCode) return
  if (String(changeForm.afterValue) === String(changeForm.beforeValue)) {
    modal.msgWarning('变动前后内容相同')
    return
  }
  changeLoading.value = true
  saveFixedAssetChange({
    assetId: changeDialog.assetId,
    remark: changeForm.remark,
    items: [{ fieldCode: changeForm.fieldCode, afterValue: String(changeForm.afterValue) }]
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '变动保存成功')
      changeDialog.visible = false
      getList()
    }
  }).finally(() => {
    changeLoading.value = false
  })
}

function loadMeta() {
  listAllAssetCategory().then((res: any) => {
    categories.value = res.data || []
  })
  getSubjectTree({ bookId: currBookStore.bookId }).then((res: any) => {
    subjectOptions.value = res.data || []
  })
  getDeptTree().then((res: any) => {
    deptOptions.value = res.data || []
  })
}

loadMeta()
getList()
</script>

<style scoped>
.category-side {
  min-height: 520px;
}
.side-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}
.import-summary {
  margin: 0 0 12px;
}
</style>

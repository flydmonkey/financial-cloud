<!-- 月结向导 · 步骤2 计提与结转 -->
<template>
  <div class="step-body">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span>固定资产折旧</span>
            <el-tag
              :type="deprStatusTag"
              size="small"
              style="margin-left: 8px"
            >
              {{ deprStatusLabel }}
            </el-tag>
          </template>
          <p class="hint">
            {{ deprHint }}
          </p>
          <el-button
            type="primary"
            :loading="loadingAction"
            :disabled="!deprNeeded || deprAccrued"
            @click="emit('accrue-depreciation')"
          >
            计提折旧
          </el-button>
          <el-button
            :loading="loadingStep2"
            @click="emit('refresh')"
          >
            刷新
          </el-button>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>必做损益结转</span>
            <el-tag
              :type="canLeaveStep2 && carryComplete ? 'success' : 'danger'"
              size="small"
              style="margin-left: 8px"
            >
              {{ carryComplete ? '已完成' : '未完成' }}
            </el-tag>
          </template>
          <p class="hint">
            结转科目按账套会计准则自动匹配：小企业用 5401/5402/5601–5603/5711 等；企业会计制度用 5401/5405/5501–5503/5601 等。有余额才生成分录。
          </p>
          <p
            v-if="cbfySubtitle"
            class="hint"
          >
            {{ cbfySubtitle }}
          </p>
          <el-table
            v-loading="loadingStep2"
            :data="carryRows"
            size="small"
            border
          >
            <el-table-column
              prop="code"
              label="编码"
              width="130"
            />
            <el-table-column
              prop="name"
              label="名称"
              min-width="160"
            />
            <el-table-column
              label="状态"
              width="120"
            >
              <template #default="scope">
                {{ phaseLabel(scope.row.phase) }}
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="120"
            >
              <template #default="scope">
                <el-button
                  v-if="scope.row.phase !== 'posted' && scope.row.phase !== 'na'"
                  type="primary"
                  link
                  :loading="loadingAction"
                  @click="emit('generate-and-post', scope.row.code)"
                >
                  生成并过账
                </el-button>
                <span v-else>—</span>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 8px">
            <el-button
              link
              @click="emit('open-carry-advanced')"
            >
              高级：打开完整结转页
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { CarryRow } from './useMonthEndWizard'

const props = defineProps<{
  carryRows: CarryRow[]
  deprNeeded: boolean
  deprAccrued: boolean
  canLeaveStep2: boolean
  loadingStep2: boolean
  loadingAction: boolean
}>()

const emit = defineEmits<{
  refresh: []
  'accrue-depreciation': []
  'generate-and-post': [code: string]
  'open-carry-advanced': []
}>()

const deprStatusTag = computed(() => {
  if (!props.deprNeeded) return 'info'
  return props.deprAccrued ? 'success' : 'danger'
})

const deprStatusLabel = computed(() => {
  if (!props.deprNeeded) return '不适用'
  return props.deprAccrued ? '已计提' : '未计提'
})

const deprHint = computed(() => {
  if (!props.deprNeeded) return '本期无应计提折旧的资产。'
  return props.deprAccrued ? '本期折旧已计提。' : '请先完成本期固定资产折旧计提。'
})

const carryComplete = computed(
  () =>
    props.carryRows.length > 0 &&
    props.carryRows.every((r) => r.phase === 'posted' || r.phase === 'na'),
)

const cbfySubtitle = computed(() => {
  const cbfy = props.carryRows.find((r) => r.code === 'qm_jz_cbfy')
  if (!cbfy) return ''
  if (cbfy.name.includes('含主营业务成本')) return ''
  return '含主营业务成本（5401/6401）'
})

function phaseLabel(phase: CarryRow['phase']): string {
  switch (phase) {
    case 'missing':
      return '未生成'
    case 'draft':
      return '已生成未过账'
    case 'posted':
      return '已过账'
    case 'na':
      return '无需结转'
    default:
      return phase
  }
}
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
.hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 8px;
}
</style>

#!/usr/bin/env python3
from pathlib import Path

CONTENT = r"""<template>
  <el-card shadow="hover" header-class="el-card-header">
    <template #header>
      <div class="card-title">
        <span>增值税及附加</span>
        <el-select v-model="queryParams.accountPeriod" disabled style="width: 120px" placeholder="">
          <template #label><span>{{ accountPeriod }}</span></template>
          <template v-for="(item, index) in statistics_period" :key="index">
            <el-option :label="item.label" :value="item.value" />
          </template>
        </el-select>
      </div>
    </template>
    <div class="card-content">
      <div class="card-content-item">
        <div>
          <div class="flex justify-items-center">
            <span>预计应交税费</span>
            <el-tooltip content="" placement="top">
              <template #content>
                <span>本期预计应交税费=税金及附加+所得税费用</span>
              </template>
              <el-icon><Warning /></el-icon>
            </el-tooltip>
          </div>
          <div class="bold">{{ formatAmount(resData.balance) }}</div>
        </div>
      </div>
      <div ref="chartRef" style="width: 100%; height: 300px" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import {ref, getCurrentInstance, reactive, toRefs, computed, onMounted, onBeforeUnmount} from "vue";
import bookStore from "@/store/modules/bookStore";
import {getAccountPeriod} from "@/utils/financialCloud";
import {Warning} from "@element-plus/icons-vue"
import {formatAmount} from "@/utils";
import echarts from '@/utils/echarts'
import {statisticsAddedTax} from "@/api/dashboard";
import {BaseValue} from "@/types/FundBalance";

const currBookStore = bookStore()
const {proxy} = getCurrentInstance()!;
const chartRef = ref(null)
let chartInstance: any = null
const {statistics_period} = proxy!.useDict("statistics_period");
const loading = ref(false)

interface DataState {
  queryParams: { bookId: string; accountPeriod: "currentPeriod"; periodType: string; reportDate: string };
  resData: { balance: number; tax: BaseValue[] };
}

const data = reactive<DataState>({
  queryParams: { bookId: currBookStore.bookId, accountPeriod: "currentPeriod", periodType: "month", reportDate: currBookStore.termCurrent },
  resData: { balance: 0, tax: [] },
});
const {queryParams, resData} = toRefs(data);
const accountPeriod = computed(() => getAccountPeriod(queryParams, currBookStore))

const initChart = (option: any) => {
  if (chartRef.value && !chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption(option)
    window.addEventListener('resize', handleResize)
  } else {
    chartInstance.setOption(option, true)
  }
}
const handleResize = () => { chartInstance?.resize() }
const getList = () => {
  loading.value = true
  statisticsAddedTax(queryParams.value).then((res: any) => {
    resData.value = res.data
    initChart({
      title: { text: '近期变动趋势', left: 'left', top: '0%', textStyle: {fontSize: 14} },
      tooltip: { trigger: 'axis', axisPointer: {type: 'shadow'} },
      legend: { data: ['应交税费'], right: 0, top: '0%' },
      grid: { left: '10%', right: '10%', bottom: '31%', top: '23.5%' },
      xAxis: [{ type: 'category', data: resData.value.tax.map(d => d.name), axisTick: {alignWithLabel: true} }],
      yAxis: { type: 'value', name: '金额/万' },
      series: [{ name: '应交税费', type: 'bar', data: resData.value.tax.map(d => ({ value: d.value, itemStyle: { color: d.value < 0 ? 'red' : '#91ccff' } })), barWidth: 15 }]
    })
  }).finally(() => { loading.value = false })
}
onMounted(() => getList())
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose(); chartInstance = null
})
</script>
<style scoped lang="scss">
:deep(.el-card__header) { padding: 4px 4px 4px 20px; background-color: #1890ff44; height: 30px; }
:deep(.el-select__wrapper) { background-color: transparent; box-shadow: none; }
.card-title { display: flex; justify-content: space-between; align-items: center; }
.card-content { &-item { font-size: 0.9em; height: 70px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; .bold { font-weight: bold; font-size: 1.5em; } } }
</style>
"""

if __name__ == "__main__":
    path = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src" / "views" / "dashboard" / "accounting" / "added_tax.vue"
    path.write_text(CONTENT, encoding="utf-8", newline="\n")
    print(path.read_text(encoding="utf-8").splitlines()[7])

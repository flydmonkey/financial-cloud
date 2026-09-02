#!/usr/bin/env python3
"""Rewrite net_profit.vue with correct UTF-8 Chinese labels."""
from pathlib import Path

OUT = (
    Path(__file__).resolve().parents[1]
    / "financial-cloud-ui"
    / "src"
    / "views"
    / "dashboard"
    / "accounting"
    / "net_profit.vue"
)

# Placeholders are ASCII; replaced with unicode escapes below.
CONTENT = r"""<template>
  <el-card
    shadow="hover"
    header-class="el-card-header"
  >
    <template #header>
      <div class="card-title">
        <span>__TITLE__</span>
        <el-select
          v-model="queryParams.accountPeriod"
          style="width: 120px"
          placeholder=""
          @change="getList"
        >
          <template #label>
            <span>{{ accountPeriod }}</span>
          </template>
          <template
            v-for="(item, index) in statistics_period"
            :key="index"
          >
            <el-option
              :label="item.label"
              :value="item.value"
            />
          </template>
        </el-select>
      </div>
    </template>

    <div
      v-loading="loading"
      class="card-content"
    >
      <div class="card-content-item">
        <div>
          <div class="flex justify-items-center">
            <span>__NET_PROFIT__</span>
            <el-tooltip
              content=""
              placement="top"
            >
              <template #content>
                <span>
                  __NET_PROFIT_TIP__
                </span>
              </template>
              <el-icon>
                <Warning />
              </el-icon>
            </el-tooltip>
          </div>
          <div class="bold">
            {{ formatAmount(resData.balance) }}
          </div>
        </div>
        <div class="small">
          <div>__VS_LAST__{{ resData.balanceLast }}%</div>
          <div>__VS_YEAR__{{ resData.balanceLastYear }}%</div>
        </div>
      </div>
      <div class="card-content-item">
        <div>
          <div class="flex justify-items-center">
            <span>__RATIO__</span>
          </div>
          <div class="bold">
            {{ resData.balanceRatio }}%
          </div>
        </div>
        <div class="small">
          <div>__VS_LAST__{{ resData.balanceRatioLast }}%</div>
          <div>__VS_YEAR__{{ resData.balanceRatioLastYear }}%</div>
        </div>
      </div>

      <div
        ref="chartRef"
        style="width: 100%; height: 300px"
      />
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
import {statisticsNetProfit} from "@/api/dashboard"
import {BaseValue} from "@/types/FundBalance";

const currBookStore = bookStore()
const {proxy} = getCurrentInstance()!;
const chartRef = ref(null)
let chartInstance: any = null
const {statistics_period} = proxy!.useDict("statistics_period");
const loading = ref(false)

interface DataState {
  queryParams: {
    bookId: string;
    accountPeriod: string;
    periodType: string;
    reportDate: string;
  };
  resData: {
    balance: number;
    balanceLastYear: number;
    balanceLast: number;
    balanceRatio: number;
    balanceRatioLastYear: number;
    balanceRatioLast: number;
    balanceList: BaseValue[];
    balanceRatioList: BaseValue[];
  };
}

const data = reactive<DataState>({
  queryParams: {
    bookId: currBookStore.bookId,
    accountPeriod: "currentPeriod",
    periodType: "month",
    reportDate: currBookStore.termCurrent
  },
  resData: {
    balance: 0,
    balanceLastYear: 0,
    balanceLast: 0,
    balanceRatio: 0,
    balanceRatioLastYear: 0,
    balanceRatioLast: 0,
    balanceList: [],
    balanceRatioList: [],
  }
});
const {queryParams, resData} = toRefs(data);
const accountPeriod = computed(() => {
  return getAccountPeriod(queryParams, currBookStore)
})

const initChart = (option: any) => {
  if (chartRef.value && !chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption(option)
    window.addEventListener('resize', handleResize)
  } else {
    chartInstance.setOption(option, true);
  }
}

const handleResize = () => {
  chartInstance.resize()
}
const getList = () => {
  loading.value = true
  statisticsNetProfit(queryParams.value).then((res: any) => {
    resData.value = res.data
    const option = {
      title: {
        text: '__CHART_TITLE__',
        left: 'left',
        top: "6%",
        textStyle: {fontSize: 14}
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {type: 'shadow'}
      },
      legend: {
        data: ['__NET_PROFIT__', '__RATIO__'],
        right: 0,
        top: "6%",
      },
      grid: {
        left: '10%',
        right: '10%',
        bottom: '25%',
        top: "30%",
      },
      xAxis: [
        {
          type: 'category',
          data: resData.value.balanceList.map(d => d.name),
          axisTick: {alignWithLabel: true}
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '__Y_AMOUNT__',
          position: 'left'
        },
        {
          type: 'value',
          name: '__Y_RATIO__',
          position: 'right'
        }
      ],
      series: [
        {
          name: '__NET_PROFIT__',
          type: 'bar',
          data: resData.value.balanceList.map(d => ({
            value: d.value,
            itemStyle: {
              color: d.value < 0 ? 'red' : '#91ccff'
            }
          })),
          barWidth: 15
        },
        {
          name: '__RATIO__',
          type: 'line',
          yAxisIndex: 1,
          data: resData.value.balanceRatioList.map(d => d.value),
          smooth: true,
          lineStyle: {
            color: '#73c0de'
          },
          areaStyle: {
            color: 'rgba(115, 192, 222, 0.2)'
          },
          symbol: 'circle',
          symbolSize: 6
        }
      ]
    }
    initChart(option)
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  getList()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if(chartInstance){
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>
<style scoped lang="scss">
:deep(.el-card__header) {
  padding: 4px 4px 4px 20px;
  background-color: #1890ff44;
  height: 30px;
}

:deep(.el-select__wrapper) {
  background-color: transparent;
  box-shadow: none;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-content {

  &-item {
    font-size: 0.9em;
    height: 46px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .bold {
      font-weight: bold;
      font-size: 1.5em;
    }

    .small {
      font-size: 0.9em;
      line-height: 1.5em;
    }
  }
}
</style>
"""

REPLACEMENTS = {
    "__TITLE__": "\u51c0\u5229\u6da6",
    "__NET_PROFIT__": "\u51c0\u5229\u6da6",
    "__NET_PROFIT_TIP__": "\u672c\u671f\u51c0\u5229\u6da6\uff0c\u53d6\u81ea\u5229\u6da6\u8868\u6700\u540e\u4e00\u884c\u3002",
    "__VS_LAST__": "\u540c\u6bd4\u4e0a\u671f\uff1a",
    "__VS_YEAR__": "\u540c\u6bd4\u53bb\u5e74\uff1a",
    "__RATIO__": "\u51c0\u5229\u6da6\u7387",
    "__CHART_TITLE__": "\u8fd1\u671f\u51c0\u5229\u6da6\u4e0e\u5229\u6da6\u7387\u8d8b\u52bf",
    "__Y_AMOUNT__": "\u91d1\u989d/\u4e07",
    "__Y_RATIO__": "\u51c0\u5229\u6da6\u7387(%)",
}


def main() -> None:
    text = CONTENT
    for key, value in REPLACEMENTS.items():
        text = text.replace(key, value)
    OUT.write_text(text, encoding="utf-8", newline="\n")
    verify = OUT.read_text(encoding="utf-8")
    assert "\u51c0\u5229\u6da6" in verify
    assert "????" not in verify
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()

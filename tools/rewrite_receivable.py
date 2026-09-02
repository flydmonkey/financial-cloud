#!/usr/bin/env python3
"""Rewrite receivable.vue with correct UTF-8 Chinese labels."""
from pathlib import Path

OUT = (
    Path(__file__).resolve().parents[1]
    / "financial-cloud-ui"
    / "src"
    / "views"
    / "dashboard"
    / "accounting"
    / "receivable.vue"
)

CONTENT = r"""<template>
  <el-row :gutter="10">
    <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
      <el-card style="height: 100%" shadow="hover" header-class="el-card-header">
        <template #header>
          <div class="card-title">
            <span>__RECV__</span>
            <el-select style="width: 120px" v-model="queryParams.accountPeriod" placeholder="" @change="getList">
              <template #label>
                <span>{{ accountPeriod }}</span>
              </template>
              <template v-for="(item, index) in statistics_period" :key="index">
                <el-option :label="item.label" :value="item.value"></el-option>
              </template>
            </el-select>
          </div>
        </template>
        <div class="card-content" v-loading="loading">
          <div class="card-content-item bold">
            <div class="flex justify-items-center">
              <span>__RECV_ACC__</span>
              <el-tooltip content="" placement="top">
                <template #content>
                  <span>
                    __RECV_TIP__
                  </span>
                </template>
                <el-icon>
                  <Warning/>
                </el-icon>
              </el-tooltip>
            </div>
            <div>{{ formatAmount(recvData.balance) }}</div>
          </div>
          <template v-for="(item, index) in recvData.subjectBalance" :key="index">
            <div class="card-content-item">
              <div>{{ item.name }}</div>
              <div>{{ formatAmount(item.value) }}</div>
            </div>
          </template>
          <template v-if="recvData.subjectBalance.length < 5" v-for="index in (5 - recvData.subjectBalance.length)"
                    :key="'em' + index">
            <div class="card-content-item">
              <div>--</div>
              <div>--</div>
            </div>
          </template>

          <div class="card-content-item bold">
            <div class="flex justify-items-center">
              <span>__CYCLE__</span>
              <el-tooltip content="" placement="top">
                <template #content>
                  <span>
                    __CYCLE_TIP1__
                    <br/>__CYCLE_TIP2__
                  </span>
                </template>
                <el-icon>
                  <Warning/>
                </el-icon>
              </el-tooltip>
            </div>
            <div>{{ recvData.cycleDays }}__DAY__</div>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
      <el-card style="height: 100%" shadow="hover" header-class="el-card-header">
        <template #header>
          <div class="card-title">
            <span>__PAY__</span>
            <el-select style="width: 120px" v-model="queryParams.accountPeriod" placeholder="" @change="getList">
              <template #label>
                <span>{{ accountPeriod }}</span>
              </template>
              <template v-for="(item, index) in statistics_period" :key="index">
                <el-option :label="item.label" :value="item.value"></el-option>
              </template>
            </el-select>
          </div>
        </template>
        <div class="card-content" v-loading="loading">
          <div class="card-content-item bold">
            <div class="flex justify-items-center">
              <span>__PAY_ACC__</span>
              <el-tooltip content="" placement="top">
                <template #content>
                  <span>
                    __PAY_TIP__
                  </span>
                </template>
                <el-icon>
                  <Warning/>
                </el-icon>
              </el-tooltip>
            </div>
            <div>{{ formatAmount(payData.balance) }}</div>
          </div>
          <template v-for="(item, index) in payData.subjectBalance" :key="index">
            <div class="card-content-item">
              <div>{{ item.name }}</div>
              <div>{{ formatAmount(item.value) }}</div>
            </div>
          </template>
          <template v-if="payData.subjectBalance.length < 5" v-for="index in (5 - payData.subjectBalance.length)"
                    :key="'em' + index">
            <div class="card-content-item">
              <div>--</div>
              <div>--</div>
            </div>
          </template>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>


<script setup lang="ts">
import {ref, getCurrentInstance, reactive, toRefs, computed, onMounted} from "vue";
import bookStore from "@/store/modules/bookStore";
import {getAccountPeriod} from "@/utils/financialCloud";
import {Warning} from "@element-plus/icons-vue"
import {formatAmount} from "@/utils";
import {BaseValue} from "@/types/FundBalance";
import {statisticsAccountsReceivable, statisticsAccountsPayable} from "@/api/dashboard"

const currBookStore = bookStore()
const {proxy} = getCurrentInstance()!;
const loading = ref(false);
const {statistics_period} = proxy!.useDict("statistics_period");


interface DataState {
  queryParams: {
    bookId: string;
    accountPeriod: string;
    periodType: string;
    reportDate: string;
  };
  recvData: {
    balance: number;
    cycleDays: number;
    subjectBalance: BaseValue[];
  };
  payData: {
    balance: number;
    subjectBalance: BaseValue[];
  };
}

const data = reactive<DataState>({
  queryParams: {
    bookId: currBookStore.bookId,
    accountPeriod: "currentPeriod",
    periodType: "month",
    reportDate: currBookStore.termCurrent
  },
  recvData: {
    balance: 0,
    cycleDays: 0,
    subjectBalance: [],
  },
  payData: {
    balance: 0,
    subjectBalance: [],
  },
});
const {queryParams, recvData, payData} = toRefs(data);
const accountPeriod = computed(() => {
  return getAccountPeriod(queryParams, currBookStore)
})

const getList = () => {
  loading.value = true
  statisticsAccountsReceivable(queryParams.value).then((res: any) => {
    recvData.value = res.data
  }).finally(() => {
    loading.value = false
  })
  statisticsAccountsPayable(queryParams.value).then((res: any) => {
    payData.value = res.data
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  getList()
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
    height: 30px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    &.bold {
      font-size: 1em;
      font-weight: bold;
    }
  }
}
</style>
"""

REPLACEMENTS = {
    "__RECV__": "\u5e94\u6536",
    "__RECV_ACC__": "\u5e94\u6536\u8d26\u6b3e",
    "__RECV_TIP__": "\u622a\u6b62\u81f3\u6240\u9009\u671f\u95f4\u7684\u671f\u672b\uff0c\u201c\u5e94\u6536\u8d26\u6b3e\u201d\u79d1\u76ee\u4f59\u989d\u3002",
    "__CYCLE__": "\u5e73\u5747\u5468\u8f6c\u5929\u6570",
    "__CYCLE_TIP1__": "\u5e74\u521d\u622a\u6b62\u4e0a\u4e00\u671f\u671f\u95f4\u7684\u5e94\u6536\u8d26\u6b3e\u5468\u8f6c\u5929\u6570\uff0c\u53cd\u6620\u4f01\u4e1a\u56de\u6b3e\u901f\u5ea6\uff0c\u5929\u6570\u8d8a\u5c0f\u5219\u56de\u6b3e\u901f\u5ea6\u8d8a\u5feb\u3002",
    "__CYCLE_TIP2__": "\u8ba1\u7b97\u516c\u5f0f\u4e3a\uff1a\u5e74\u521d\u81f3\u6240\u9009\u671f\u672b\u603b\u5929\u6570/(2*\u8425\u4e1a\u6536\u5165/(\u671f\u521d\u5e94\u6536\u8d26\u6b3e+\u671f\u672b\u5e94\u6536\u8d26\u6b3e))",
    "__DAY__": "\u5929",
    "__PAY__": "\u5e94\u4ed8",
    "__PAY_ACC__": "\u5e94\u4ed8\u8d26\u6b3e",
    "__PAY_TIP__": "\u622a\u6b62\u81f3\u6240\u9009\u671f\u95f4\u7684\u671f\u672b\uff0c\u201c\u5e94\u4ed8\u8d26\u6b3e\u201d\u79d1\u76ee\u4f59\u989d\u3002",
}


def main() -> None:
    text = CONTENT
    for key, value in REPLACEMENTS.items():
        text = text.replace(key, value)
    OUT.write_text(text, encoding="utf-8", newline="\n")
    verify = OUT.read_text(encoding="utf-8")
    assert "\u5e94\u6536\u8d26\u6b3e" in verify
    assert "????" not in verify
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()

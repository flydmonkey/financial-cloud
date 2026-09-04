<!-- 月结向导 · 步骤0 人工确认 -->
<template>
  <div class="step-body">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="以下为人工确认项（本期不系统检）。往来应收应付与账龄已在「系统校验」中计算（逾期仅警告，不阻断结账）。"
      style="margin-bottom: 12px"
    />
    <el-table
      :data="manualCheckData"
      border
      style="width: 100%"
    >
      <el-table-column
        prop="item"
        label="检查项目"
        width="180"
      />
      <el-table-column
        prop="content"
        label="说明"
      />
    </el-table>
  </div>
</template>

<script lang="ts" setup>
const manualCheckData = [
  {
    item: '检查银行和现金',
    content: '【人工确认 / 本期不系统检】余额与银行对账单、库存现金盘点表比对（系统暂无银行余额调节）',
  },
  {
    item: '检查存货',
    content: '【人工确认 / 本期不系统检】账实一致（本产品不含进销存）',
  },
  {
    item: '检查主营业务收入',
    content: '【人工确认 / 本期不系统检】与税控发票统计及无票收入备查勾稽',
  },
  {
    item: '检查工资、五险一金',
    content: '【人工确认】可结合薪资模块核对计提与发放',
  },
  {
    item: '检查税费',
    content: '【人工确认 / 本期不系统检】增值税结转、附加税与所得税计提请人工确认',
  },
  {
    item: '检查原始凭证',
    content: '【人工确认】费用审批与附件归档',
  },
  {
    item: '检查科目余额表',
    content: '【人工确认】核对发生额、余额方向是否异常',
  },
]
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
</style>

<!-- 月结向导 · 步骤4 结账 -->
<template>
  <div class="step-body">
    <div
      v-if="isCheckout"
      class="checkout-result"
    >
      <el-result
        v-if="checkoutOk"
        icon="success"
        title="结账成功"
        :sub-title="`${closedTerm || '本期'}月结已完成，请核对已结期间报表后交付账本包`"
      >
        <template #extra>
          <el-alert v-if="checkoutError" type="warning" :closable="false" :title="checkoutError" />
          <div class="delivery-actions">
            <el-button type="primary" @click="emit('review-reports')">核对本期报表</el-button>
            <el-button @click="emit('deliver-books')">交付本期账本包</el-button>
          </div>
        </template>
      </el-result>
      <el-result
        v-else
        icon="error"
        title="结账失败"
        :sub-title="checkoutError || '请检查硬门槛后再结账'"
      >
        <template #extra>
          <el-button
            type="primary"
            link
            @click="emit('back-to-verify')"
          >
            返回系统校验
          </el-button>
        </template>
      </el-result>
    </div>
    <el-alert
      v-else
      type="success"
      :closable="false"
      show-icon
      title="系统硬检已通过，确认后执行结账（将锁定本期并推进账期）"
    />
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  isCheckout: boolean
  checkoutOk: boolean
  checkoutError: string
  closedTerm?: string
}>()

const emit = defineEmits<{
  'back-to-verify': []
  'review-reports': []
  'deliver-books': []
}>()
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
.delivery-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 12px; }
.checkout-result {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 280px;
  padding: 24px 16px;
}
.checkout-result :deep(.el-result) {
  width: 100%;
  max-width: 480px;
}
</style>

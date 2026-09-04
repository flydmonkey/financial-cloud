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
        sub-title="本期月结已完成，已进入下一个账期"
      />
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
}>()

const emit = defineEmits<{
  'back-to-verify': []
}>()
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
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

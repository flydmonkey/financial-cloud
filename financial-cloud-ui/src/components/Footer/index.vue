<template>
  <div
    class="el-login-footer"
    :style="{'position': props.position}"
  >
    <div>
      财务云<br>
      Version {{ version }}<br>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";

const version = ref(__APP_VERSION__)
const props = defineProps({
  position: {
    type: String,
    default: () => {
      return "fixed"
    }
  }
})

</script>

<style scoped lang="scss">
.el-login-footer {
  position: relative;
  bottom: 0;
  border-top: 1px solid #e5e5e5;
  width: 100%;
  text-align: center;
  font-size: 14px;
  color: #000000AA;
  padding-top: 10px;
  padding-bottom: 10px;

  a {
    color: #1890ff;
  }
}
</style>
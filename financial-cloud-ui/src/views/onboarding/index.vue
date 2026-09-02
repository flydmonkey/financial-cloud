<template>
  <div class="onboarding">
    <div class="top-box">
      <div class="title">
        <img
          :src="staticAppInfo.logo"
          alt=""
        >
        {{ staticAppInfo.consoleTitle || '金账簿' }}
      </div>
    </div>

    <div class="onboarding-card">
      <h2 class="card-title">欢迎使用，请先创建账套</h2>
      <p class="card-desc">
        填写以下信息即可完成初始化，系统将自动配置会计科目、报表与凭证模板。
      </p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="onboarding-form"
      >
        <el-form-item
          prop="name"
          label="账套名称"
        >
          <el-input
            v-model="form.name"
            placeholder="例如：主账套"
          />
        </el-form-item>
        <el-form-item
          prop="companyName"
          label="单位名称"
        >
          <el-input
            v-model="form.companyName"
            placeholder="公司或组织全称"
          />
        </el-form-item>
        <el-form-item
          prop="standardId"
          label="会计准则"
        >
          <el-select
            v-model="form.standardId"
            placeholder="请选择会计准则"
            style="width: 100%"
          >
            <el-option
              v-for="item in standardList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          prop="enableDate"
          label="建账期间"
        >
          <el-date-picker
            v-model="form.enableDate"
            style="width: 100%"
            type="month"
            placeholder="选择账套启用年月"
            format="YYYY-MM"
            value-format="YYYY-MM"
          />
        </el-form-item>
        <el-form-item
          prop="vatType"
          label="纳税性质"
        >
          <el-select
            v-model="form.vatType"
            placeholder="请选择纳税性质"
            style="width: 100%"
          >
            <el-option
              v-for="dict in books_vat_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-button
        type="primary"
        class="submit-btn"
        :loading="loading"
        @click="handleSubmit"
      >
        创建并进入系统
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {getCurrentInstance, onMounted, reactive, ref, toRefs} from "vue";
import {ElForm} from "element-plus";
import {listStandardsAll} from "@/api/standard/standard";
import {setupBook} from "@/api/book/book";
import {switchBook} from "@/api/idm/user";
import {loginPreGet} from "@/api/login.js";
import {resolveInstitutionLogo} from "@/constants/branding";
import appStore from "@/store/modules/app.js";
import modal from "@/plugins/modal";

const proxy: any = getCurrentInstance()!.proxy;
const {books_vat_type} = proxy?.useDict("books_vat_type");

const formRef = ref<InstanceType<typeof ElForm> | null>(null);
const loading = ref(false);
const standardList = ref<any[]>([]);
const staticAppInfo = ref<any>({});

const data = reactive({
  form: {
    name: "",
    companyName: "",
    standardId: "",
    enableDate: "",
    vatType: undefined as number | undefined,
    voucherReviewed: 0,
    status: 1,
  },
  rules: {
    name: [{required: true, message: "账套名称不能为空", trigger: "blur"}],
    companyName: [{required: true, message: "单位名称不能为空", trigger: "blur"}],
    standardId: [{required: true, message: "会计准则不能为空", trigger: "change"}],
    enableDate: [{required: true, message: "建账期间不能为空", trigger: "change"}],
    vatType: [{required: true, message: "纳税性质不能为空", trigger: "change"}],
  },
});

const {form, rules} = toRefs(data);

function loadStandards() {
  listStandardsAll({status: 1}).then((res: any) => {
    if (res.code === 0) {
      standardList.value = res.data || [];
    }
  });
}

function loadAppInfo() {
  loginPreGet().then((res: any) => {
    if (res.code === 0 && res.data?.inst) {
      const inst = {...res.data.inst};
      inst.logo = resolveInstitutionLogo(inst.logo);
      staticAppInfo.value = inst;
      appStore().setAppInfo(inst);
    }
  });
}

function handleSubmit() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return;
    }
    loading.value = true;
    setupBook(form.value).then((res: any) => {
      if (res.code !== 0) {
        modal.msgError(res.message || "创建账套失败");
        loading.value = false;
        return;
      }
      const bookId = res.data?.bookId;
      switchBook(bookId).then((switchRes: any) => {
        if (switchRes.code === 0) {
          window.location.href = import.meta.env.VITE_APP_CONTEXT_PATH || "/";
        } else {
          modal.msgError(switchRes.message || "切换账套失败");
          loading.value = false;
        }
      }).catch(() => {
        loading.value = false;
      });
    }).catch(() => {
      loading.value = false;
    });
  });
}

onMounted(() => {
  loadAppInfo();
  loadStandards();
});
</script>

<style lang="scss" scoped>
.onboarding {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
}

.top-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 10%;

  .title {
    display: flex;
    align-items: center;
    font-size: 22px;
    font-weight: 600;
    color: #303133;

    img {
      height: 36px;
      margin-right: 12px;
    }
  }
}

.onboarding-card {
  margin: 20px auto 40px;
  padding: 36px 40px 32px;
  width: min(560px, calc(100% - 32px));
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #eaeaea;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

.card-title {
  margin: 0 0 8px;
  font-size: 22px;
  color: #303133;
}

.card-desc {
  margin: 0 0 28px;
  font-size: 14px;
  line-height: 1.6;
  color: #909399;
}

.submit-btn {
  width: 100%;
  height: 42px;
  margin-top: 8px;
  font-size: 16px;
}
</style>

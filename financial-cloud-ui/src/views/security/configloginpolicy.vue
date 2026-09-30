<template>
  <div
    v-loading="loading"
    class="app-container"
  >
    <el-card class="common-card">
      <template v-if="loadError">
        <el-empty :description="loadError">
          <el-button
            type="primary"
            @click="get"
          >
            重试
          </el-button>
        </el-empty>
      </template>
      <template v-else-if="!loading && !form?.id">
        <el-empty description="暂无登录策略数据" />
      </template>
      <template v-else>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="180px"
          class="login-policy-form"
        >
          <el-row :gutter="30">
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.sessionValidity')"
                prop="sessionValidity"
              >
                <el-input-number
                  v-model="form.sessionValidity"
                  :min="0"
                />
                {{ $t('jbx.text.hour') }}
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.tokenValidity')"
                prop="tokenValidity"
              >
                <el-input-number
                  v-model="form.tokenValidity"
                  :min="0"
                />
                {{ $t('jbx.text.hour') }}
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.terminals')"
                prop="terminals"
              >
                <el-input-number
                  v-model="form.terminals"
                  :min="0"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.isFirstPasswordModify')"
                prop="isFirstPasswordModify"
              >
                <el-switch
                  v-model="form.isFirstPasswordModify"
                  active-value="Y"
                  inactive-value="N"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.captcha')"
                prop="captcha"
              >
                <el-select
                  v-model="form.captcha"
                  style="width: 150px;"
                >
                  <el-option
                    v-for="dict in captcha_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.captchaMgt')"
                prop="captchaMgt"
              >
                <el-select
                  v-model="form.captchaMgt"
                  style="width: 150px;"
                >
                  <el-option
                    v-for="dict in captcha_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.scanCode')"
                prop="scanCode"
              >
                <el-select
                  v-model="form.scanCode"
                  style="width: 150px;"
                >
                  <el-option
                    v-for="dict in scanCode_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.loginAttempts')"
                prop="loginAttempts"
              >
                <el-input-number
                  v-model="form.loginAttempts"
                  :min="0"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.isMobile')"
                prop="isMobile"
              >
                <el-switch
                  v-model="form.isMobile"
                  active-value="Y"
                  inactive-value="N"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.isSocial')"
                prop="isSocial"
              >
                <el-switch
                  v-model="form.isSocial"
                  active-value="Y"
                  inactive-value="N"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.isAutoLock')"
                prop="isAutoLock"
              >
                <el-switch
                  v-model="form.isAutoLock"
                  active-value="Y"
                  inactive-value="N"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.lockInterval')"
                prop="lockInterval"
              >
                <el-input-number
                  v-model="form.lockInterval"
                  :min="0"
                />
                {{ $t('jbx.text.minute') }}
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.passwordAttempts')"
                prop="passwordAttempts"
              >
                <el-input-number
                  v-model="form.passwordAttempts"
                  :min="0"
                />
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="12"
            >
              <el-form-item
                :label="$t('jbx.loginpolicy.passwordAttemptsCaptcha')"
                prop="passwordAttemptsCaptcha"
              >
                <el-switch
                  v-model="form.passwordAttemptsCaptcha"
                  active-value="Y"
                  inactive-value="N"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="dialog-footer">
          <el-button
            type="primary"
            :loading="saving"
            @click="submitForm"
          >
            {{ $t('jbx.text.submit') }}
          </el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup name="ConfigLoginPolicy" lang="ts">
import { ElForm } from "element-plus";
import { ref, getCurrentInstance, reactive, toRefs } from "vue";
import modal from "@/plugins/modal";
import { getPolicy, updateSecurityPolicy } from "@/api/security/configloginpolicy";
import { useI18n } from "vue-i18n";

const { proxy } = getCurrentInstance()!;
const formRef = ref<InstanceType<typeof ElForm> | null>(null);
const { t } = useI18n();

const { captcha_type, scanCode_type } = (proxy as any).useDict("captcha_type", "scanCode_type");

const loading = ref(true);
const saving = ref(false);
const loadError = ref("");

const data: any = reactive({
  form: {},
  rules: {},
});

const { form, rules } = toRefs(data);

function get() {
  loading.value = true;
  loadError.value = "";
  getPolicy()
    .then((res: any) => {
      form.value = res.data || {};
      if (!res.data) {
        loadError.value = "未获取到登录策略，请确认接口可用或联系管理员";
      }
    })
    .catch((err: any) => {
      form.value = {};
      loadError.value = err?.message || "加载登录策略失败，请稍后重试";
    })
    .finally(() => {
      loading.value = false;
    });
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return;
    }
    saving.value = true;
    updateSecurityPolicy(form.value)
      .then(() => {
        modal.msgSuccess(t("jbx.alert.operate.success"));
        get();
      })
      .finally(() => {
        saving.value = false;
      });
  });
}

get();
</script>

<style scoped>
.login-policy-form {
  margin: 12px 0 8px;
}

.dialog-footer {
  text-align: center;
  padding-top: 8px;
}

@media (max-width: 768px) {
  .login-policy-form :deep(.el-form-item__label) {
    width: 140px !important;
  }
}
</style>

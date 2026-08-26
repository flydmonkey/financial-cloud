<template>
   <el-form ref="pwdRef" :model="user" :rules="rules" label-width="100px">
      <el-form-item label="旧密码" prop="oldPassword">
         <el-input v-model="user.oldPassword" placeholder="请输入旧密码" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
         <el-input v-model="user.newPassword" placeholder="请输入新密码" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
         <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password/>
      </el-form-item>
      <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button type="danger" @click="close">关闭</el-button>
      </el-form-item>
   </el-form>
</template>

<script setup lang="ts">
import {ref, getCurrentInstance, reactive, toRefs, watch, defineComponent} from "vue";
import modal from "@/plugins/modal";
import {changePass, updatePassword} from "@/api/system/user";
import {validatePass} from "@/api/commonApi";
import {useI18n} from "vue-i18n";
import {ElForm} from "element-plus";

const props: any = defineProps({
  user: {
    type: Object
  },
  pwdPolicy: {
    type: Object,
    default: {}
  }
})

const {proxy} = getCurrentInstance()!;

const {t} = useI18n();

const user: any = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined
});
const pwdRef = ref<InstanceType<typeof ElForm> | null>(null);

const equalToPassword: any = (rule: any, value: any, callback: any) => {
  if (user.newPassword !== value) {
    callback(new Error("两次输入密码不一致"));
  } else {
    callback();
  }
};

const notAllowedEqualToPassword: any = (rule: any, value: any, callback: any) => {
  if (user.newPassword === user.oldPassword) {
    callback(new Error("新密码不能和旧密码相同"));
  } else {
    callback();
  }
};

const validateWhitespace: any = (rule: any, value: any, callback: any) => {
  // 使用正则表达式检查输入是否只包含空格
  if (/^\s+$/.test(value)) {
    callback(new Error("密码不能输入纯空格"));
  } else {
    callback();
  }
}

const rules: any = ref({
  oldPassword: [
    { required: true, trigger: "blur", message: "请输入旧密码" },
    { validator: notAllowedEqualToPassword, trigger: ['blur', 'change'] },
    { validator: validateWhitespace, trigger: ['blur', 'change'] }
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: 'blur' },
    { validator: (rule: any, value: any, callback: any) => validatePass(rule, value, callback, props.pwdPolicy), trigger: ['blur', 'change'] },
    { validator: notAllowedEqualToPassword, trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: "请输入确认新密码", trigger: "blur" },
    { validator: equalToPassword, trigger: "blur" }
  ]
});

/** 提交按钮 */
function submit(): any {
  pwdRef?.value?.validate((valid: any) =>  {
    if (valid) {
      let params: any = {
        userId: props.user.id,
        oldPassword: user.oldPassword,
        password: user.newPassword,
        confirmPassword: user.confirmPassword,
      }
      updatePassword(params).then((res: any) =>  {
        if (res.code === 0) {
          modal.msgSuccess(t('org.success.update'));
        } else {
          modal.msgError(res.message);
        }
      });
    }
  });
}

/** 关闭按钮 */
function close(): any {
  proxy.$tab.closePage();
}

</script>

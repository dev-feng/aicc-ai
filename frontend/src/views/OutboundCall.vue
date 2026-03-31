<template>
  <section class="page">
    <div class="hero">
      <p class="eyebrow">Outbound</p>
      <h1>发起测试外呼</h1>
      <p>调用后端外呼接口，拿到真实 callId，便于后续人工接管和 Phase 2 联调继续使用。</p>
    </div>

    <div class="panel">
      <div class="panel-inner">
        <h2 class="panel-title">外呼参数</h2>
        <p class="panel-subtitle">当前直连后端 <code>/api/call/outbound</code>，成功后会返回真实 callId。</p>

        <el-form label-position="top" :model="form" @submit.prevent>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="主叫号码">
                <el-input v-model="form.caller" placeholder="例如 1002" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="被叫号码">
                <el-input v-model="form.callee" placeholder="例如 1003" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-button type="primary" :loading="submitting" @click="submitCall">发起外呼</el-button>
        </el-form>

        <div v-if="callId" class="result-box" style="margin-top: 20px">
          <p class="result-label">最近一次成功返回的 callId</p>
          <p class="result-value">{{ callId }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createOutboundCall } from "../api/call.js";

const form = reactive({
  caller: "",
  callee: ""
});

const callId = ref("");
const submitting = ref(false);

async function submitCall() {
  if (!form.caller || !form.callee) {
    ElMessage.warning("请输入主叫和被叫号码");
    return;
  }

  submitting.value = true;
  try {
    const result = await createOutboundCall(form);
    if (result.code === 200) {
      callId.value = result.data.callId;
      ElMessage.success(`外呼已提交，callId=${result.data.callId}`);
      return;
    }
    ElMessage.error(result.msg || "外呼失败");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "外呼失败");
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page">
    <div class="hero">
      <p class="eyebrow">Transfer</p>
      <h1>人工接管</h1>
      <p>手动触发转人工，用于验证 T17 的坐席路由能力。可指定目标坐席，也可留空走默认可用坐席。</p>
    </div>

    <div class="dashboard-grid">
      <div class="panel">
        <div class="panel-inner">
          <h2 class="panel-title">转人工请求</h2>
          <p class="panel-subtitle">请求后端 <code>/api/call/transfer</code>，依赖已有有效 callId。</p>

          <el-form label-position="top" :model="form">
            <el-form-item label="callId">
              <el-input v-model="form.callId" placeholder="请输入需要接管的 callId" />
            </el-form-item>
            <el-form-item label="目标坐席 ID（可选）">
              <el-input v-model="form.targetAgentId" placeholder="留空则自动选择可用坐席" />
            </el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitTransfer">执行转人工</el-button>
          </el-form>
        </div>
      </div>

      <div class="panel">
        <div class="panel-inner">
          <h2 class="panel-title">路由结果</h2>
          <div v-if="result" class="result-box">
            <p class="result-label">callId</p>
            <p class="result-value">{{ result.callId }}</p>
            <p class="result-meta">目标坐席：{{ result.agentId ?? "-" }}</p>
            <p class="result-meta">分机号：{{ result.extensionNo ?? "-" }}</p>
            <p class="result-meta">状态：{{ formatTransferStatus(result.status) }}</p>
          </div>
          <p v-else class="panel-subtitle">尚未执行转人工。</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { transferCall } from "../api/call.js";

const form = reactive({
  callId: "",
  targetAgentId: ""
});

const result = ref(null);
const submitting = ref(false);

const transferStatusMap = {
  transfer_pending: "待转人工",
  transferred: "已转人工",
  transfer_failed: "转人工失败"
};

async function submitTransfer() {
  if (!form.callId) {
    ElMessage.warning("请输入 callId");
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      callId: form.callId,
      targetAgentId: form.targetAgentId ? Number(form.targetAgentId) : null
    };
    const response = await transferCall(payload);
    if (response.code === 200) {
      result.value = response.data;
      ElMessage.success("转人工执行成功");
      return;
    }
    ElMessage.error(response.msg || "转人工失败");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "转人工失败");
  } finally {
    submitting.value = false;
  }
}

function formatTransferStatus(status) {
  return transferStatusMap[status] || status || "-";
}
</script>

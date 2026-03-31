<template>
  <section class="page">
    <div class="hero">
      <p class="eyebrow">Agents</p>
      <h1>坐席管理</h1>
      <p>用于创建坐席、查询坐席详情，以及绑定或解绑分机。页面直接对接 Phase 2 的坐席接口。</p>
    </div>

    <div class="dashboard-grid">
      <div class="panel">
        <div class="panel-inner">
          <h2 class="panel-title">创建坐席</h2>
          <el-form label-position="top" :model="createForm">
            <el-form-item label="坐席编码">
              <el-input v-model="createForm.agentCode" placeholder="例如 agent-1001" />
            </el-form-item>
            <el-form-item label="坐席名称">
              <el-input v-model="createForm.agentName" placeholder="例如 Alice" />
            </el-form-item>
            <el-button type="primary" :loading="creating" @click="submitCreateAgent">创建坐席</el-button>
          </el-form>
        </div>
      </div>

      <div class="panel">
        <div class="panel-inner">
          <h2 class="panel-title">查询坐席</h2>
          <el-form label-position="top" :model="queryForm">
            <el-form-item label="坐席 ID">
              <el-input v-model="queryForm.agentId" placeholder="例如 1" />
            </el-form-item>
            <el-button type="primary" :loading="querying" @click="submitQueryAgent">查询详情</el-button>
          </el-form>

          <div v-if="agentDetail" class="result-box" style="margin-top: 20px">
            <p class="result-label">当前坐席</p>
            <p class="result-value">{{ agentDetail.agentName }} / {{ agentDetail.agentCode }}</p>
            <p class="result-meta">状态：{{ formatAgentStatus(agentDetail.status) }}</p>
            <p class="result-meta">启用：{{ agentDetail.enabled === 1 ? "是" : "否" }}</p>
            <p class="result-meta">
              分机：{{ agentDetail.extensionNos?.length ? agentDetail.extensionNos.join(", ") : "暂无绑定" }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="panel">
        <div class="panel-inner">
          <h2 class="panel-title">绑定分机</h2>
          <el-form label-position="top" :model="bindForm">
            <el-form-item label="坐席 ID">
              <el-input v-model="bindForm.agentId" placeholder="例如 1" />
            </el-form-item>
            <el-form-item label="分机号">
              <el-input v-model="bindForm.extensionNo" placeholder="例如 1001" />
            </el-form-item>
            <el-button type="primary" :loading="binding" @click="submitBindExtension">绑定分机</el-button>
          </el-form>
        </div>
      </div>

      <div class="panel">
        <div class="panel-inner">
          <h2 class="panel-title">解绑分机</h2>
          <el-form label-position="top" :model="unbindForm">
            <el-form-item label="坐席 ID">
              <el-input v-model="unbindForm.agentId" placeholder="例如 1" />
            </el-form-item>
            <el-form-item label="分机号">
              <el-input v-model="unbindForm.extensionNo" placeholder="例如 1001" />
            </el-form-item>
            <el-button :loading="unbinding" @click="submitUnbindExtension">解绑分机</el-button>
          </el-form>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { bindAgentExtension, createAgent, queryAgent, unbindAgentExtension } from "../api/call.js";

const createForm = reactive({
  agentCode: "",
  agentName: ""
});

const queryForm = reactive({
  agentId: ""
});

const bindForm = reactive({
  agentId: "",
  extensionNo: ""
});

const unbindForm = reactive({
  agentId: "",
  extensionNo: ""
});

const creating = ref(false);
const querying = ref(false);
const binding = ref(false);
const unbinding = ref(false);
const agentDetail = ref(null);

const agentStatusMap = {
  offline: "离线",
  idle: "空闲",
  busy: "忙碌"
};

async function submitCreateAgent() {
  if (!createForm.agentCode || !createForm.agentName) {
    ElMessage.warning("请输入坐席编码和名称");
    return;
  }
  creating.value = true;
  try {
    const result = await createAgent(createForm);
    handleAgentResult(result, "坐席创建成功");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "创建失败");
  } finally {
    creating.value = false;
  }
}

async function submitQueryAgent() {
  if (!queryForm.agentId) {
    ElMessage.warning("请输入坐席 ID");
    return;
  }
  querying.value = true;
  try {
    const result = await queryAgent(queryForm.agentId);
    handleAgentResult(result, "坐席查询成功");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "查询失败");
  } finally {
    querying.value = false;
  }
}

async function submitBindExtension() {
  if (!bindForm.agentId || !bindForm.extensionNo) {
    ElMessage.warning("请输入坐席 ID 和分机号");
    return;
  }
  binding.value = true;
  try {
    const result = await bindAgentExtension({
      agentId: Number(bindForm.agentId),
      extensionNo: bindForm.extensionNo
    });
    handleAgentResult(result, "分机绑定成功");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "绑定失败");
  } finally {
    binding.value = false;
  }
}

async function submitUnbindExtension() {
  if (!unbindForm.agentId || !unbindForm.extensionNo) {
    ElMessage.warning("请输入坐席 ID 和分机号");
    return;
  }
  unbinding.value = true;
  try {
    const result = await unbindAgentExtension({
      agentId: Number(unbindForm.agentId),
      extensionNo: unbindForm.extensionNo
    });
    handleAgentResult(result, "分机解绑成功");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "解绑失败");
  } finally {
    unbinding.value = false;
  }
}

function handleAgentResult(result, successMessage) {
  if (result.code === 200) {
    agentDetail.value = result.data;
    ElMessage.success(successMessage);
    return;
  }
  ElMessage.error(result.msg || "请求失败");
}

function formatAgentStatus(status) {
  return agentStatusMap[status] || status || "-";
}
</script>

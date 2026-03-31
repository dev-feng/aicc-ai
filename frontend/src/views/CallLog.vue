<template>
  <section class="page">
    <div class="hero">
      <p class="eyebrow">Managed Logs</p>
      <h1>受管通话日志</h1>
      <p>这里只查询已经通过受管过滤的通话记录，便于区分业务通话和 FreeSWITCH 内部腿或扫描流量。</p>
    </div>

    <div class="panel">
      <div class="panel-inner">
        <h2 class="panel-title">查询条件</h2>
        <p class="panel-subtitle">请求后端 <code>/api/call/managed-log</code>，按号码和时间范围过滤。</p>

        <div class="filter-bar">
          <el-form label-position="top" :model="filters">
            <el-row :gutter="16">
              <el-col :xs="24" :md="8">
                <el-form-item label="号码">
                  <el-input v-model="filters.phone" placeholder="例如 1002" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="开始时间">
                  <el-date-picker
                    v-model="filters.startTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="选择开始时间"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="结束时间">
                  <el-date-picker
                    v-model="filters.endTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="选择结束时间"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-button type="primary" :loading="loading" @click="searchLogs">查询日志</el-button>
          </el-form>
        </div>
      </div>
    </div>

    <div class="panel table-card">
      <div class="panel-inner">
        <h2 class="panel-title">结果列表</h2>
        <el-table :data="rows" stripe>
          <el-table-column label="呼叫类型" min-width="120">
            <template #default="{ row }">
              <el-tag :type="row.direction === 'inbound' ? 'success' : 'warning'" effect="plain">
                {{ formatDirection(row.direction) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="caller" label="主叫" min-width="120" />
          <el-table-column prop="callee" label="被叫" min-width="120" />
          <el-table-column prop="startTime" label="开始时间" min-width="180" />
          <el-table-column prop="endTime" label="结束时间" min-width="180" />
          <el-table-column prop="durationSec" label="通话时长(秒)" min-width="120" />
          <el-table-column label="挂断原因" min-width="150">
            <template #default="{ row }">
              {{ formatHangupCause(row.hangupCause) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { queryManagedLogs } from "../api/call.js";

const filters = reactive({
  phone: "",
  startTime: "",
  endTime: ""
});

const rows = ref([]);
const loading = ref(false);

const directionMap = {
  inbound: "呼入",
  outbound: "呼出"
};

const hangupCauseMap = {
  NORMAL_CLEARING: "正常挂断",
  NO_ANSWER: "无人接听",
  ORIGINATOR_CANCEL: "主叫取消",
  CALL_REJECTED: "呼叫被拒绝",
  USER_BUSY: "用户忙线"
};

async function searchLogs() {
  if (!filters.phone) {
    ElMessage.warning("请输入号码");
    return;
  }

  loading.value = true;
  try {
    const result = await queryManagedLogs(filters);
    if (result.code === 200) {
      rows.value = result.data ?? [];
      ElMessage.success(`查询完成，返回 ${rows.value.length} 条记录`);
      return;
    }
    ElMessage.error(result.msg || "查询失败");
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || "查询失败");
  } finally {
    loading.value = false;
  }
}

function formatDirection(direction) {
  return directionMap[direction] || direction || "-";
}

function formatHangupCause(cause) {
  return hangupCauseMap[cause] || cause || "-";
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-badge">CC</span>
        <div>
          <p class="brand-title">Call Center</p>
          <p class="brand-subtitle">Phase 2 Console</p>
        </div>
      </div>

      <nav class="nav">
        <RouterLink class="nav-link" to="/">外呼控制台</RouterLink>
        <RouterLink class="nav-link" to="/managed-logs">受管通话日志</RouterLink>
        <RouterLink class="nav-link" to="/agents">坐席管理</RouterLink>
        <RouterLink class="nav-link" to="/transfer">人工接管</RouterLink>
      </nav>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from "vue-router";
</script>

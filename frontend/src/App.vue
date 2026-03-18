<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-badge">CC</span>
        <div>
          <p class="brand-title">Call Center</p>
          <p class="brand-subtitle">Phase 1 Console</p>
        </div>
      </div>

      <nav class="nav">
        <RouterLink class="nav-link" to="/">发起外呼</RouterLink>
        <RouterLink class="nav-link" to="/logs">通话日志</RouterLink>
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

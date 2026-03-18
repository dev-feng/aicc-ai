import { createRouter, createWebHistory } from "vue-router";
import OutboundCall from "../views/OutboundCall.vue";
import CallLog from "../views/CallLog.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "outbound", component: OutboundCall },
    { path: "/logs", name: "logs", component: CallLog }
  ]
});

export default router;

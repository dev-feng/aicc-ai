import { createRouter, createWebHistory } from "vue-router";
import OutboundCall from "../views/OutboundCall.vue";
import CallLog from "../views/CallLog.vue";
import AgentConsole from "../views/AgentConsole.vue";
import TransferConsole from "../views/TransferConsole.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "outbound", component: OutboundCall },
    { path: "/managed-logs", name: "managed-logs", component: CallLog },
    { path: "/agents", name: "agents", component: AgentConsole },
    { path: "/transfer", name: "transfer", component: TransferConsole }
  ]
});

export default router;

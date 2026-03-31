import axios from "axios";

const client = axios.create({
  baseURL: "/api",
  timeout: 10000
});

export async function createOutboundCall(payload) {
  const response = await client.post("/call/outbound", payload);
  return response.data;
}

export async function queryCallLogs(params) {
  const response = await client.get("/call/log", { params });
  return response.data;
}

export async function queryManagedLogs(params) {
  const response = await client.get("/call/managed-log", { params });
  return response.data;
}

export async function transferCall(payload) {
  const response = await client.post("/call/transfer", payload);
  return response.data;
}

export async function createAgent(payload) {
  const response = await client.post("/agent", payload);
  return response.data;
}

export async function queryAgent(agentId) {
  const response = await client.get(`/agent/${agentId}`);
  return response.data;
}

export async function bindAgentExtension(payload) {
  const response = await client.post("/agent/bind-extension", payload);
  return response.data;
}

export async function unbindAgentExtension(payload) {
  const response = await client.post("/agent/unbind-extension", payload);
  return response.data;
}

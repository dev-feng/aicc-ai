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

import http from "@/utils/http";

export const getEcosystemList = () =>
  http.get("/ecosystem/list");

export const getEcosystemPage = (params) =>
  http.get("/ecosystem", { params });

export const getEcosystemById = (id) =>
  http.get(`/ecosystem/${id}`);

export const createEcosystem = (data) =>
  http.post("/ecosystem", data);

export const updateEcosystem = (id, data) =>
  http.put(`/ecosystem/${id}`, data);

export const deleteEcosystem = (id) =>
  http.delete(`/ecosystem/${id}`);

export const suggestEcosystemByAI = (name) =>
  http.post("/ai/ecosystem/suggest", { name });

export const uploadEcosystemImage = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/ecosystem/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
};

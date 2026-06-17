import http from "@/utils/http";

export const getSpeciesPage = (params) =>
  http.get("/species", { params });

export const getSpeciesById = (id) =>
  http.get(`/species/${id}`);

export const createSpecies = (data) =>
  http.post("/species", data);

export const updateSpecies = (id, data) =>
  http.put(`/species/${id}`, data);

export const updateSpeciesByBody = (data) =>
  http.put("/species", data);

export const deleteSpecies = (id) =>
  http.delete(`/species/${id}`);

export const uploadSpeciesImage = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/species/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
};

export const suggestSpeciesByAI = (chineseName) =>
  http.post("/ai/species/suggest", { chineseName });

export const suggestSpecies = (keyword, limit = 5) =>
  http.get("/species/suggest", { params: { keyword, limit } });

export const importSpeciesCsv = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/species/import-csv", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
};

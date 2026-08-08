const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      ...(options.body instanceof globalThis.FormData
        ? {}
        : { "Content-Type": "application/json" }),
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function loadDashboardData() {
  const [storeInventory, depotInventory, forecasts, recommendations] =
    await Promise.all([
      request("/api/inventory/stores"),
      request("/api/inventory/depot"),
      request("/api/forecasts"),
      request("/api/recommendations"),
    ]);

  return {
    storeInventory,
    depotInventory,
    forecasts,
    recommendations,
  };
}

export function loadDepotInventory() {
  return request("/api/inventory/depot");
}

export function loadStores() {
  return request("/api/stores");
}

export function loadProducts() { return request("/api/products"); }
export function createProduct(payload) { return request("/api/products", { method: "POST", body: JSON.stringify(payload) }); }
export function updateProduct(id, payload) { return request(`/api/products/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function deleteProduct(id) { return request(`/api/products/${id}`, { method: "DELETE" }); }

export function createStore(payload) {
  return request("/api/stores", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateStore(id, payload) {
  return request(`/api/stores/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteStore(id) {
  return request(`/api/stores/${id}`, { method: "DELETE" });
}

export function updateDepotInventory(productId, availableUnits, reservedUnits) {
  return request("/api/inventory/depot", {
    method: "POST",
    body: JSON.stringify({ productId, availableUnits, reservedUnits }),
  });
}

export function syncMlRecommendations() {
  return request("/api/ml/sync", { method: "POST" });
}

export function overrideRecommendationAmount(id, recommendedShipment, reason) {
  return request(`/api/recommendations/${id}/override`, {
    method: "PATCH",
    body: JSON.stringify({ recommendedShipment, reason, overriddenBy: "SM" }),
  });
}

export function updateRecommendationStatus(id, status) {
  return request(`/api/recommendations/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export async function loadShipmentPageData() {
  const [shipments, approvedRecommendations] = await Promise.all([
    request("/api/shipments"),
    request("/api/recommendations?status=APPROVED"),
  ]);

  return { shipments, approvedRecommendations };
}

export function createShipment(payload) {
  return request("/api/shipments", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateShipmentStatus(id, status) {
  return request(`/api/shipments/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export function importHistoricalSalesCsv(file) {
  const formData = new globalThis.FormData();
  formData.append("file", file);

  return request("/api/imports/sales-records", {
    method: "POST",
    body: formData,
  });
}

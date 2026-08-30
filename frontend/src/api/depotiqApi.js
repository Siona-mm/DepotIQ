const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";
const AUTH_STORAGE_KEY = "depotiq-basic-auth";

function authorizationHeader() {
  const token = globalThis.sessionStorage.getItem(AUTH_STORAGE_KEY);
  return token ? { Authorization: `Basic ${token}` } : {};
}

function messageForStatus(status, apiMessage) {
  if (status === 401) {
    return "Your session has expired. Please sign in again.";
  }

  if (status === 403) {
    return "You do not have permission to complete this action.";
  }

  if (status === 404) {
    return apiMessage || "The requested record could not be found. Refresh the page and try again.";
  }

  if (status === 409) {
    return apiMessage || "This change conflicts with existing data. Refresh the page and try again.";
  }

  if (status === 502 || status === 503 || status === 504) {
    return "A connected service is currently unavailable. Please try again shortly.";
  }

  if (status >= 500) {
    return "The server could not complete this request. Please try again shortly.";
  }

  return apiMessage || "We could not complete this request. Please review the information and try again.";
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        ...(options.body instanceof globalThis.FormData
          ? {}
          : { "Content-Type": "application/json" }),
        ...authorizationHeader(),
        ...options.headers,
      },
      ...options,
    });
  } catch {
    throw new Error("Could not reach DepotIQ. Check that the backend is running, then try again.");
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const validationMessage = Object.values(body?.validationErrors ?? {})[0];
    throw new Error(messageForStatus(
      response.status,
      validationMessage ?? body?.message,
    ));
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function signIn(username, password) {
  const token = globalThis.btoa(`${username}:${password}`);
  const user = await request("/api/auth/me", {
    headers: { Authorization: `Basic ${token}` },
  });
  globalThis.sessionStorage.setItem(AUTH_STORAGE_KEY, token);
  return user;
}

export function signOut() {
  globalThis.sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

export function loadAuthenticatedUser() {
  return globalThis.sessionStorage.getItem(AUTH_STORAGE_KEY)
    ? request("/api/auth/me")
    : Promise.resolve(null);
}

export async function updateCredentials(payload) {
  const user = await request("/api/auth/credentials", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  const password = payload.newPassword || payload.currentPassword;
  const token = globalThis.btoa(`${user.username}:${password}`);
  globalThis.sessionStorage.setItem(AUTH_STORAGE_KEY, token);
  return user;
}

export function loadProfile() {
  return request("/api/profile/me");
}

export function updateProfile(payload) {
  return request("/api/profile/me", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function loadSettings() {
  return request("/api/settings/me");
}

export function updateSettings(payload) {
  return request("/api/settings/me", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function resetSettings() {
  return request("/api/settings/me", { method: "DELETE" });
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

export function loadStoreInventory() {
  return request("/api/inventory/stores");
}

export function updateStoreInventory(payload) {
  return request("/api/inventory/stores", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loadStores() {
  return request("/api/stores");
}

export function loadProducts() { return request("/api/products"); }
export function createProduct(payload) { return request("/api/products", { method: "POST", body: JSON.stringify(payload) }); }
export function updateProduct(id, payload) { return request(`/api/products/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function deleteProduct(id) { return request(`/api/products/${id}`, { method: "DELETE" }); }

export function loadForecasts() { return request("/api/forecasts"); }


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

export function loadMlStatus() {
  return request("/api/ml/status");
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

export function loadRecommendationHistory() {
  return request("/api/recommendations");
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

export function approveAndDispatchShipment(recommendationIds) {
  return request("/api/shipments/approve-and-dispatch", {
    method: "POST",
    body: JSON.stringify({ recommendationIds }),
  });
}

export function updateShipmentStatus(id, status) {
  return request(`/api/shipments/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export function importCsv(file, type = "sales-records", receiptId = "") {
  const formData = new globalThis.FormData();
  formData.append("file", file);
  if (receiptId.trim()) formData.append("receiptId", receiptId.trim());
  return request(`/api/imports/${type}`, { method: "POST", body: formData });
}

export function importHistoricalSalesCsv(file) {
  return importCsv(file);
}

export function loadImportHistory() {
  return request("/api/imports");
}

export function loadOperationalActivity() {
  return request("/api/activity");
}

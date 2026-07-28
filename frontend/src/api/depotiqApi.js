const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
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

export function syncMlRecommendations() {
  return request("/api/ml/sync", { method: "POST" });
}

export function overrideRecommendationAmount(id, recommendedShipment, reason) {
  return request(`/api/recommendations/${id}/override`, {
    method: "PATCH",
    body: JSON.stringify({ recommendedShipment, reason, overriddenBy: "SM" }),
  });
}

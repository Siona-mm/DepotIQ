const ACTIONABLE_ROLES = new Set(["ROLE_ADMIN", "ROLE_MANAGER"]);

function roleCanTakeAction(user) {
  return user?.roles?.some((role) => ACTIONABLE_ROLES.has(role));
}

function labelFor(item) {
  return [item.storeCode, item.productCode].filter(Boolean).join(" · ") || "Store inventory";
}

export function buildOperationalNotifications(
  { recommendations = [], shipments = [], storeInventory = [] },
  user,
  settings = {},
) {
  const canTakeAction = roleCanTakeAction(user);
  const alertThreshold = Number(settings.alertThreshold ?? 250);
  const lowStockInventory = storeInventory.filter(
    (item) => Number(item.inventoryLevel) <= alertThreshold,
  );
  const notifications = [];
  const urgentRecommendations = recommendations.filter(
    (item) => item.priority === "URGENT" && !["DELIVERED", "REJECTED"].includes(item.status),
  );
  const pendingRecommendations = recommendations.filter(
    (item) => ["PENDING", "EDITED"].includes(item.status),
  );
  const activeShipments = shipments.filter(
    (item) => !["DELIVERED", "CANCELLED"].includes(item.status),
  );

  urgentRecommendations.slice(0, 3).forEach((item) => {
    notifications.push({
      id: `urgent-recommendation-${item.id}`,
      tone: "urgent",
      title: "Urgent replenishment needed",
      detail: `${labelFor(item)} needs ${Number(item.recommendedShipment || 0).toLocaleString()} units.`,
      page: "Dashboard",
    });
  });

  if (canTakeAction && pendingRecommendations.length) {
    notifications.push({
      id: "pending-recommendations",
      tone: "action",
      title: `${pendingRecommendations.length} recommendation${pendingRecommendations.length === 1 ? "" : "s"} awaiting review`,
      detail: "Review pending shipment recommendations before stock levels are affected.",
      page: "Dashboard",
    });
  }

  if (settings.emailAlerts && lowStockInventory.length) {
    notifications.push({
      id: `low-stock-under-${alertThreshold}`,
      tone: "urgent",
      title: `${lowStockInventory.length} low-stock item${lowStockInventory.length === 1 ? "" : "s"} need attention`,
      detail: `Current stock is at or below your ${alertThreshold.toLocaleString()} unit alert threshold.`,
      page: "Store Inventory",
    });
  }

  if (activeShipments.length) {
    notifications.push({
      id: "active-shipments",
      tone: "update",
      title: `${activeShipments.length} active shipment${activeShipments.length === 1 ? "" : "s"}`,
      detail: "Track current shipments and their latest delivery status.",
      page: "Shipments",
    });
  }

  return notifications;
}

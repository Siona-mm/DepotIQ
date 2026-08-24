import { Bell, CheckCheck, CircleAlert, PackageCheck, RefreshCw, TriangleAlert } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { loadDashboardData, loadSettings, loadShipmentPageData } from "../api/depotiqApi.js";
import { buildOperationalNotifications } from "../notifications/operationalNotifications.js";

function iconFor(tone) {
  if (tone === "urgent") return <TriangleAlert aria-hidden="true" size={16} />;
  if (tone === "action") return <CircleAlert aria-hidden="true" size={16} />;
  return <PackageCheck aria-hidden="true" size={16} />;
}

export default function NotificationCenter({ onNavigate, user }) {
  const [open, setOpen] = useState(false);
  const [readIds, setReadIds] = useState([]);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [data, setData] = useState({
    recommendations: [],
    shipments: [],
    storeInventory: [],
    settings: { alertThreshold: 250, emailAlerts: false },
  });
  const popoverRef = useRef(null);
  const readStorageKey = `depotiq-read-notifications-${user?.username || "anonymous"}`;
  const notifications = useMemo(
    () => buildOperationalNotifications(data, user, data.settings),
    [data, user],
  );
  const unreadCount = notifications.filter((item) => !readIds.includes(item.id)).length;

  useEffect(() => {
    let active = true;

    const refreshNotifications = () => {
      Promise.allSettled([loadDashboardData(), loadShipmentPageData(), loadSettings()])
        .then(([dashboardResult, shipmentResult, settingsResult]) => {
          if (active) {
            setData({
              recommendations: dashboardResult.status === "fulfilled"
                ? dashboardResult.value.recommendations
                : [],
              shipments: shipmentResult.status === "fulfilled"
                ? shipmentResult.value.shipments
                : [],
              storeInventory: dashboardResult.status === "fulfilled"
                ? dashboardResult.value.storeInventory
                : [],
              settings: settingsResult.status === "fulfilled"
                ? settingsResult.value
                : { alertThreshold: 250, emailAlerts: false },
            });
            setLastUpdated(new Date());
          }
        });
    };

    refreshNotifications();
    globalThis.addEventListener("depotiq-settings-updated", refreshNotifications);

    return () => {
      active = false;
      globalThis.removeEventListener("depotiq-settings-updated", refreshNotifications);
    };
  }, []);

  useEffect(() => {
    if (!data.settings.autoRefresh) return undefined;

    const timer = globalThis.setInterval(
      () => globalThis.dispatchEvent(new globalThis.Event("depotiq-settings-updated")),
      60000,
    );
    return () => globalThis.clearInterval(timer);
  }, [data.settings.autoRefresh]);

  useEffect(() => {
    try {
      const stored = JSON.parse(globalThis.sessionStorage.getItem(readStorageKey) || "[]");
      setReadIds(Array.isArray(stored) ? stored : []);
    } catch {
      setReadIds([]);
    }
  }, [readStorageKey]);

  useEffect(() => {
    const closeFromOutside = (event) => {
      if (!popoverRef.current?.contains(event.target)) setOpen(false);
    };
    const closeFromEscape = (event) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("pointerdown", closeFromOutside);
    document.addEventListener("keydown", closeFromEscape);
    return () => {
      document.removeEventListener("pointerdown", closeFromOutside);
      document.removeEventListener("keydown", closeFromEscape);
    };
  }, []);

  const saveReadIds = (next) => {
    setReadIds(next);
    globalThis.sessionStorage.setItem(readStorageKey, JSON.stringify(next));
  };
  const markRead = (id) => {
    if (!readIds.includes(id)) saveReadIds([...readIds, id]);
  };

  return (
    <div className="notification-center" ref={popoverRef}>
      <button
        aria-expanded={open}
        aria-haspopup="dialog"
        aria-label={unreadCount ? `${unreadCount} unread notifications` : "Open notifications"}
        className="notification-trigger"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        <Bell aria-hidden="true" size={18} />
        {unreadCount > 0 && <span className="notification-badge">{unreadCount > 9 ? "9+" : unreadCount}</span>}
      </button>

      {open && (
        <section aria-label="Notifications" className="notification-popover" role="dialog">
          <header>
            <div>
              <span>Operational updates</span>
              <h2>Notifications</h2>
            </div>
            {unreadCount > 0 && (
              <button className="notification-mark-all" onClick={() => saveReadIds(notifications.map((item) => item.id))} type="button">
                <CheckCheck aria-hidden="true" size={14} />
                Mark all read
              </button>
            )}
            <button
              aria-label="Refresh notifications"
              className="notification-refresh"
              onClick={() => globalThis.dispatchEvent(new globalThis.Event("depotiq-settings-updated"))}
              title="Refresh notifications"
              type="button"
            >
              <RefreshCw aria-hidden="true" size={14} />
            </button>
          </header>

          {lastUpdated && <p className="notification-updated">Updated {lastUpdated.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</p>}

          <div className="notification-list">
            {notifications.length ? notifications.map((item) => {
              const isUnread = !readIds.includes(item.id);
              return (
                <button
                  className={`notification-item ${item.tone} ${isUnread ? "unread" : ""}`}
                  key={item.id}
                  onClick={() => {
                    markRead(item.id);
                    setOpen(false);
                    onNavigate(item.page);
                  }}
                  type="button"
                >
                  <span className="notification-icon">{iconFor(item.tone)}</span>
                  <span>
                    <strong>{item.title}</strong>
                    <small>{item.detail}</small>
                  </span>
                </button>
              );
            }) : (
              <div className="notification-empty">
                <CheckCheck aria-hidden="true" size={18} />
                <strong>You are all caught up</strong>
                <span>New inventory and shipment updates will appear here.</span>
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}

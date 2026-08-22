import { Bell, CheckCheck, CircleAlert, PackageCheck, TriangleAlert } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { loadDashboardData, loadShipmentPageData } from "../api/depotiqApi.js";
import { buildOperationalNotifications } from "../notifications/operationalNotifications.js";

function iconFor(tone) {
  if (tone === "urgent") return <TriangleAlert aria-hidden="true" size={16} />;
  if (tone === "action") return <CircleAlert aria-hidden="true" size={16} />;
  return <PackageCheck aria-hidden="true" size={16} />;
}

export default function NotificationCenter({ onNavigate, user }) {
  const [open, setOpen] = useState(false);
  const [readIds, setReadIds] = useState([]);
  const [data, setData] = useState({ recommendations: [], shipments: [] });
  const popoverRef = useRef(null);
  const notifications = useMemo(
    () => buildOperationalNotifications(data, user),
    [data, user],
  );
  const unreadCount = notifications.filter((item) => !readIds.includes(item.id)).length;

  useEffect(() => {
    let active = true;

    Promise.all([loadDashboardData(), loadShipmentPageData()])
      .then(([dashboard, shipmentData]) => {
        if (active) {
          setData({
            recommendations: dashboard.recommendations,
            shipments: shipmentData.shipments,
          });
        }
      })
      .catch(() => {
        if (active) setData({ recommendations: [], shipments: [] });
      });

    return () => { active = false; };
  }, []);

  useEffect(() => {
    const closeFromOutside = (event) => {
      if (!popoverRef.current?.contains(event.target)) setOpen(false);
    };

    document.addEventListener("pointerdown", closeFromOutside);
    return () => document.removeEventListener("pointerdown", closeFromOutside);
  }, []);

  const markRead = (id) => setReadIds((current) => current.includes(id) ? current : [...current, id]);

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
              <button className="notification-mark-all" onClick={() => setReadIds(notifications.map((item) => item.id))} type="button">
                <CheckCheck aria-hidden="true" size={14} />
                Mark all read
              </button>
            )}
          </header>

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

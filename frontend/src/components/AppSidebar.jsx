import {
  Boxes,
  ChartNoAxesCombined,
  ChevronsLeft,
  FileChartColumn,
  LayoutDashboard,
  PackageOpen,
  PackageSearch,
  Settings,
  Store,
  Truck,
  Upload,
  Warehouse,
} from "lucide-react";

const NAVIGATION = [
  [LayoutDashboard, "Dashboard", true],
  [Warehouse, "Depot Inventory", true],
  [Boxes, "Store Inventory", true],
  [Store, "Stores", false],
  [PackageSearch, "Products", false],
  [ChartNoAxesCombined, "Forecasts", false],
  [Truck, "Shipments", true],
  [Upload, "Upload Data", false, "upload"],
  [FileChartColumn, "Reports", true],
  [Settings, "Profile", true],
  [Settings, "Settings", true],
];

export default function AppSidebar({
  activePage,
  collapsed,
  onCollapse,
  onNavigate,
  onAction,
  onSignOut,
  user,
}) {
  return (
    <aside className={collapsed ? "sidebar collapsed" : "sidebar"}>
      <div className="brand">
        <PackageOpen aria-hidden="true" size={29} strokeWidth={1.8} />
        <span>DepotIQ</span>
      </div>

      <nav aria-label="Main navigation">
        {NAVIGATION.map(([Icon, label, routeAvailable, action]) => {
          const available = routeAvailable || Boolean(action && onAction);

          return (
            <button
              aria-current={activePage === label ? "page" : undefined}
              className={
                activePage === label ? "nav-item active" : "nav-item"
              }
              disabled={!available}
              key={label}
              onClick={() =>
                action ? onAction?.(action) : onNavigate(label)
              }
              title={available ? label : `${label} is coming next`}
              type="button"
            >
              <Icon aria-hidden="true" size={18} strokeWidth={1.8} />
              <span>{label}</span>
            </button>
          );
        })}
      </nav>

      {user && (
        <div className="sidebar-user">
          <button className="sidebar-profile" onClick={() => onNavigate("Profile")} type="button">
            <strong>{user.username}</strong>
            <span>{user.roles?.[0]?.replace("ROLE_", "") ?? "User"}</span>
          </button>
          <button onClick={onSignOut} type="button">
            Sign out
          </button>
        </div>
      )}

      <button
        aria-expanded={!collapsed}
        className="collapse-button"
        onClick={onCollapse}
        title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        type="button"
      >
        <ChevronsLeft aria-hidden="true" size={16} />
        <span>Collapse</span>
      </button>
    </aside>
  );
}

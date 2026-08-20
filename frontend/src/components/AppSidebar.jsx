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
  [Store, "Stores", "catalog"],
  [PackageSearch, "Products", "catalog"],
  [ChartNoAxesCombined, "Forecasts", "forecasts"],
  [Truck, "Shipments", true],
  [Upload, "Upload Data", false, "upload"],
  [FileChartColumn, "Reports", true],
  [Settings, "Settings", true],
];

export default function AppSidebar({
  activePage,
  collapsed,
  onCollapse,
  onNavigate,
  onAction,
  permissions,
}) {
  return (
    <aside className={collapsed ? "sidebar collapsed" : "sidebar"}>
      <div className="brand">
        <PackageOpen aria-hidden="true" size={29} strokeWidth={1.8} />
        <span>DepotIQ</span>
      </div>

      <nav aria-label="Main navigation">
        {NAVIGATION.filter(([, , access, action]) =>
          (access !== "catalog" || permissions?.canViewCatalog) &&
          (access !== "forecasts" || permissions?.canViewForecasts) &&
          (action !== "upload" || permissions?.canImportData),
        ).map(([Icon, label, access, action]) => {
          const available = access === "catalog" || access === "forecasts" || Boolean(action && onAction) || access;

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

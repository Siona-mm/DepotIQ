import {
  Boxes,
  ChartNoAxesCombined,
  ChevronsLeft,
  FileChartColumn,
  History,
  LayoutDashboard,
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
  [Store, "Stores", true, undefined, "catalog"],
  [PackageSearch, "Products", true, undefined, "catalog"],
  [ChartNoAxesCombined, "Forecasts", true, undefined, "forecasts"],
  [Truck, "Shipments", true],
  [History, "History", true],
  [Upload, "Upload Data", false, "upload", "import"],
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
        <img alt="" aria-hidden="true" src="/depotiq-mark.png" style={{ width: 30, height: 30, objectFit: "contain" }} />
        <span>DepotIQ</span>
      </div>

      <nav aria-label="Main navigation">
        {NAVIGATION.filter(([, , , , access]) => {
          if (access === "catalog") return permissions?.canViewCatalog;
          if (access === "forecasts") return permissions?.canViewForecasts;
          if (access === "import") return permissions?.canImportData;
          return true;
        }).map(([Icon, label, routeAvailable, action]) => {
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

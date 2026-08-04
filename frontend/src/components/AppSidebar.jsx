import {
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
  [Warehouse, "Depot Inventory", false],
  [Store, "Stores", false],
  [PackageSearch, "Products", false],
  [ChartNoAxesCombined, "Forecasts", false],
  [Truck, "Shipments", true],
  [Upload, "Upload Data", false],
  [FileChartColumn, "Reports", false],
  [Settings, "Settings", false],
];

export default function AppSidebar({
  activePage,
  collapsed,
  onCollapse,
  onNavigate,
}) {
  return (
    <aside className={collapsed ? "sidebar collapsed" : "sidebar"}>
      <div className="brand">
        <PackageOpen aria-hidden="true" size={29} strokeWidth={1.8} />
        <span>DepotIQ</span>
      </div>

      <nav aria-label="Main navigation">
        {NAVIGATION.map(([Icon, label, available]) => (
          <button
            aria-current={activePage === label ? "page" : undefined}
            className={
              activePage === label ? "nav-item active" : "nav-item"
            }
            disabled={!available}
            key={label}
            onClick={() => available && onNavigate(label)}
            title={available ? label : `${label} is coming next`}
            type="button"
          >
            <Icon aria-hidden="true" size={18} strokeWidth={1.8} />
            <span>{label}</span>
          </button>
        ))}
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

import {
  AlertTriangle,
  Boxes,
  ChartNoAxesCombined,
  ChevronLeft,
  ClipboardList,
  LayoutDashboard,
  PackageSearch,
  RefreshCw,
  Search,
  Settings,
  Store,
  Truck,
  Upload,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  loadDashboardData,
  syncMlRecommendations,
} from "../api/depotiqApi.js";

const EMPTY_DATA = {
  storeInventory: [],
  depotInventory: [],
  forecasts: [],
  recommendations: [],
};

const NAVIGATION = [
  [LayoutDashboard, "Dashboard", true],
  [Boxes, "Depot inventory"],
  [Store, "Stores"],
  [PackageSearch, "Products"],
  [ChartNoAxesCombined, "Forecasts"],
  [Truck, "Shipments"],
  [Upload, "Upload data"],
  [ClipboardList, "Reports"],
  [Settings, "Settings"],
];

function formatNumber(value) {
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(
    Number(value ?? 0),
  );
}

function Metric({ icon: Icon, label, value, note }) {
  return (
    <article className="metric">
      <Icon aria-hidden="true" size={19} strokeWidth={1.7} />
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        <small>{note}</small>
      </div>
    </article>
  );
}

export default function DashboardView() {
  const [data, setData] = useState(EMPTY_DATA);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");
  const [syncMessage, setSyncMessage] = useState("");

  const load = useCallback(async () => {
    setError("");

    try {
      setData(await loadDashboardData());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const summary = useMemo(() => {
    const urgent = data.recommendations.filter(
      (item) => item.priority === "URGENT",
    ).length;
    const pending = data.recommendations.filter(
      (item) => item.status === "PENDING",
    ).length;
    const stores = new Set(data.storeInventory.map((item) => item.storeId)).size;
    const freeDepotUnits = data.depotInventory.reduce(
      (total, item) => total + Number(item.freeUnits ?? 0),
      0,
    );

    return { urgent, pending, stores, freeDepotUnits };
  }, [data]);

  const handleSync = async () => {
    setSyncing(true);
    setError("");
    setSyncMessage("");

    try {
      const result = await syncMlRecommendations();
      setSyncMessage(
        `${result.recommendationsSynced} recommendations synced; ` +
          `${result.skippedUnknownStoreOrProduct} unmatched rows skipped.`,
      );
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <Boxes aria-hidden="true" size={24} strokeWidth={1.8} />
          <span>DepotIQ</span>
        </div>

        <nav aria-label="Main navigation">
          {NAVIGATION.map(([Icon, label, active]) => (
            <button
              className={active ? "nav-item active" : "nav-item"}
              key={label}
              type="button"
            >
              <Icon aria-hidden="true" size={18} strokeWidth={1.7} />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <button className="collapse-button" type="button" title="Collapse sidebar">
          <ChevronLeft aria-hidden="true" size={17} />
          <span>Collapse</span>
        </button>
      </aside>

      <main className="dashboard">
        <header className="topbar">
          <div>
            <p className="section-label">Operations overview</p>
            <h1>Dashboard</h1>
          </div>
          <div className="topbar-actions">
            <label className="search-box">
              <Search aria-hidden="true" size={16} />
              <span className="sr-only">Search</span>
              <input placeholder="Search stores or products" type="search" />
            </label>
            <button
              className="primary-button"
              disabled={syncing}
              onClick={handleSync}
              type="button"
            >
              <RefreshCw
                aria-hidden="true"
                className={syncing ? "spinning" : ""}
                size={16}
              />
              {syncing ? "Syncing" : "Sync ML"}
            </button>
            <div className="avatar" aria-label="Signed in as SM">
              SM
            </div>
          </div>
        </header>

        {error && (
          <div className="notice error" role="alert">
            <AlertTriangle aria-hidden="true" size={17} />
            <span>{error}</span>
          </div>
        )}
        {syncMessage && <div className="notice">{syncMessage}</div>}

        <section className="metrics" aria-label="Depot summary">
          <Metric
            icon={AlertTriangle}
            label="Stockout risks"
            note="Urgent recommendations"
            value={summary.urgent}
          />
          <Metric
            icon={Truck}
            label="Pending shipments"
            note="Awaiting transport planning"
            value={summary.pending}
          />
          <Metric
            icon={ChartNoAxesCombined}
            label="Active forecasts"
            note="Latest model outputs"
            value={data.forecasts.length}
          />
          <Metric
            icon={Store}
            label="Stores supplied"
            note="Connected to this depot"
            value={summary.stores}
          />
        </section>

        <section className="workspace">
          <div className="table-panel">
            <div className="panel-heading">
              <div>
                <p className="section-label">Planning queue</p>
                <h2>Shipment recommendations</h2>
              </div>
              <span>{data.recommendations.length} records</span>
            </div>

            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Store</th>
                    <th>Product</th>
                    <th>Stock</th>
                    <th>Forecast</th>
                    <th>Range</th>
                    <th>Send</th>
                    <th>Priority</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recommendations.slice(0, 12).map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.storeCode}</strong>
                        <small>{item.storeName}</small>
                      </td>
                      <td>
                        <strong>{item.productCode}</strong>
                        <small>{item.productName}</small>
                      </td>
                      <td>{formatNumber(item.currentInventory)}</td>
                      <td>{formatNumber(item.predictedDemand)}</td>
                      <td>
                        {formatNumber(item.confidenceLower)}-
                        {formatNumber(item.confidenceUpper)}
                      </td>
                      <td>
                        <strong>{formatNumber(item.recommendedShipment)}</strong>
                      </td>
                      <td>
                        <span className={`priority ${item.priority.toLowerCase()}`}>
                          {item.priority}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {!loading && data.recommendations.length === 0 && (
              <div className="empty-state">
                <Truck aria-hidden="true" size={25} strokeWidth={1.5} />
                <strong>No recommendations yet</strong>
                <span>Start the ML service, then select Sync ML.</span>
              </div>
            )}
            {loading && <div className="empty-state">Loading depot data...</div>}
          </div>

          <aside className="insights">
            <div className="panel-heading">
              <div>
                <p className="section-label">Depot capacity</p>
                <h2>Available stock</h2>
              </div>
              <strong>{formatNumber(summary.freeDepotUnits)}</strong>
            </div>

            <div className="inventory-list">
              {data.depotInventory.map((item) => {
                const maximum = Math.max(
                  ...data.depotInventory.map((entry) => entry.freeUnits),
                  1,
                );
                const width = `${Math.max((item.freeUnits / maximum) * 100, 2)}%`;

                return (
                  <div className="inventory-row" key={item.id}>
                    <div>
                      <strong>{item.productName}</strong>
                      <span>{item.productCode}</span>
                    </div>
                    <b>{formatNumber(item.freeUnits)}</b>
                    <div className="stock-bar">
                      <span style={{ width }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </aside>
        </section>
      </main>
    </div>
  );
}

import { ChartNoAxesCombined, Search, Target } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { loadForecasts } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

const formatNumber = (value) => new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(Number(value ?? 0));

export default function ForecastsView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, user }) {
  const [forecasts, setForecasts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    try { setError(""); setForecasts(await loadForecasts()); }
    catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const visible = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return !normalized ? forecasts : forecasts.filter((item) =>
      [item.storeCode, item.storeName, item.productCode, item.productName, item.category, item.modelName]
        .some((value) => String(value ?? "").toLowerCase().includes(normalized)),
    );
  }, [forecasts, query]);

  const averageMae = forecasts.length
    ? forecasts.reduce((sum, item) => sum + Number(item.modelMae ?? 0), 0) / forecasts.length
    : 0;

  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Forecasts" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} user={user} />
    <main className="dashboard forecasts-page">
      <header className="topbar"><h1>Forecasts</h1><label className="search-box"><Search size={15} /><span className="sr-only">Search forecasts</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Search store, product, category, or model..." value={query} /></label></header>
      {error && <div className="notice error" role="status">{error}</div>}
      <section className="metrics-grid"><article className="metric-card"><ChartNoAxesCombined size={20} /><div><span>Active Forecasts</span><strong>{forecasts.length}</strong><small>Store-product predictions</small></div></article><article className="metric-card"><Target size={20} /><div><span>Average Model MAE</span><strong>{averageMae.toFixed(1)}</strong><small>Across all forecasts</small></div></article></section>
      <section className="table-panel"><div className="panel-toolbar"><h2>Demand forecasts</h2><span>{visible.length} shown</span></div><div className="table-scroll"><table><thead><tr><th>Store</th><th>Product</th><th>Category</th><th>Date</th><th>Horizon</th><th>Predicted demand</th><th>Confidence range</th><th>Model</th></tr></thead><tbody>{visible.map((item) => <tr key={item.id}><td>{item.storeCode}</td><td>{item.productCode}</td><td>{item.category}</td><td>{item.forecastDate}</td><td>{item.horizonDays} days</td><td>{formatNumber(item.predictedDemand)}</td><td>{formatNumber(item.confidenceLower)} – {formatNumber(item.confidenceUpper)}</td><td>{item.modelName || "—"}</td></tr>)}</tbody></table></div>{loading && <div className="panel-empty">Loading forecasts...</div>}{!loading && !visible.length && <div className="panel-empty">No forecasts match your search.</div>}</section>
    </main>
  </div>;
}

import { ChartNoAxesCombined, Search, Target } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { loadForecasts } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import UserAvatar from "../components/UserAvatar.jsx";

const formatNumber = (value) => new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(Number(value ?? 0));
const formatCompact = (value) => new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 }).format(Number(value ?? 0));
const CHART_TOOLTIP_STYLE = { border: "1px solid #e5e7eb", borderRadius: "6px", boxShadow: "0 8px 24px rgba(17, 17, 17, 0.08)", fontSize: "12px" };

export default function ForecastsView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user, dataRevision = 0 }) {
  const [forecasts, setForecasts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    try { setError(""); setForecasts(await loadForecasts()); }
    catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [dataRevision, load]);

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

  const horizonSummary = useMemo(() => {
    const grouped = new Map();
    visible.forEach((forecast) => {
      const horizon = Number(forecast.horizonDays ?? 0);
      const current = grouped.get(horizon) ?? { horizon, demand: 0, confidenceWidth: 0, forecasts: 0 };
      current.demand += Number(forecast.predictedDemand ?? 0);
      current.confidenceWidth += Math.max(0, Number(forecast.confidenceUpper ?? 0) - Number(forecast.confidenceLower ?? 0));
      current.forecasts += 1;
      grouped.set(horizon, current);
    });
    return [...grouped.values()].sort((left, right) => left.horizon - right.horizon).map((item) => ({ ...item, label: `${item.horizon} days`, averageConfidenceWidth: item.forecasts ? item.confidenceWidth / item.forecasts : 0 }));
  }, [visible]);

  const categorySummary = useMemo(() => {
    const grouped = new Map();
    visible.forEach((forecast) => {
      const category = forecast.category || "Uncategorized";
      grouped.set(category, (grouped.get(category) ?? 0) + Number(forecast.predictedDemand ?? 0));
    });
    return [...grouped.entries()].map(([category, demand]) => ({ category, demand })).sort((left, right) => right.demand - left.demand).slice(0, 7);
  }, [visible]);

  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Forecasts" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} profile={profile} user={user} />
    <main className="dashboard forecasts-page">
      <header className="topbar"><h1>Forecasts</h1><label className="search-box"><Search size={15} /><span className="sr-only">Search forecasts</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Search store, product, category, or model..." value={query} /></label><UserAvatar onClick={() => onNavigate("Profile")} profile={profile} user={user} /></header>
      {error && <div className="notice error" role="status">{error}</div>}
      <section className="metrics-grid"><article className="metric-card"><ChartNoAxesCombined size={20} /><div><span>Active Forecasts</span><strong>{forecasts.length}</strong><small>Store-product predictions</small></div></article><article className="metric-card"><Target size={20} /><div><span>Average Model MAE</span><strong>{averageMae.toFixed(1)}</strong><small>Across all forecasts</small></div></article></section>
      <section className="forecast-visuals" aria-label="Forecast charts">
        <article className="forecast-chart-panel">
          <header><div><h2>Demand by planning horizon</h2><p>Total predicted units in the current view</p></div><span>{formatNumber(visible.length)} forecasts</span></header>
          <div className="forecast-chart-canvas"><ResponsiveContainer height="100%" width="100%"><BarChart data={horizonSummary} margin={{ top: 10, right: 12, left: 0, bottom: 0 }}><CartesianGrid stroke="#e5e7eb" strokeDasharray="3 3" vertical={false} /><XAxis axisLine={false} dataKey="label" tick={{ fill: "#6b7280", fontSize: 11 }} tickLine={false} /><YAxis axisLine={false} tick={{ fill: "#6b7280", fontSize: 11 }} tickFormatter={formatCompact} tickLine={false} width={46} /><Tooltip contentStyle={CHART_TOOLTIP_STYLE} cursor={{ fill: "#f3f4f6" }} formatter={(value) => [formatNumber(value), "Predicted demand"]} /><Bar dataKey="demand" fill="#111111" maxBarSize={54} radius={[3, 3, 0, 0]} /></BarChart></ResponsiveContainer></div>
          <div className="forecast-chart-notes">{horizonSummary.map((item) => <span key={item.horizon}><strong>{item.label}</strong>±{formatNumber(item.averageConfidenceWidth / 2)} units</span>)}</div>
        </article>
        <article className="forecast-chart-panel">
          <header><div><h2>Demand by category</h2><p>Highest-volume product groups</p></div><span>Top {categorySummary.length}</span></header>
          <div className="forecast-chart-canvas"><ResponsiveContainer height="100%" width="100%"><BarChart data={categorySummary} layout="vertical" margin={{ top: 4, right: 18, left: 12, bottom: 0 }}><CartesianGrid horizontal={false} stroke="#e5e7eb" strokeDasharray="3 3" /><XAxis axisLine={false} tick={{ fill: "#6b7280", fontSize: 11 }} tickFormatter={formatCompact} tickLine={false} type="number" /><YAxis axisLine={false} dataKey="category" tick={{ fill: "#6b7280", fontSize: 11 }} tickLine={false} type="category" width={96} /><Tooltip contentStyle={CHART_TOOLTIP_STYLE} cursor={{ fill: "#f3f4f6" }} formatter={(value) => [formatNumber(value), "Predicted demand"]} /><Bar dataKey="demand" fill="#111111" maxBarSize={22} radius={[0, 3, 3, 0]} /></BarChart></ResponsiveContainer></div>
        </article>
      </section>
      <section className="table-panel forecasts-table-panel"><div className="panel-toolbar"><h2>Demand forecasts</h2></div><div className="table-scroll"><table><thead><tr><th>Store</th><th>Product</th><th>Category</th><th>Date</th><th>Horizon</th><th>Predicted demand</th><th>Confidence range</th><th>Model</th></tr></thead><tbody>{visible.map((item) => <tr key={item.id}><td>{item.storeCode}</td><td>{item.productCode}</td><td>{item.category}</td><td>{item.forecastDate}</td><td>{item.horizonDays} days</td><td>{formatNumber(item.predictedDemand)}</td><td>{formatNumber(item.confidenceLower)} – {formatNumber(item.confidenceUpper)}</td><td>{String(item.modelName || "—").replaceAll("_", " ")}</td></tr>)}</tbody></table></div>{loading && <div className="panel-empty">Loading forecasts...</div>}{!loading && !visible.length && <div className="panel-empty">No forecasts match your search.</div>}</section>
    </main>
  </div>;
}

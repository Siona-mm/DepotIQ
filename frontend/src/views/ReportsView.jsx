import {
  AlertTriangle,
  BarChart3,
  Download,
  PackageCheck,
  Search,
  TrendingUp,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { loadDashboardData } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

const EMPTY_DATA = { forecasts: [], recommendations: [] };

function formatNumber(value) {
  return new Intl.NumberFormat("en-US", {
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));
}

function ReportMetric({ icon: Icon, label, value, note }) {
  return (
    <article className="metric-card">
      <Icon aria-hidden="true" size={20} strokeWidth={1.6} />
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        <small>{note}</small>
      </div>
    </article>
  );
}

function exportForecastCsv(forecasts) {
  const rows = [
    [
      "Store",
      "Product",
      "Category",
      "Forecast date",
      "Horizon days",
      "Predicted demand",
      "Confidence lower",
      "Confidence upper",
      "Model",
    ],
    ...forecasts.map((forecast) => [
      forecast.storeCode,
      forecast.productCode,
      forecast.category,
      forecast.forecastDate,
      forecast.horizonDays,
      forecast.predictedDemand,
      forecast.confidenceLower,
      forecast.confidenceUpper,
      forecast.modelName,
    ]),
  ];
  const csv = rows
    .map((row) => row.map((cell) => `"${String(cell ?? "").replaceAll('"', '""')}"`).join(","))
    .join("\n");
  const blob = new globalThis.Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = globalThis.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "depotiq-forecast-report.csv";
  link.click();
  globalThis.URL.revokeObjectURL(url);
}

export default function ReportsView({
  collapsed,
  onCollapse,
  onAction,
  onNavigate,
  onSignOut,
  user,
}) {
  const [data, setData] = useState(EMPTY_DATA);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [horizon, setHorizon] = useState("ALL");
  const [store, setStore] = useState("ALL");

  const load = useCallback(async () => {
    setError("");
    try {
      const result = await loadDashboardData();
      setData(result);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const storeOptions = useMemo(
    () =>
      Array.from(
        new Map(
          data.forecasts.map((forecast) => [
            forecast.storeCode,
            `${forecast.storeCode} - ${forecast.storeName}`,
          ]),
        ),
      ),
    [data.forecasts],
  );

  const filteredForecasts = useMemo(
    () =>
      data.forecasts.filter(
        (forecast) =>
          (horizon === "ALL" || String(forecast.horizonDays) === horizon) &&
          (store === "ALL" || forecast.storeCode === store),
      ),
    [data.forecasts, horizon, store],
  );

  const categorySummary = useMemo(() => {
    const grouped = new Map();
    filteredForecasts.forEach((forecast) => {
      const current = grouped.get(forecast.category) ?? {
        category: forecast.category,
        forecasts: 0,
        demand: 0,
        confidenceWidth: 0,
      };
      current.forecasts += 1;
      current.demand += Number(forecast.predictedDemand ?? 0);
      current.confidenceWidth +=
        Number(forecast.confidenceUpper ?? 0) -
        Number(forecast.confidenceLower ?? 0);
      grouped.set(forecast.category, current);
    });
    return [...grouped.values()]
      .map((item) => ({
        ...item,
        averageConfidenceWidth: item.confidenceWidth / item.forecasts,
      }))
      .sort((left, right) => right.demand - left.demand);
  }, [filteredForecasts]);

  const summary = useMemo(() => {
    const totalDemand = filteredForecasts.reduce(
      (total, forecast) => total + Number(forecast.predictedDemand ?? 0),
      0,
    );
    const averageUncertainty =
      filteredForecasts.length === 0
        ? 0
        : filteredForecasts.reduce(
            (total, forecast) =>
              total +
              (Number(forecast.confidenceUpper ?? 0) -
                Number(forecast.confidenceLower ?? 0)),
            0,
          ) / filteredForecasts.length;
    const urgent = data.recommendations.filter(
      (item) => item.priority === "URGENT" && item.status !== "DELIVERED",
    ).length;
    const actionable = data.recommendations.filter(
      (item) =>
        (item.status === "PENDING" || item.status === "EDITED") &&
        Number(item.recommendedShipment) > 0,
    ).length;

    return { totalDemand, averageUncertainty, urgent, actionable };
  }, [data.recommendations, filteredForecasts]);

  const risks = useMemo(
    () =>
      data.recommendations
        .filter(
          (item) =>
            item.priority === "URGENT" &&
            item.status !== "DELIVERED" &&
            Number(item.recommendedShipment) > 0,
        )
        .sort(
          (left, right) =>
            Number(right.recommendedShipment) - Number(left.recommendedShipment),
        )
        .slice(0, 8),
    [data.recommendations],
  );

  const maxCategoryDemand = Math.max(
    ...categorySummary.map((item) => item.demand),
    1,
  );

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Reports"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onSignOut={onSignOut}
        user={user}
      />

      <main className="dashboard reports-page">
        <header className="topbar">
          <h1>Reports</h1>
          <label className="search-box reports-search">
            <Search aria-hidden="true" size={15} strokeWidth={2} />
            <span>Forecast and inventory reporting</span>
          </label>
          <div className="avatar" aria-label="Signed in as SM">SM</div>
        </header>

        <div className="page-heading">
          <div>
            <span>Planning intelligence</span>
            <h2>Demand and stock risk overview</h2>
          </div>
          <button
            className="primary-button"
            disabled={filteredForecasts.length === 0}
            onClick={() => exportForecastCsv(filteredForecasts)}
            type="button"
          >
            <Download aria-hidden="true" size={16} />
            Export CSV
          </button>
        </div>

        {error && <div className="notice error" role="status">{error}</div>}

        <section className="metrics-grid reports-metrics" aria-label="Report summary">
          <ReportMetric icon={BarChart3} label="Forecast Coverage" note="Current filtered forecasts" value={formatNumber(filteredForecasts.length)} />
          <ReportMetric icon={TrendingUp} label="Planned Demand" note="Across selected planning window" value={formatNumber(summary.totalDemand)} />
          <ReportMetric icon={AlertTriangle} label="Urgent Risks" note="Store-product pairs needing stock" value={formatNumber(summary.urgent)} />
          <ReportMetric icon={PackageCheck} label="Open Actions" note="Pending recommendations" value={formatNumber(summary.actionable)} />
        </section>

        <section className="report-controls" aria-label="Report filters">
          <label>
            <span>Planning horizon</span>
            <select onChange={(event) => setHorizon(event.target.value)} value={horizon}>
              <option value="ALL">All horizons</option>
              {[3, 7, 14, 30].map((days) => <option key={days} value={days}>{days}-day plan</option>)}
            </select>
          </label>
          <label>
            <span>Store</span>
            <select onChange={(event) => setStore(event.target.value)} value={store}>
              <option value="ALL">All stores</option>
              {storeOptions.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <div className="report-confidence">
            <span>Average confidence range</span>
            <strong>+/- {formatNumber(summary.averageUncertainty / 2)} units</strong>
          </div>
        </section>

        <section className="report-grid">
          <section className="table-panel report-category-panel">
            <div className="panel-toolbar"><div><h2>Demand by category</h2><span className="panel-subtitle">Forecast volume within the selected view</span></div></div>
            <div className="category-bars">
              {categorySummary.map((item) => (
                <div className="category-bar-row" key={item.category}>
                  <div><strong>{item.category}</strong><span>{formatNumber(item.forecasts)} forecasts</span></div>
                  <div className="category-track"><span style={{ width: `${(item.demand / maxCategoryDemand) * 100}%` }} /></div>
                  <b>{formatNumber(item.demand)}</b>
                </div>
              ))}
              {!loading && categorySummary.length === 0 && <div className="panel-empty">No forecasts match this report.</div>}
            </div>
          </section>

          <section className="table-panel report-risk-panel">
            <div className="panel-toolbar"><div><h2>Priority stock risks</h2><span className="panel-subtitle">Highest urgent shipment requirements</span></div></div>
            <div className="table-scroll">
              <table>
                <thead><tr><th>Store</th><th>Product</th><th>Stock</th><th>Shipment</th></tr></thead>
                <tbody>{risks.map((item) => <tr key={item.id}><td>{item.storeCode}</td><td>{item.productCode}</td><td>{formatNumber(item.currentInventory)}</td><td><strong>{formatNumber(item.recommendedShipment)}</strong></td></tr>)}</tbody>
              </table>
            </div>
            {!loading && risks.length === 0 && <div className="panel-empty">No urgent risks right now.</div>}
          </section>
        </section>
      </main>
    </div>
  );
}

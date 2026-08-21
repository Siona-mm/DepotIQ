import { CircleAlert, Database, RefreshCw, Sparkles } from "lucide-react";

function formatDate(value, includeTime = false) {
  if (!value) {
    return "No synced data";
  }

  return new Intl.DateTimeFormat("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
    ...(includeTime ? { hour: "numeric", minute: "2-digit" } : {}),
  }).format(new Date(value));
}

function formatMae(value) {
  return value === null || value === undefined ? "Not available" : Number(value).toFixed(2);
}

export default function MlStatusPanel({ loading, onRetry, status }) {
  const model = status?.models?.find((item) => item.artifactAvailable) ?? status?.models?.[0];
  const unavailable = !status || !status.serviceAvailable;

  return (
    <section className="ml-status-panel" aria-label="ML model status">
      <div className="ml-status-heading">
        <div>
          <span className="eyebrow">Model monitoring</span>
          <h2>Forecast service</h2>
        </div>
        {loading ? (
          <span className="ml-status-badge checking">Checking</span>
        ) : (
          <span className={unavailable ? "ml-status-badge unavailable" : "ml-status-badge available"}>
            {unavailable ? "Unavailable" : "Connected"}
          </span>
        )}
      </div>

      {loading ? (
        <p className="ml-status-message">Checking the forecast service and synced model data…</p>
      ) : unavailable ? (
        <div className="ml-status-unavailable">
          <CircleAlert aria-hidden="true" size={18} />
          <p>The forecast service is not reachable. Existing synced forecasts remain available.</p>
          <button className="secondary-button ml-status-retry" onClick={onRetry} type="button">
            <RefreshCw aria-hidden="true" size={14} />
            Retry status
          </button>
        </div>
      ) : (
        <>
          <div className="ml-status-model">
            <Sparkles aria-hidden="true" size={18} />
            <div>
              <strong>{model ? model.modelName.replaceAll("_", " ") : "No model reported"}</strong>
              <span>{model ? `Version ${model.modelVersion} · ${model.horizonDays}-day horizon` : "Sync model output to populate metadata"}</span>
            </div>
          </div>
          <dl className="ml-status-metrics">
            <div><dt>Forecasts</dt><dd>{status.forecastCount}</dd></div>
            <div><dt>Recommendations</dt><dd>{status.recommendationCount}</dd></div>
            <div><dt>Coverage</dt><dd>{status.coveredStores} stores · {status.coveredProducts} products</dd></div>
            <div><dt>Average MAE</dt><dd>{formatMae(status.averageMae)}</dd></div>
          </dl>
          <footer className="ml-status-footer">
            <Database aria-hidden="true" size={14} />
            <span>Latest forecast: <strong>{formatDate(status.latestForecastDate)}</strong></span>
            <span>Last synced: <strong>{formatDate(status.lastSynchronizedAt, true)}</strong></span>
          </footer>
        </>
      )}
    </section>
  );
}

import { BarChart3, Boxes, ShieldCheck, Truck, X } from "lucide-react";

const number = (value) => new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(Number(value ?? 0));

function InsightMetric({ icon: Icon, label, value }) {
  return <div className="insight-metric"><Icon aria-hidden="true" size={16} /><span>{label}</span><strong>{value}</strong></div>;
}

export default function RecommendationInsightsDialog({ depotAvailableUnits, onClose, recommendation }) {
  const confidence = `${number(recommendation.confidenceLower)} – ${number(recommendation.confidenceUpper)} units`;
  const explanation = recommendation.explanation ||
    `Demand, current store stock, incoming units, and the safety-stock buffer are combined to recommend this shipment.`;

  return <div className="modal-backdrop" onClick={(event) => event.target === event.currentTarget && onClose()}>
    <section aria-labelledby="recommendation-insights-title" aria-modal="true" className="insights-dialog" role="dialog">
      <header>
        <div><span>Recommendation details</span><h2 id="recommendation-insights-title">{recommendation.storeCode} / {recommendation.productCode}</h2></div>
        <button aria-label="Close recommendation details" className="icon-button" onClick={onClose} type="button"><X size={16} /></button>
      </header>
      <p className="insights-product">{recommendation.storeName} · {recommendation.productName}</p>
      <section className="insight-summary">
        <div><span>Recommended shipment</span><strong>{number(recommendation.recommendedShipment)} units</strong></div>
        <span className={`priority ${recommendation.priority.toLowerCase()}`}>{recommendation.priority}</span>
      </section>
      <section className="insight-grid" aria-label="Recommendation inputs">
        <InsightMetric icon={BarChart3} label="Predicted demand" value={`${number(recommendation.predictedDemand)} units`} />
        <InsightMetric icon={Boxes} label="Store stock" value={`${number(recommendation.currentInventory)} units`} />
        <InsightMetric icon={Truck} label="Incoming stock" value={`${number(recommendation.incomingUnits)} units`} />
        <InsightMetric icon={ShieldCheck} label="Safety stock" value={`${number(recommendation.safetyStock)} units`} />
        <InsightMetric icon={Boxes} label="Required stock" value={`${number(recommendation.requiredStock)} units`} />
        <InsightMetric icon={Truck} label="Depot available" value={`${number(depotAvailableUnits)} units`} />
      </section>
      <section className="insight-explanation"><h3>Why this is recommended</h3><p>{explanation}</p><div><span>Forecast confidence range</span><strong>{confidence}</strong></div></section>
      <footer><button className="primary-button" onClick={onClose} type="button">Close details</button></footer>
    </section>
  </div>;
}

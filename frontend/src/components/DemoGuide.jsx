import { CheckCircle2, X } from "lucide-react";

const STEPS = [
  ["Identify risk", "Open the Dashboard to see forecast-driven stockout risks and suggested shipment quantities."],
  ["Review evidence", "Compare predicted demand, store stock, depot availability, and the recommendation priority."],
  ["Plan delivery", "Approve a recommendation, then create and track a shipment from the Shipments page."],
  ["Measure impact", "Use Forecasts and Reports to review model coverage, accuracy, and operational risk."],
];

export default function DemoGuide({ onClose, permissions }) {
  return (
    <div className="modal-backdrop" onClick={(event) => event.target === event.currentTarget && onClose()}>
      <section aria-labelledby="demo-guide-title" aria-modal="true" className="demo-guide" role="dialog">
        <header>
          <div>
            <span>Guided walkthrough</span>
            <h2 id="demo-guide-title">DepotIQ in four steps</h2>
          </div>
          <button aria-label="Close demo guide" className="icon-button" onClick={onClose} type="button"><X size={16} /></button>
        </header>
        <p>Use this flow to show how data becomes a practical replenishment decision.</p>
        <ol>
          {STEPS.map(([title, description], index) => (
            <li key={title}>
              <span>{index + 1}</span>
              <div><strong>{title}</strong><p>{description}</p></div>
            </li>
          ))}
        </ol>
        {permissions.canImportData && <div className="demo-guide-note"><CheckCircle2 size={16} /><span>As an admin, you can also import historical sales data and manage the store and product catalog.</span></div>}
        <footer><button className="primary-button" onClick={onClose} type="button">Start exploring</button></footer>
      </section>
    </div>
  );
}

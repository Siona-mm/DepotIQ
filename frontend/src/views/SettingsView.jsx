import {
  Bell,
  Database,
  RefreshCw,
  Save,
  Search,
  ShieldCheck,
  SlidersHorizontal,
} from "lucide-react";
import { useEffect, useState } from "react";
import AppSidebar from "../components/AppSidebar.jsx";

const SETTINGS_KEY = "depotiq-operations-settings";

const DEFAULTS = {
  defaultHorizon: "7",
  safetyStockDays: "3",
  alertThreshold: "250",
  autoRefresh: true,
  requireApproval: true,
  allowOverrides: true,
  emailAlerts: false,
};

function readSettings() {
  try {
    const saved = globalThis.localStorage.getItem(SETTINGS_KEY);
    return saved ? { ...DEFAULTS, ...JSON.parse(saved) } : DEFAULTS;
  } catch {
    return DEFAULTS;
  }
}

function Toggle({ checked, description, label, onChange }) {
  return (
    <label className="settings-toggle">
      <span>
        <strong>{label}</strong>
        <small>{description}</small>
      </span>
      <input checked={checked} onChange={(event) => onChange(event.target.checked)} type="checkbox" />
      <i aria-hidden="true" />
    </label>
  );
}

export default function SettingsView({
  collapsed,
  onCollapse,
  onAction,
  onNavigate,
  onSignOut,
  permissions,
  user,
}) {
  const [settings, setSettings] = useState(readSettings);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const clearMessage = globalThis.setTimeout(() => setMessage(""), 3500);
    return () => globalThis.clearTimeout(clearMessage);
  }, [message]);

  const update = (key, value) => {
    setSettings((current) => ({ ...current, [key]: value }));
    setMessage("");
  };

  const save = () => {
    globalThis.localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    setMessage("Settings saved for this browser.");
  };

  const reset = () => {
    globalThis.localStorage.removeItem(SETTINGS_KEY);
    setSettings(DEFAULTS);
    setMessage("Default settings restored.");
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Settings"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onSignOut={onSignOut}
        permissions={permissions}
        user={user}
      />

      <main className="dashboard settings-page">
        <header className="topbar">
          <h1>Settings</h1>
          <label className="search-box reports-search">
            <Search aria-hidden="true" size={15} strokeWidth={2} />
            <span>Depot operating preferences</span>
          </label>
          <div className="avatar" aria-label="Signed in as SM">SM</div>
        </header>

        <div className="page-heading">
          <div>
            <span>Operations workspace</span>
            <h2>Planning preferences and workflow controls</h2>
          </div>
          <div className="settings-page-actions">
            <button className="secondary-button" onClick={reset} type="button">Restore defaults</button>
            <button className="primary-button" onClick={save} type="button"><Save aria-hidden="true" size={16} />Save settings</button>
          </div>
        </div>

        {message && <div className="notice" role="status">{message}</div>}

        <section className="settings-layout">
          <section className="settings-section">
            <header><SlidersHorizontal aria-hidden="true" size={18} /><div><h2>Planning defaults</h2><p>Used as the starting point when reviewing demand and stock recommendations.</p></div></header>
            <div className="settings-fields">
              <label><span>Default planning horizon</span><select onChange={(event) => update("defaultHorizon", event.target.value)} value={settings.defaultHorizon}><option value="3">3 days</option><option value="7">7 days</option><option value="14">14 days</option><option value="30">30 days</option></select></label>
              <label><span>Safety stock coverage</span><select onChange={(event) => update("safetyStockDays", event.target.value)} value={settings.safetyStockDays}><option value="1">1 day</option><option value="3">3 days</option><option value="5">5 days</option><option value="7">7 days</option></select></label>
              <label><span>Low-stock alert threshold</span><div className="number-input"><input min="0" onChange={(event) => update("alertThreshold", event.target.value)} type="number" value={settings.alertThreshold} /><span>units</span></div></label>
            </div>
          </section>

          <section className="settings-section">
            <header><ShieldCheck aria-hidden="true" size={18} /><div><h2>Approval workflow</h2><p>Control how recommendations move from the model into transport planning.</p></div></header>
            <div className="settings-toggles">
              <Toggle checked={settings.requireApproval} description="Keep recommendations pending until an admin approves them." label="Require approval before planning" onChange={(value) => update("requireApproval", value)} />
              <Toggle checked={settings.allowOverrides} description="Allow admins to edit a shipment quantity with a saved reason." label="Allow recommendation overrides" onChange={(value) => update("allowOverrides", value)} />
            </div>
          </section>

          <section className="settings-section">
            <header><Bell aria-hidden="true" size={18} /><div><h2>Alerts and refresh</h2><p>Choose what the operations team sees while monitoring the depot.</p></div></header>
            <div className="settings-toggles">
              <Toggle checked={settings.autoRefresh} description="Refresh dashboard data when the page is opened." label="Refresh live dashboard data" onChange={(value) => update("autoRefresh", value)} />
              <Toggle checked={settings.emailAlerts} description="Reserved for a future email or Slack notification integration." label="Send low-stock notifications" onChange={(value) => update("emailAlerts", value)} />
            </div>
          </section>

          <aside className="settings-status-card">
            <div><Database aria-hidden="true" size={18} /><span>System status</span></div>
            <strong>Connected</strong>
            <p>PostgreSQL, the forecasting service, and the operational dashboard are available in this local environment.</p>
            <span className="settings-status-note"><RefreshCw aria-hidden="true" size={13} />Changes are saved only in this browser until a shared settings API is added.</span>
          </aside>
        </section>
      </main>
    </div>
  );
}

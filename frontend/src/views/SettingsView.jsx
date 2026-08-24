import {
  Bell,
  Save,
  Search,
  ShieldCheck,
  SlidersHorizontal,
} from "lucide-react";
import { useEffect, useState } from "react";
import { loadSettings, updateSettings } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";

const DEFAULTS = {
  defaultHorizon: "7",
  safetyStockDays: "3",
  alertThreshold: "250",
  autoRefresh: true,
  requireApproval: true,
  allowOverrides: true,
  emailAlerts: false,
};

function Toggle({ checked, description, disabled, label, onChange }) {
  return (
    <label className="settings-toggle">
      <span>
        <strong>{label}</strong>
        <small>{description}</small>
      </span>
      <input checked={checked} disabled={disabled} onChange={(event) => onChange(event.target.checked)} type="checkbox" />
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
  profile,
  user,
}) {
  const [settings, setSettings] = useState(DEFAULTS);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const canManageSettings = permissions.canManageSettings;

  useEffect(() => {
    loadSettings()
      .then((saved) => setSettings(saved))
      .catch((requestError) => setError(requestError.message));
  }, []);

  useEffect(() => {
    const clearMessage = globalThis.setTimeout(() => setMessage(""), 3500);
    return () => globalThis.clearTimeout(clearMessage);
  }, [message]);

  const update = (key, value) => {
    if (!canManageSettings) return;
    setSettings((current) => ({ ...current, [key]: value }));
    setMessage("");
  };

  const save = async () => {
    if (!canManageSettings) return;
    setSaving(true);
    setError("");
    try {
      const saved = await updateSettings({
        ...settings,
        defaultHorizon: Number(settings.defaultHorizon),
        safetyStockDays: Number(settings.safetyStockDays),
        alertThreshold: Number(settings.alertThreshold),
      });
      setSettings(saved);
      setMessage("Settings saved to your DepotIQ account.");
      globalThis.dispatchEvent(new globalThis.Event("depotiq-settings-updated"));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
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
        profile={profile}
        user={user}
      />

      <main className="dashboard settings-page">
        <header className="topbar">
          <h1>Settings</h1>
          <label className="search-box reports-search">
            <Search aria-hidden="true" size={15} strokeWidth={2} />
            <span>Depot operating preferences</span>
          </label>
          <HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} />
        </header>

        <div className="page-heading">
          <div>
            <span>Operations workspace</span>
            <h2>Planning preferences and workflow controls</h2>
          </div>
        </div>

        {(message || error) && <div className={error ? "notice error" : "notice"} role="status">{error || message}</div>}
        {!canManageSettings && <div className="notice" role="status">Your role has read-only access to settings.</div>}

        <section className="settings-layout">
          <section className="settings-section">
            <header><SlidersHorizontal aria-hidden="true" size={18} /><div><h2>Planning defaults</h2><p>Used as the starting point when reviewing demand and stock recommendations.</p></div></header>
            <div className="settings-fields">
              <label><span>Default planning horizon</span><select disabled={!canManageSettings} onChange={(event) => update("defaultHorizon", event.target.value)} value={settings.defaultHorizon}><option value="3">3 days</option><option value="7">7 days</option><option value="14">14 days</option><option value="30">30 days</option></select></label>
              <label><span>Safety stock coverage</span><select disabled={!canManageSettings} onChange={(event) => update("safetyStockDays", event.target.value)} value={settings.safetyStockDays}><option value="1">1 day</option><option value="3">3 days</option><option value="5">5 days</option><option value="7">7 days</option></select></label>
              <label><span>Low-stock alert threshold</span><div className="number-input"><input disabled={!canManageSettings} min="0" onChange={(event) => update("alertThreshold", event.target.value)} type="number" value={settings.alertThreshold} /><span>units</span></div></label>
            </div>
          </section>

          <section className="settings-section">
            <header><ShieldCheck aria-hidden="true" size={18} /><div><h2>Approval workflow</h2><p>Control how recommendations move from the model into transport planning.</p></div></header>
            <div className="settings-toggles">
              <Toggle checked={settings.requireApproval} description="Keep recommendations pending until an admin approves them." disabled={!canManageSettings} label="Require approval before planning" onChange={(value) => update("requireApproval", value)} />
              <Toggle checked={settings.allowOverrides} description="Allow admins to edit a shipment quantity with a saved reason." disabled={!canManageSettings} label="Allow recommendation overrides" onChange={(value) => update("allowOverrides", value)} />
            </div>
          </section>

          <section className="settings-section">
            <header><Bell aria-hidden="true" size={18} /><div><h2>Alerts and refresh</h2><p>Choose what the operations team sees while monitoring the depot.</p></div></header>
            <div className="settings-toggles">
              <Toggle checked={settings.autoRefresh} description="Refresh dashboard data when the page is opened." disabled={!canManageSettings} label="Refresh live dashboard data" onChange={(value) => update("autoRefresh", value)} />
              <Toggle checked={settings.emailAlerts} description="Show a notification when store stock reaches your configured threshold." disabled={!canManageSettings} label="Show low-stock alerts in the app" onChange={(value) => update("emailAlerts", value)} />
            </div>
          </section>

        </section>
        {canManageSettings && <footer className="settings-save-footer"><div><strong>Save operating preferences</strong><span>Changes apply to your DepotIQ account.</span></div><button className="primary-button" disabled={saving} onClick={save} type="button"><Save aria-hidden="true" size={16} />{saving ? "Saving..." : "Save settings"}</button></footer>}
      </main>
    </div>
  );
}

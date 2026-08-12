import { Bell, Database, Save, SlidersHorizontal } from "lucide-react";
import { useState } from "react";
import AppSidebar from "../components/AppSidebar.jsx";

const STORAGE_KEY = "depotiq-settings";
const defaults = { horizonDays: "7", alertsEnabled: true, compactTables: false };

function loadSettings() {
  try { return { ...defaults, ...JSON.parse(globalThis.localStorage.getItem(STORAGE_KEY)) }; }
  catch { return defaults; }
}

export default function SettingsView({ collapsed, onAction, onCollapse, onNavigate }) {
  const [settings, setSettings] = useState(loadSettings);
  const [saved, setSaved] = useState(false);
  const update = (key, value) => { setSaved(false); setSettings((current) => ({ ...current, [key]: value })); };
  const save = (event) => { event.preventDefault(); globalThis.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings)); setSaved(true); };

  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Settings" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} />
    <main className="dashboard settings-page"><header className="topbar"><h1>Settings</h1></header>
      {saved && <div className="notice" role="status">Settings saved in this browser.</div>}
      <form className="settings-grid" onSubmit={save}>
        <section className="table-panel settings-card"><header><SlidersHorizontal size={18} /><div><h2>Forecast preferences</h2><p>Set the default forecast horizon used across the dashboard.</p></div></header><label>Preferred forecast horizon<select onChange={(event) => update("horizonDays", event.target.value)} value={settings.horizonDays}>{[3, 7, 14, 30].map((days) => <option key={days} value={days}>{days} days</option>)}</select></label></section>
        <section className="table-panel settings-card"><header><Bell size={18} /><div><h2>Notifications</h2><p>Control local dashboard alerts.</p></div></header><label className="toggle-field"><input checked={settings.alertsEnabled} onChange={(event) => update("alertsEnabled", event.target.checked)} type="checkbox" /><span>Show low-stock and shipment alerts</span></label></section>
        <section className="table-panel settings-card"><header><Database size={18} /><div><h2>Display</h2><p>Choose how data tables are displayed in this browser.</p></div></header><label className="toggle-field"><input checked={settings.compactTables} onChange={(event) => update("compactTables", event.target.checked)} type="checkbox" /><span>Use compact table rows</span></label></section>
        <footer><button className="save-button" type="submit"><Save size={15} />Save settings</button></footer>
      </form>
    </main>
  </div>;
}

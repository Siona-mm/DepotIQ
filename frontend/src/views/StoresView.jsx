import { Building2, MapPin, PencilLine, Plus, Search, Store, Trash2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { createStore, deleteStore, loadStores, updateStore } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";
import { compareBusinessCodes, storeDetailIssues, storeNeedsDetails } from "../utils/businessCodes.js";
import { buildStorePayload, catalogFormValues, EMPTY_STORE } from "../utils/catalogForms.js";

function formatNumber(value) {
  return new Intl.NumberFormat("en-US").format(Number(value ?? 0));
}

function formatStoreType(value) {
  return String(value || "").toLowerCase().replaceAll("_", " ").replace(/^./, (letter) => letter.toUpperCase());
}

export default function StoresView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user }) {
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_STORE);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [editItem, setEditItem] = useState(null);

  const load = useCallback(async () => {
    setError("");
    try {
      setStores(await loadStores());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const visibleStores = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return stores.filter((store) => !normalizedQuery ||
      [store.storeCode, store.externalStoreId, store.name, store.storeType, store.region].some((value) =>
        String(value ?? "").toLowerCase().includes(normalizedQuery),
      ),
    ).sort((left, right) => compareBusinessCodes(left.storeCode, right.storeCode));
  }, [query, stores]);
  const incompleteCount = stores.filter(storeNeedsDetails).length;

  const updateForm = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const openCreate = () => { setForm(EMPTY_STORE); setEditItem(null); setError(""); setCreateOpen(true); };
  const openEdit = (store) => { setForm(catalogFormValues(EMPTY_STORE, store)); setEditItem(store); setError(""); setCreateOpen(true); };
  const saveStore = async (event) => {
    event.preventDefault();
    setSaving(true); setError("");
    try {
      const payload = buildStorePayload(form);
      const saved = editItem
        ? await updateStore(editItem.id, payload)
        : await createStore(payload);
      setStores((current) => editItem
        ? current.map((store) => store.id === saved.id ? saved : store)
        : [...current, saved]);
      setCreateOpen(false);
      setMessage(`${saved.storeCode} was ${editItem ? "updated" : "created"}.`);
    } catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  };
  const removeStore = async (store) => {
    if (!globalThis.confirm(`Delete ${store.storeCode}?`)) return;
    setError("");
    try { await deleteStore(store.id); setStores((current) => current.filter((item) => item.id !== store.id)); setMessage(`${store.storeCode} was deleted.`); }
    catch (requestError) { setError(requestError.message); }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Stores"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onSignOut={onSignOut}
        permissions={permissions}
        profile={profile}
        user={user}
      />

      <main className="dashboard stores-page">
        <header className="topbar">
          <h1>Stores</h1>
          <label className="search-box">
            <Search aria-hidden="true" size={15} />
            <span className="sr-only">Search stores</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search store, code, type, or region..."
              type="search"
              value={query}
            />
          </label>
          <HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} />
        </header>

        {!createOpen && (error || message) && <div className={error ? "notice error" : "notice"} role="status">{error || message}</div>}

        {incompleteCount > 0 && <div className="notice" role="status">
          {incompleteCount} store{incompleteCount === 1 ? " needs" : "s need"} operating details corrected. Each marked row lists the fields to check; use Edit to correct them.
        </div>}
        <section className="metrics-grid" aria-label="Store summary">
          <article className="metric-card"><Store size={20} /><div><span>Total Stores</span><strong>{stores.length}</strong><small>Active locations</small></div></article>
          <article className="metric-card"><MapPin size={20} /><div><span>Regions</span><strong>{new Set(stores.map((store) => store.region)).size}</strong><small>Coverage areas</small></div></article>
          <article className="metric-card"><Building2 size={20} /><div><span>Warehouse Stores</span><strong>{stores.filter((store) => store.hasWarehouse).length}</strong><small>With local storage</small></div></article>
        </section>

        <section className="table-panel stores-table-panel">
          <div className="panel-toolbar"><h2>Store directory</h2><div className="table-actions"><button className="tool-button" onClick={openCreate} type="button"><Plus size={14} />Add store</button></div></div>
          <div className="table-scroll">
            <table>
              <thead><tr><th>Code</th><th>Store</th><th>Type</th><th>Region</th><th>Warehouse</th><th>Capacity</th><th>Lead time</th><th>Horizon</th><th>Actions</th></tr></thead>
              <tbody>
                {visibleStores.map((store) => (
                  <tr key={store.id}>
                    <td>{store.storeCode}</td><td className="store-directory-name" title={store.name}>
                      <strong>{store.name}</strong>
                      {store.externalStoreId && <small title={store.externalStoreId}>External ID: {store.externalStoreId}</small>}
                      {storeNeedsDetails(store) && <span className="store-details-warning">Check: {storeDetailIssues(store).join(", ")}</span>}
                    </td><td>{formatStoreType(store.storeType)}</td><td>{store.region || "Not set"}</td><td>{store.hasWarehouse == null ? "Not set" : store.hasWarehouse ? "Yes" : "No"}</td>
                    <td>{store.storageCapacity > 0 ? formatNumber(store.storageCapacity) : "Not set"}</td><td>{store.deliveryLeadTimeDays > 0 ? `${store.deliveryLeadTimeDays} days` : "Not set"}</td><td>{[3, 7, 14, 30].includes(store.preferredHorizonDays) ? `${store.preferredHorizonDays} days` : "Not set"}</td><td><div className="action-buttons"><button aria-label={`Edit ${store.storeCode}`} className="icon-button" onClick={() => openEdit(store)} type="button"><PencilLine size={14} /></button><button aria-label={`Delete ${store.storeCode}`} className="reject-button" onClick={() => removeStore(store)} type="button"><Trash2 size={14} /></button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && visibleStores.length === 0 && <div className="panel-empty">No stores match your search.</div>}
          {loading && <div className="panel-empty">Loading stores...</div>}
        </section>
      </main>

      {createOpen && <div className="modal-backdrop" onClick={(event) => { if (event.target === event.currentTarget && !saving) setCreateOpen(false); }}>
        <section aria-modal="true" className="override-dialog store-form-dialog" role="dialog">
          <header><div><span>Store directory</span><h2>{editItem ? "Edit store" : "Add store"}</h2></div><button aria-label="Close store dialog" className="icon-button" disabled={saving} onClick={() => setCreateOpen(false)} type="button"><X size={16} /></button></header>
          <form onSubmit={saveStore}>
            <p className="catalog-form-help">Operating details are required. External store ID is optional unless you link this store to a catalog CSV. DepotIQ generates the store code.</p>
            {error && <div className="notice error catalog-form-error" role="alert">{error}</div>}
            <label className="generated-code-field">Store code<input disabled value={editItem?.storeCode || "Generated automatically"} /><small>Assigned by DepotIQ when the store is created.</small></label>
            <label>External store ID (optional)<input maxLength={100} onChange={(event) => updateForm("externalStoreId", event.target.value)} value={form.externalStoreId} /><small>Matches this store to catalog CSV uploads. This is separate from the generated store code.</small></label>
            <label>Name<input maxLength={150} onChange={(event) => updateForm("name", event.target.value)} required value={form.name} /></label>
            <label>Store type<select required onChange={(event) => updateForm("storeType", event.target.value)} value={form.storeType}><option value="" disabled>Select store type</option>{["SMALL", "MEDIUM", "LARGE", "WAREHOUSE_STORE"].map((type) => <option key={type} value={type}>{formatStoreType(type)}</option>)}</select></label>
            <label>Region<input maxLength={100} onChange={(event) => updateForm("region", event.target.value)} required value={form.region} /></label>
            <label>Storage capacity<input min="1" max="2147483647" step="1" onChange={(event) => updateForm("storageCapacity", event.target.value)} required type="number" value={form.storageCapacity} /></label>
            <label>Delivery lead time (days)<input min="1" max="2147483647" step="1" onChange={(event) => updateForm("deliveryLeadTimeDays", event.target.value)} required type="number" value={form.deliveryLeadTimeDays} /></label>
            <label>Preferred horizon (days)<select onChange={(event) => updateForm("preferredHorizonDays", event.target.value)} required value={form.preferredHorizonDays}><option value="" disabled>Select horizon</option>{[3, 7, 14, 30].map((days) => <option key={days} value={days}>{days} days</option>)}</select></label>
            <label>Has warehouse<select required value={form.hasWarehouse} onChange={(event) => updateForm("hasWarehouse", event.target.value)}><option value="" disabled>Select Yes or No</option><option value="true">Yes</option><option value="false">No</option></select></label>
            <footer><button className="secondary-button" disabled={saving} onClick={() => setCreateOpen(false)} type="button">Cancel</button><button className="save-button" disabled={saving} type="submit">{saving ? "Saving..." : editItem ? "Save changes" : "Create store"}</button></footer>
          </form>
        </section>
      </div>}
    </div>
  );
}

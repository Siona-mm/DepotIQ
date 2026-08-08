import { Building2, MapPin, Search, Store } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { loadStores } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

function formatNumber(value) {
  return new Intl.NumberFormat("en-US").format(Number(value ?? 0));
}

export default function StoresView({ collapsed, onAction, onCollapse, onNavigate }) {
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");

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
    if (!normalizedQuery) {
      return stores;
    }

    return stores.filter((store) =>
      [store.storeCode, store.name, store.storeType, store.region].some((value) =>
        String(value ?? "").toLowerCase().includes(normalizedQuery),
      ),
    );
  }, [query, stores]);

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Stores"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
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
        </header>

        {error && <div className="notice error" role="status">{error}</div>}

        <section className="metrics-grid" aria-label="Store summary">
          <article className="metric-card"><Store size={20} /><div><span>Total Stores</span><strong>{stores.length}</strong><small>Active locations</small></div></article>
          <article className="metric-card"><MapPin size={20} /><div><span>Regions</span><strong>{new Set(stores.map((store) => store.region)).size}</strong><small>Coverage areas</small></div></article>
          <article className="metric-card"><Building2 size={20} /><div><span>Warehouse Stores</span><strong>{stores.filter((store) => store.hasWarehouse).length}</strong><small>With local storage</small></div></article>
        </section>

        <section className="table-panel stores-table-panel">
          <div className="panel-toolbar"><h2>Store directory</h2><span>{visibleStores.length} shown</span></div>
          <div className="table-scroll">
            <table>
              <thead><tr><th>Code</th><th>Store</th><th>Type</th><th>Region</th><th>Capacity</th><th>Lead time</th><th>Horizon</th></tr></thead>
              <tbody>
                {visibleStores.map((store) => (
                  <tr key={store.id}>
                    <td>{store.storeCode}</td><td>{store.name}</td><td>{store.storeType}</td><td>{store.region}</td>
                    <td>{formatNumber(store.storageCapacity)}</td><td>{store.deliveryLeadTimeDays} days</td><td>{store.preferredHorizonDays} days</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && visibleStores.length === 0 && <div className="panel-empty">No stores match your search.</div>}
          {loading && <div className="panel-empty">Loading stores...</div>}
        </section>
      </main>
    </div>
  );
}

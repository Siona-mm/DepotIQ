import {
  Boxes,
  CheckCircle2,
  CircleAlert,
  PackageCheck,
  PencilLine,
  Search,
  Warehouse,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  loadDepotInventory,
  updateDepotInventory,
} from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

function formatNumber(value) {
  return new Intl.NumberFormat("en-US").format(Number(value ?? 0));
}

function formatUpdated(value) {
  if (!value) {
    return "Not recorded";
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function stockState(item) {
  const freeUnits = Number(item.freeUnits ?? 0);

  if (freeUnits === 0) {
    return "OUT";
  }
  if (freeUnits < 250) {
    return "LOW";
  }
  if (freeUnits < 750) {
    return "WATCH";
  }
  return "HEALTHY";
}

function stateLabel(state) {
  return {
    HEALTHY: "Healthy",
    LOW: "Low stock",
    OUT: "Out of stock",
    WATCH: "Watch",
  }[state];
}

function InventoryMetric({ icon: Icon, label, note, value }) {
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

export default function DepotInventoryView({
  collapsed,
  onAction,
  onCollapse,
  onNavigate,
  onSignOut,
  permissions,
  user,
}) {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [editItem, setEditItem] = useState(null);
  const [availableUnits, setAvailableUnits] = useState("");
  const [reservedUnits, setReservedUnits] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setError("");
    try {
      setInventory(await loadDepotInventory());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!editItem) {
      return undefined;
    }

    const closeFromEscape = (event) => {
      if (event.key === "Escape" && !saving) {
        setEditItem(null);
      }
    };

    document.addEventListener("keydown", closeFromEscape);
    return () => document.removeEventListener("keydown", closeFromEscape);
  }, [editItem, saving]);

  const categories = useMemo(
    () =>
      Array.from(new Set(inventory.map((item) => item.category))).sort(),
    [inventory],
  );

  const filteredInventory = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return inventory
      .filter((item) => category === "ALL" || item.category === category)
      .filter((item) => status === "ALL" || stockState(item) === status)
      .filter(
        (item) =>
          !normalizedQuery ||
          [item.productCode, item.productName, item.category].some((value) =>
            String(value ?? "")
              .toLowerCase()
              .includes(normalizedQuery),
          ),
      )
      .sort((left, right) => {
        const stateOrder = { OUT: 0, LOW: 1, WATCH: 2, HEALTHY: 3 };
        const stateDifference =
          stateOrder[stockState(left)] - stateOrder[stockState(right)];
        return stateDifference || left.productCode.localeCompare(right.productCode);
      });
  }, [category, inventory, query, status]);

  const summary = useMemo(() => {
    const totalAvailable = inventory.reduce(
      (total, item) => total + Number(item.availableUnits ?? 0),
      0,
    );
    const totalReserved = inventory.reduce(
      (total, item) => total + Number(item.reservedUnits ?? 0),
      0,
    );
    const attention = inventory.filter((item) =>
      ["LOW", "OUT"].includes(stockState(item)),
    ).length;

    return { attention, totalAvailable, totalReserved };
  }, [inventory]);

  const openEdit = (item) => {
    setEditItem(item);
    setAvailableUnits(String(item.availableUnits));
    setReservedUnits(String(item.reservedUnits));
    setError("");
    setMessage("");
  };

  const saveInventory = async (event) => {
    event.preventDefault();
    const available = Number(availableUnits);
    const reserved = Number(reservedUnits);

    if (reserved > available) {
      setError("Reserved units cannot exceed available units.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      const updated = await updateDepotInventory(
        editItem.productId,
        available,
        reserved,
      );
      setInventory((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setEditItem(null);
      setMessage(`${updated.productCode} inventory was updated.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Depot Inventory"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onSignOut={onSignOut}
        permissions={permissions}
        user={user}
      />

      <main className="dashboard inventory-page">
        <header className="topbar">
          <h1>Depot Inventory</h1>
          <label className="search-box">
            <Search aria-hidden="true" size={15} />
            <span className="sr-only">Search depot inventory</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search product, code, or category..."
              type="search"
              value={query}
            />
          </label>
          <div className="avatar" aria-label="Signed in as SM">
            SM
          </div>
        </header>

        {(error || message) && (
          <div className={error ? "notice error" : "notice"} role="status">
            {error || message}
          </div>
        )}

        <section className="metrics-grid" aria-label="Depot inventory summary">
          <InventoryMetric
            icon={Boxes}
            label="Products Tracked"
            note="Active depot SKUs"
            value={inventory.length}
          />
          <InventoryMetric
            icon={Warehouse}
            label="Available Units"
            note="Physical depot stock"
            value={formatNumber(summary.totalAvailable)}
          />
          <InventoryMetric
            icon={PackageCheck}
            label="Reserved Units"
            note="Committed to shipments"
            value={formatNumber(summary.totalReserved)}
          />
          <InventoryMetric
            icon={CircleAlert}
            label="Needs Attention"
            note="Low or unavailable stock"
            value={summary.attention}
          />
        </section>

        <section className="table-panel inventory-table-panel">
          <div className="panel-toolbar inventory-toolbar">
            <div>
              <h2>Depot Stock</h2>
              <span className="panel-subtitle">
                {filteredInventory.length} product
                {filteredInventory.length === 1 ? "" : "s"}
              </span>
            </div>
            <div className="inventory-filters">
              <label>
                <span>Category</span>
                <select
                  onChange={(event) => setCategory(event.target.value)}
                  value={category}
                >
                  <option value="ALL">All categories</option>
                  {categories.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Stock status</span>
                <select
                  onChange={(event) => setStatus(event.target.value)}
                  value={status}
                >
                  <option value="ALL">All statuses</option>
                  <option value="OUT">Out of stock</option>
                  <option value="LOW">Low stock</option>
                  <option value="WATCH">Watch</option>
                  <option value="HEALTHY">Healthy</option>
                </select>
              </label>
            </div>
          </div>

          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Available</th>
                  <th>Reserved</th>
                  <th>Free Stock</th>
                  <th>Status</th>
                  <th>Last Updated</th>
                  {permissions.canManageInventory && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredInventory.map((item) => {
                  const itemStatus = stockState(item);
                  return (
                    <tr key={item.id}>
                      <td>
                        <div className="table-primary">
                          <strong>{item.productCode}</strong>
                          <span>{item.productName}</span>
                        </div>
                      </td>
                      <td>{item.category}</td>
                      <td>{formatNumber(item.availableUnits)}</td>
                      <td>{formatNumber(item.reservedUnits)}</td>
                      <td>
                        <strong>{formatNumber(item.freeUnits)}</strong>
                      </td>
                      <td>
                        <span className={`inventory-status ${itemStatus.toLowerCase()}`}>
                          {itemStatus === "HEALTHY" && (
                            <CheckCircle2 aria-hidden="true" size={13} />
                          )}
                          {stateLabel(itemStatus)}
                        </span>
                      </td>
                      <td>{formatUpdated(item.lastUpdated)}</td>
                      {permissions.canManageInventory && (
                        <td>
                          <button
                            aria-label={`Edit inventory for ${item.productCode}`}
                            className="row-action inventory-edit"
                            onClick={() => openEdit(item)}
                            title="Edit stock quantities"
                            type="button"
                          >
                            <PencilLine aria-hidden="true" size={14} />
                            Edit
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {!loading && filteredInventory.length === 0 && (
            <div className="shipments-empty">
              <Warehouse aria-hidden="true" size={24} strokeWidth={1.5} />
              <strong>No inventory matches this view</strong>
              <span>Change the search or stock filters.</span>
            </div>
          )}
          {loading && (
            <div className="shipments-empty">
              <span>Loading depot inventory...</span>
            </div>
          )}
        </section>
      </main>

      {editItem && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (event.target === event.currentTarget && !saving) {
              setEditItem(null);
            }
          }}
        >
          <section
            aria-labelledby="edit-inventory-title"
            aria-modal="true"
            className="inventory-dialog"
            role="dialog"
          >
            <header>
              <div>
                <span>{editItem.productCode}</span>
                <h2 id="edit-inventory-title">Update depot stock</h2>
              </div>
              <button
                aria-label="Close inventory dialog"
                className="icon-button"
                disabled={saving}
                onClick={() => setEditItem(null)}
                type="button"
              >
                <X aria-hidden="true" size={17} />
              </button>
            </header>
            <p>{editItem.productName}</p>
            <form onSubmit={saveInventory}>
              <div className="inventory-quantity-fields">
                <label>
                  Available units
                  <input
                    min="0"
                    onChange={(event) => setAvailableUnits(event.target.value)}
                    required
                    type="number"
                    value={availableUnits}
                  />
                </label>
                <label>
                  Reserved units
                  <input
                    min="0"
                    onChange={(event) => setReservedUnits(event.target.value)}
                    required
                    type="number"
                    value={reservedUnits}
                  />
                </label>
              </div>
              <div className="inventory-free-preview">
                <span>Free stock after update</span>
                <strong>
                  {formatNumber(
                    Math.max(0, Number(availableUnits) - Number(reservedUnits)),
                  )}
                </strong>
              </div>
              <footer>
                <button
                  className="secondary-button"
                  disabled={saving}
                  onClick={() => setEditItem(null)}
                  type="button"
                >
                  Cancel
                </button>
                <button className="save-button" disabled={saving} type="submit">
                  {saving ? "Saving..." : "Save inventory"}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}

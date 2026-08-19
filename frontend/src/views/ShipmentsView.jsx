import {
  Ban,
  CalendarDays,
  Check,
  CircleCheckBig,
  Clock3,
  PackageCheck,
  Plus,
  Search,
  Truck,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createShipment,
  loadShipmentPageData,
  updateShipmentStatus,
} from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

const STATUS_ACTIONS = {
  PLANNED: ["READY", "Mark ready", CircleCheckBig],
  READY: ["DISPATCHED", "Dispatch", Truck],
  DISPATCHED: ["DELIVERED", "Mark delivered", PackageCheck],
};

function dateInputValue(offsetDays) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}

function formatDate(value) {
  if (!value) {
    return "Not set";
  }

  return new Intl.DateTimeFormat("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function formatNumber(value) {
  return new Intl.NumberFormat("en-US").format(Number(value ?? 0));
}

function ShipmentMetric({ icon: Icon, label, value, note }) {
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

export default function ShipmentsView({
  collapsed,
  onCollapse,
  onAction,
  onNavigate,
  onSignOut,
  permissions,
  user,
}) {
  const [shipments, setShipments] = useState([]);
  const [approvedRecommendations, setApprovedRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [planOpen, setPlanOpen] = useState(false);
  const [storeId, setStoreId] = useState("");
  const [selectedIds, setSelectedIds] = useState([]);
  const [dispatchDate, setDispatchDate] = useState(dateInputValue(1));
  const [deliveryDate, setDeliveryDate] = useState(dateInputValue(2));
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [updatingId, setUpdatingId] = useState(null);

  const load = useCallback(async () => {
    setError("");

    try {
      const result = await loadShipmentPageData();
      setShipments(result.shipments);
      setApprovedRecommendations(result.approvedRecommendations);
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
    if (!planOpen) {
      return undefined;
    }

    const closeFromEscape = (event) => {
      if (event.key === "Escape" && !saving) {
        setPlanOpen(false);
      }
    };

    document.addEventListener("keydown", closeFromEscape);
    return () => document.removeEventListener("keydown", closeFromEscape);
  }, [planOpen, saving]);

  const storeOptions = useMemo(
    () =>
      Array.from(
        new Map(
          approvedRecommendations.map((item) => [
            String(item.storeId),
            `${item.storeCode} - ${item.storeName}`,
          ]),
        ),
      ),
    [approvedRecommendations],
  );

  const storeRecommendations = useMemo(
    () =>
      approvedRecommendations.filter(
        (item) => String(item.storeId) === storeId,
      ),
    [approvedRecommendations, storeId],
  );

  const filteredShipments = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return shipments.filter((shipment) => {
      const matchesStatus =
        statusFilter === "ALL" || shipment.status === statusFilter;
      const matchesQuery =
        !normalizedQuery ||
        [
          shipment.shipmentNumber,
          shipment.storeCode,
          shipment.storeName,
          ...shipment.items.flatMap((item) => [
            item.productCode,
            item.productName,
          ]),
        ].some((value) =>
          String(value ?? "")
            .toLowerCase()
            .includes(normalizedQuery),
        );

      return matchesStatus && matchesQuery;
    });
  }, [query, shipments, statusFilter]);

  const summary = useMemo(
    () => ({
      total: shipments.length,
      queued: shipments.filter(
        (item) => item.status === "PLANNED" || item.status === "READY",
      ).length,
      transit: shipments.filter((item) => item.status === "DISPATCHED").length,
      delivered: shipments.filter((item) => item.status === "DELIVERED").length,
    }),
    [shipments],
  );

  const resetPlan = () => {
    setStoreId("");
    setSelectedIds([]);
    setDispatchDate(dateInputValue(1));
    setDeliveryDate(dateInputValue(2));
    setNotes("");
  };

  const openPlan = () => {
    resetPlan();
    setError("");
    setMessage("");
    setPlanOpen(true);
  };

  const toggleRecommendation = (id) => {
    setSelectedIds((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id],
    );
  };

  const saveShipment = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");

    try {
      const shipment = await createShipment({
        recommendationIds: selectedIds,
        plannedDispatchDate: dispatchDate,
        expectedDeliveryDate: deliveryDate,
        notes: notes.trim() || null,
      });
      setShipments((current) => [shipment, ...current]);
      setApprovedRecommendations((current) =>
        current.filter((item) => !selectedIds.includes(item.id)),
      );
      setPlanOpen(false);
      setMessage(
        `${shipment.shipmentNumber} was planned for ${shipment.storeCode}.`,
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  };

  const changeStatus = async (shipment, status) => {
    setUpdatingId(shipment.id);
    setError("");
    setMessage("");

    try {
      const updated = await updateShipmentStatus(shipment.id, status);
      setShipments((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setMessage(`${updated.shipmentNumber} is now ${status.toLowerCase()}.`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Shipments"
        collapsed={collapsed}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onAction={onAction}
        onSignOut={onSignOut}
        permissions={permissions}
        user={user}
      />

      <main className="dashboard shipments-page">
        <header className="topbar">
          <h1>Shipments</h1>

          <label className="search-box">
            <Search aria-hidden="true" size={15} strokeWidth={2} />
            <span className="sr-only">Search shipments</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search shipment, store, or product..."
              type="search"
              value={query}
            />
          </label>

          <div className="avatar" aria-label="Signed in as SM">
            SM
          </div>
        </header>

        <div className="page-heading">
          <div>
            <span>Depot operations</span>
            <h2>Plan and track store deliveries</h2>
          </div>
          {permissions.canPlanShipments && (
            <button className="primary-button" onClick={openPlan} type="button">
              <Plus aria-hidden="true" size={16} />
              Plan shipment
            </button>
          )}
        </div>

        {(error || message) && (
          <div className={error ? "notice error" : "notice"} role="status">
            {error || message}
          </div>
        )}

        <section className="metrics-grid" aria-label="Shipment summary">
          <ShipmentMetric
            icon={Truck}
            label="Total Shipments"
            note="All shipment records"
            value={summary.total}
          />
          <ShipmentMetric
            icon={Clock3}
            label="Queued"
            note="Planned or ready"
            value={summary.queued}
          />
          <ShipmentMetric
            icon={CalendarDays}
            label="In Transit"
            note="Currently dispatched"
            value={summary.transit}
          />
          <ShipmentMetric
            icon={Check}
            label="Delivered"
            note="Received by stores"
            value={summary.delivered}
          />
        </section>

        <section className="table-panel shipments-table-panel">
          <div className="panel-toolbar">
            <div>
              <h2>Shipment Register</h2>
              <span className="panel-subtitle">
                {filteredShipments.length} shipment
                {filteredShipments.length === 1 ? "" : "s"}
              </span>
            </div>
            <label className="status-filter">
              <span>Status</span>
              <select
                onChange={(event) => setStatusFilter(event.target.value)}
                value={statusFilter}
              >
                <option value="ALL">All statuses</option>
                <option value="PLANNED">Planned</option>
                <option value="READY">Ready</option>
                <option value="DISPATCHED">Dispatched</option>
                <option value="DELIVERED">Delivered</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </label>
          </div>

          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Shipment</th>
                  <th>Store</th>
                  <th>Products</th>
                  <th>Total Units</th>
                  <th>Dispatch</th>
                  <th>Expected Delivery</th>
                  <th>Status</th>
                  {permissions.canPlanShipments && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredShipments.map((shipment) => {
                  const action = STATUS_ACTIONS[shipment.status];
                  const ActionIcon = action?.[2];
                  const cancellable =
                    shipment.status === "PLANNED" ||
                    shipment.status === "READY";

                  return (
                    <tr key={shipment.id}>
                      <td>
                        <strong className="shipment-number">
                          {shipment.shipmentNumber}
                        </strong>
                      </td>
                      <td>
                        <div className="table-primary">
                          <strong>{shipment.storeCode}</strong>
                          <span>{shipment.storeName}</span>
                        </div>
                      </td>
                      <td>
                        <div className="table-primary">
                          <strong>{shipment.items.length} products</strong>
                          <span>
                            {shipment.items
                              .slice(0, 2)
                              .map((item) => item.productCode)
                              .join(", ")}
                            {shipment.items.length > 2 ? " +" : ""}
                          </span>
                        </div>
                      </td>
                      <td>{formatNumber(shipment.totalUnits)}</td>
                      <td>{formatDate(shipment.plannedDispatchDate)}</td>
                      <td>{formatDate(shipment.expectedDeliveryDate)}</td>
                      <td>
                        <span
                          className={`shipment-status ${shipment.status.toLowerCase()}`}
                        >
                          {shipment.status[0] +
                            shipment.status.slice(1).toLowerCase()}
                        </span>
                      </td>
                      {permissions.canPlanShipments && (
                        <td>
                          <div className="shipment-actions">
                          {action && (
                            <button
                              className="row-action primary"
                              disabled={updatingId === shipment.id}
                              onClick={() =>
                                changeStatus(shipment, action[0])
                              }
                              type="button"
                            >
                              <ActionIcon aria-hidden="true" size={14} />
                              {updatingId === shipment.id
                                ? "Saving"
                                : action[1]}
                            </button>
                          )}
                          {cancellable && (
                            <button
                              aria-label={`Cancel ${shipment.shipmentNumber}`}
                              className="row-action danger"
                              disabled={updatingId === shipment.id}
                              onClick={() =>
                                changeStatus(shipment, "CANCELLED")
                              }
                              title="Cancel shipment"
                              type="button"
                            >
                              <Ban aria-hidden="true" size={14} />
                            </button>
                          )}
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {!loading && filteredShipments.length === 0 && (
            <div className="shipments-empty">
              <Truck aria-hidden="true" size={24} strokeWidth={1.5} />
              <strong>No shipments in this view</strong>
              <span>
                Plan one from approved recommendations or change the filter.
              </span>
            </div>
          )}
          {loading && (
            <div className="shipments-empty">
              <span>Loading shipments...</span>
            </div>
          )}
        </section>
      </main>

      {planOpen && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (event.target === event.currentTarget && !saving) {
              setPlanOpen(false);
            }
          }}
        >
          <section
            aria-labelledby="plan-shipment-title"
            aria-modal="true"
            className="plan-dialog"
            role="dialog"
          >
            <header>
              <div>
                <span>Depot operation</span>
                <h2 id="plan-shipment-title">Plan shipment</h2>
              </div>
              <button
                aria-label="Close plan shipment dialog"
                className="icon-button"
                disabled={saving}
                onClick={() => setPlanOpen(false)}
                type="button"
              >
                <X aria-hidden="true" size={17} />
              </button>
            </header>

            <form onSubmit={saveShipment}>
              <label>
                Destination store
                <select
                  onChange={(event) => {
                    setStoreId(event.target.value);
                    setSelectedIds([]);
                  }}
                  required
                  value={storeId}
                >
                  <option value="">Choose a store</option>
                  {storeOptions.map(([id, label]) => (
                    <option key={id} value={id}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>

              <fieldset className="recommendation-picker">
                <legend>Approved recommendations</legend>
                {!storeId && <p>Choose a store to see approved stock.</p>}
                {storeId && storeRecommendations.length === 0 && (
                  <p>This store has no approved recommendations.</p>
                )}
                {storeRecommendations.map((item) => (
                  <label className="recommendation-option" key={item.id}>
                    <input
                      checked={selectedIds.includes(item.id)}
                      onChange={() => toggleRecommendation(item.id)}
                      type="checkbox"
                    />
                    <span>
                      <strong>
                        {item.productCode} · {item.productName}
                      </strong>
                      <small>
                        {formatNumber(item.recommendedShipment)} units ·{" "}
                        {item.priority.toLowerCase()} priority
                      </small>
                    </span>
                  </label>
                ))}
              </fieldset>

              <div className="date-fields">
                <label>
                  Dispatch date
                  <input
                    min={dateInputValue(0)}
                    onChange={(event) => setDispatchDate(event.target.value)}
                    required
                    type="date"
                    value={dispatchDate}
                  />
                </label>
                <label>
                  Expected delivery
                  <input
                    min={dispatchDate}
                    onChange={(event) => setDeliveryDate(event.target.value)}
                    required
                    type="date"
                    value={deliveryDate}
                  />
                </label>
              </div>

              <label>
                Notes
                <textarea
                  maxLength={500}
                  onChange={(event) => setNotes(event.target.value)}
                  placeholder="Optional dispatch instructions"
                  rows={3}
                  value={notes}
                />
              </label>

              <footer>
                <span>
                  {selectedIds.length} recommendation
                  {selectedIds.length === 1 ? "" : "s"} selected
                </span>
                <div>
                  <button
                    disabled={saving}
                    onClick={() => setPlanOpen(false)}
                    type="button"
                  >
                    Cancel
                  </button>
                  <button
                    className="primary-button"
                    disabled={saving || selectedIds.length === 0}
                    type="submit"
                  >
                    {saving ? "Planning..." : "Plan shipment"}
                  </button>
                </div>
              </footer>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}

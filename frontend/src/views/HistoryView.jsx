import {
  Archive,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  RotateCcw,
  Search,
  Truck,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  loadRecommendationHistory,
  updateRecommendationStatus,
} from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";

const PAGE_SIZE = 12;
const ACTIVE_STATUSES = new Set([
  "READY_FOR_TRANSPORT",
  "ASSIGNED_TO_ROUTE",
  "SHIPPED",
]);

function formatNumber(value) {
  return new Intl.NumberFormat("en-US").format(Number(value ?? 0));
}

function formatDate(value) {
  if (!value) {
    return "Not recorded";
  }

  return new Intl.DateTimeFormat("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function formatStatus(status) {
  return String(status ?? "Unknown")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (letter) => letter.toUpperCase());
}

function Metric({ icon: Icon, label, value, note }) {
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

function pageNumbers(currentPage, pageCount) {
  const values = new Set([1, pageCount]);
  for (
    let value = Math.max(1, currentPage - 1);
    value <= Math.min(pageCount, currentPage + 1);
    value += 1
  ) {
    values.add(value);
  }
  return [...values].sort((left, right) => left - right);
}

export default function HistoryView({
  collapsed,
  onAction,
  onCollapse,
  onNavigate,
  onSignOut,
  permissions,
  profile,
  user,
}) {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [undoItem, setUndoItem] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  const load = useCallback(async () => {
    setError("");
    try {
      setRecommendations(await loadRecommendationHistory());
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
    if (!undoItem) {
      return undefined;
    }

    const closeFromEscape = (event) => {
      if (event.key === "Escape" && !updatingId) {
        setUndoItem(null);
      }
    };

    document.addEventListener("keydown", closeFromEscape);
    return () => document.removeEventListener("keydown", closeFromEscape);
  }, [undoItem, updatingId]);

  const history = useMemo(
    () =>
      recommendations.filter(
        (item) =>
          item.status !== "PENDING" &&
          item.status !== "EDITED" &&
          Number(item.recommendedShipment) > 0,
      ),
    [recommendations],
  );

  const summary = useMemo(
    () => ({
      total: history.length,
      approved: history.filter((item) => item.status === "APPROVED").length,
      transport: history.filter((item) => ACTIVE_STATUSES.has(item.status)).length,
      closed: history.filter((item) =>
        ["DELIVERED", "REJECTED", "CANCELLED"].includes(item.status),
      ).length,
    }),
    [history],
  );

  const filteredHistory = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return history
      .filter(
        (item) => statusFilter === "ALL" || item.status === statusFilter,
      )
      .filter((item) => {
        if (!normalizedQuery) {
          return true;
        }

        return [
          item.storeCode,
          item.storeName,
          item.productCode,
          item.productName,
          item.category,
          item.status,
          item.priority,
        ].some((value) =>
          String(value ?? "").toLowerCase().includes(normalizedQuery),
        );
      })
      .sort(
        (left, right) =>
          new Date(right.recommendationDate ?? 0) -
          new Date(left.recommendationDate ?? 0),
      );
  }, [history, query, statusFilter]);

  const pageCount = Math.max(1, Math.ceil(filteredHistory.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const visibleHistory = filteredHistory.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );
  const pages = pageNumbers(currentPage, pageCount);

  useEffect(() => {
    setPage(1);
  }, [query, statusFilter]);

  const undoApproval = async () => {
    setUpdatingId(undoItem.id);
    setError("");
    setMessage("");

    try {
      const updated = await updateRecommendationStatus(undoItem.id, "PENDING");
      setRecommendations((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setUndoItem(null);
      setMessage(
        `${updated.storeCode} / ${updated.productCode} was returned to pending review.`,
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="History"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onSignOut={onSignOut}
        permissions={permissions}
        profile={profile}
        user={user}
      />

      <main className="dashboard history-page">
        <header className="topbar">
          <h1>History</h1>
          <label className="search-box">
            <Search aria-hidden="true" size={15} strokeWidth={2} />
            <span className="sr-only">Search recommendation history</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search store, product, category, or status..."
              type="search"
              value={query}
            />
          </label>
          <HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} />
        </header>

        <div className="page-heading">
          <div>
            <span>Recommendation archive</span>
            <h2>Review completed decisions and transport activity</h2>
          </div>
        </div>

        {(error || message) && (
          <div className={error ? "notice error" : "notice"} role="status">
            {error || message}
          </div>
        )}

        <section className="metrics-grid" aria-label="History summary">
          <Metric icon={Archive} label="History Records" note="All archived recommendations" value={summary.total} />
          <Metric icon={Clock3} label="Approved" note="Waiting for transport planning" value={summary.approved} />
          <Metric icon={Truck} label="In Transport" note="Ready, assigned, or shipped" value={summary.transport} />
          <Metric icon={CheckCircle2} label="Closed" note="Delivered, rejected, or cancelled" value={summary.closed} />
        </section>

        <section className="table-panel history-table-panel">
          <div className="panel-toolbar">
            <div>
              <h2>Recommendation history</h2>
              <span className="panel-subtitle">
                Decisions are removed from the dashboard after approval or rejection
              </span>
            </div>
            <label className="status-filter">
              <span>Status</span>
              <select
                onChange={(event) => setStatusFilter(event.target.value)}
                value={statusFilter}
              >
                <option value="ALL">All statuses</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="READY_FOR_TRANSPORT">Ready for transport</option>
                <option value="ASSIGNED_TO_ROUTE">Assigned to route</option>
                <option value="SHIPPED">Shipped</option>
                <option value="DELIVERED">Delivered</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </label>
          </div>

          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Store</th>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Predicted Demand</th>
                  <th>Shipment</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Record Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {visibleHistory.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <div className="table-primary">
                        <strong>{item.storeCode}</strong>
                        <span>{item.storeName}</span>
                      </div>
                    </td>
                    <td>
                      <div className="table-primary">
                        <strong>{item.productCode}</strong>
                        <span>{item.productName}</span>
                      </div>
                    </td>
                    <td>{item.category}</td>
                    <td>{formatNumber(item.predictedDemand)}</td>
                    <td>{formatNumber(item.recommendedShipment)}</td>
                    <td>
                      <span className={`priority ${item.priority.toLowerCase()}`}>
                        {formatStatus(item.priority)}
                      </span>
                    </td>
                    <td>
                      <span className={`history-status ${item.status.toLowerCase()}`}>
                        {formatStatus(item.status)}
                      </span>
                    </td>
                    <td>{formatDate(item.recommendationDate)}</td>
                    <td>
                      {item.status === "APPROVED" ? (
                        <button
                          className="history-undo-button"
                          onClick={() => setUndoItem(item)}
                          type="button"
                        >
                          <RotateCcw aria-hidden="true" size={13} />
                          Undo
                        </button>
                      ) : (
                        <span className="history-locked">Recorded</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {loading && <div className="panel-empty">Loading history...</div>}
          {!loading && visibleHistory.length === 0 && (
            <div className="history-empty">
              <Archive aria-hidden="true" size={24} strokeWidth={1.5} />
              <strong>No history in this view</strong>
              <span>Approved and completed recommendations will appear here.</span>
            </div>
          )}

          {!loading && filteredHistory.length > 0 && (
            <div className="pagination">
              <span>{filteredHistory.length} records</span>
              <div>
                <button
                  aria-label="Previous page"
                  className="page-button page-arrow"
                  disabled={currentPage === 1}
                  onClick={() => setPage((current) => Math.max(1, current - 1))}
                  type="button"
                >
                  <ChevronLeft aria-hidden="true" size={14} />
                </button>
                {pages.map((pageNumber, index) => (
                  <span className="history-page-number" key={pageNumber}>
                    {index > 0 && pageNumber - pages[index - 1] > 1 && (
                      <span className="pagination-ellipsis">...</span>
                    )}
                    <button
                      aria-current={pageNumber === currentPage ? "page" : undefined}
                      className={pageNumber === currentPage ? "page-button active" : "page-button"}
                      onClick={() => setPage(pageNumber)}
                      type="button"
                    >
                      {pageNumber}
                    </button>
                  </span>
                ))}
                <button
                  aria-label="Next page"
                  className="page-button page-arrow"
                  disabled={currentPage === pageCount}
                  onClick={() => setPage((current) => Math.min(pageCount, current + 1))}
                  type="button"
                >
                  <ChevronRight aria-hidden="true" size={14} />
                </button>
              </div>
            </div>
          )}
        </section>
      </main>

      {undoItem && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (event.target === event.currentTarget && !updatingId) {
              setUndoItem(null);
            }
          }}
        >
          <section
            aria-labelledby="history-undo-title"
            aria-modal="true"
            className="override-dialog confirmation-dialog"
            role="dialog"
          >
            <header>
              <div>
                <span>History action</span>
                <h2 id="history-undo-title">Undo approval</h2>
              </div>
              <button
                aria-label="Close undo approval dialog"
                className="icon-button"
                disabled={Boolean(updatingId)}
                onClick={() => setUndoItem(null)}
                type="button"
              >
                <X aria-hidden="true" size={17} />
              </button>
            </header>
            <p>
              Return {undoItem.storeCode} / {undoItem.productCode} to the dashboard
              for review? Its shipment amount can be edited and approved again.
            </p>
            <footer>
              <button className="secondary-button" onClick={() => setUndoItem(null)} type="button">
                Keep approved
              </button>
              <button
                className="save-button undo-confirm-button"
                disabled={updatingId === undoItem.id}
                onClick={undoApproval}
                type="button"
              >
                <RotateCcw aria-hidden="true" size={13} />
                {updatingId === undoItem.id ? "Saving" : "Undo approval"}
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}

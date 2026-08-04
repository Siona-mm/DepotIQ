import {
  ArrowUpDown,
  Ban,
  BarChart3,
  Boxes,
  ChartNoAxesCombined,
  Check,
  ChevronLeft,
  ChevronRight,
  PencilLine,
  RefreshCw,
  Search,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Truck,
  Warehouse,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import AppSidebar from "../components/AppSidebar.jsx";
import {
  loadDashboardData,
  importHistoricalSalesCsv,
  overrideRecommendationAmount,
  syncMlRecommendations,
  updateRecommendationStatus,
} from "../api/depotiqApi.js";

const EMPTY_DATA = {
  storeInventory: [],
  depotInventory: [],
  forecasts: [],
  recommendations: [],
};

const PAGE_SIZE = 10;
const PRIORITY_ORDER = {
  URGENT: 0,
  HIGH: 1,
  NORMAL: 2,
  LOW: 3,
};

function paginationItems(currentPage, pageCount) {
  if (pageCount <= 7) {
    return Array.from({ length: pageCount }, (_, index) => index + 1);
  }

  let start = Math.max(2, currentPage - 1);
  let end = Math.min(pageCount - 1, currentPage + 1);

  if (currentPage <= 4) {
    start = 2;
    end = 5;
  } else if (currentPage >= pageCount - 3) {
    start = pageCount - 4;
    end = pageCount - 1;
  }

  const items = [1];

  if (start > 2) {
    items.push("ellipsis-start");
  }

  for (let pageNumber = start; pageNumber <= end; pageNumber += 1) {
    items.push(pageNumber);
  }

  if (end < pageCount - 1) {
    items.push("ellipsis-end");
  }

  items.push(pageCount);
  return items;
}

function formatNumber(value) {
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(
    Number(value ?? 0),
  );
}

function formatUpdated(value) {
  if (!value) {
    return "Not synced";
  }

  return new Intl.DateTimeFormat("en-US", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
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

function EmptyPanel({ children }) {
  return <div className="panel-empty">{children}</div>;
}

function isActionable(status) {
  return status === "PENDING" || status === "EDITED";
}

function recommendationActionLabel(status) {
  const labels = {
    APPROVED: "Approved",
    REJECTED: "Rejected",
    READY_FOR_TRANSPORT: "Ready",
    ASSIGNED_TO_ROUTE: "Assigned",
    SHIPPED: "Shipped",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
  };

  return labels[status] ?? "Approve";
}

function recommendationActionClass(status) {
  if (status === "APPROVED") {
    return "approve-button approved";
  }

  if (status === "REJECTED" || status === "CANCELLED") {
    return "approve-button rejected";
  }

  if (!isActionable(status)) {
    return "approve-button processed";
  }

  return "approve-button";
}

function recommendationActionTitle(status) {
  if (isActionable(status)) {
    return "Approve recommendation";
  }

  if (status === "APPROVED") {
    return "Approved and ready to be added to a shipment";
  }

  if (status === "REJECTED" || status === "CANCELLED") {
    return "This recommendation is closed";
  }

  return "This recommendation is already being processed in transportation planning";
}

function closeFromBackdrop(event, close) {
  if (event.target === event.currentTarget) {
    close();
  }
}

export default function DashboardView({
  collapsed,
  onCollapse,
  onNavigate,
}) {
  const [data, setData] = useState(EMPTY_DATA);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");
  const [syncMessage, setSyncMessage] = useState("");
  const [query, setQuery] = useState("");
  const [filterOpen, setFilterOpen] = useState(false);
  const [storeFilter, setStoreFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [descending, setDescending] = useState(true);
  const [page, setPage] = useState(1);
  const [overrideItem, setOverrideItem] = useState(null);
  const [overrideAmount, setOverrideAmount] = useState("");
  const [overrideReason, setOverrideReason] = useState("");
  const [savingOverride, setSavingOverride] = useState(false);
  const [rejectItem, setRejectItem] = useState(null);
  const [statusUpdate, setStatusUpdate] = useState(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const filterControlRef = useRef(null);

  const load = useCallback(async () => {
    setError("");

    try {
      setData(await loadDashboardData());
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
    if (!filterOpen) {
      return undefined;
    }

    const closeFilterFromOutside = (event) => {
      if (!filterControlRef.current?.contains(event.target)) {
        setFilterOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeFilterFromOutside);
    return () => {
      document.removeEventListener("pointerdown", closeFilterFromOutside);
    };
  }, [filterOpen]);

  useEffect(() => {
    const closePopupFromKeyboard = (event) => {
      if (event.key !== "Escape") {
        return;
      }

      setFilterOpen(false);
      if (!savingOverride) {
        setOverrideItem(null);
      }
      if (!statusUpdate) {
        setRejectItem(null);
      }
      if (!uploading) {
        setUploadOpen(false);
      }
    };

    document.addEventListener("keydown", closePopupFromKeyboard);
    return () => {
      document.removeEventListener("keydown", closePopupFromKeyboard);
    };
  }, [savingOverride, statusUpdate, uploading]);

  const depotInventoryByProduct = useMemo(
    () =>
      new Map(
        data.depotInventory.map((item) => [
          item.productId,
          Number(item.availableUnits ?? item.freeUnits ?? 0),
        ]),
      ),
    [data.depotInventory],
  );

  const dashboardRecommendations = useMemo(
    () =>
      data.recommendations.filter(
        (item) => item.status !== "DELIVERED",
      ),
    [data.recommendations],
  );

  const summary = useMemo(() => {
    const urgent = dashboardRecommendations.filter(
      (item) => item.priority === "URGENT",
    ).length;
    const pending = dashboardRecommendations.filter(
      (item) => item.status === "PENDING",
    ).length;
    const stores = new Set(data.storeInventory.map((item) => item.storeId)).size;
    const meanAccuracy =
      data.forecasts.length === 0
        ? 0
        : data.forecasts.reduce((total, forecast) => {
            const predicted = Math.max(Number(forecast.predictedDemand ?? 0), 1);
            const errorRatio = Number(forecast.modelMae ?? 0) / predicted;
            return total + Math.max(0, Math.min(100, (1 - errorRatio) * 100));
          }, 0) / data.forecasts.length;

    return {
      urgent,
      pending,
      stores,
      accuracy: `${meanAccuracy.toFixed(1)}%`,
    };
  }, [dashboardRecommendations, data.forecasts, data.storeInventory]);

  const filteredRecommendations = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return dashboardRecommendations
      .filter(
        (item) => storeFilter === "ALL" || item.storeCode === storeFilter,
      )
      .filter(
        (item) => priorityFilter === "ALL" || item.priority === priorityFilter,
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
          item.priority,
        ].some((value) =>
          String(value ?? "")
            .toLowerCase()
            .includes(normalizedQuery),
        );
      })
      .sort((left, right) => {
        const priorityDifference =
          (PRIORITY_ORDER[left.priority] ?? Number.MAX_SAFE_INTEGER) -
          (PRIORITY_ORDER[right.priority] ?? Number.MAX_SAFE_INTEGER);

        if (priorityDifference !== 0) {
          return priorityDifference;
        }

        const shipmentDifference =
          Number(right.recommendedShipment) -
          Number(left.recommendedShipment);
        return descending ? shipmentDifference : -shipmentDifference;
      });
  }, [
    dashboardRecommendations,
    descending,
    priorityFilter,
    query,
    storeFilter,
  ]);

  const storeOptions = useMemo(
    () =>
      Array.from(
        new Map(
          dashboardRecommendations.map((item) => [
            item.storeCode,
            `${item.storeCode} - ${item.storeName}`,
          ]),
        ),
      ),
    [dashboardRecommendations],
  );

  const filtersActive =
    storeFilter !== "ALL" || priorityFilter !== "ALL";
  const activeFilterCount =
    Number(storeFilter !== "ALL") + Number(priorityFilter !== "ALL");

  const pageCount = Math.max(
    1,
    Math.ceil(filteredRecommendations.length / PAGE_SIZE),
  );
  const currentPage = Math.min(page, pageCount);
  const visibleRecommendations = filteredRecommendations.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  useEffect(() => {
    setPage(1);
  }, [query, storeFilter, priorityFilter, descending]);

  const handleSync = async () => {
    setSyncing(true);
    setError("");
    setSyncMessage("");

    try {
      const result = await syncMlRecommendations();
      setSyncMessage(
        `${result.recommendationsSynced} recommendations synced. ` +
          `${result.skippedUnknownStoreOrProduct} unmatched model rows skipped.`,
      );
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSyncing(false);
    }
  };

  const visiblePages = paginationItems(currentPage, pageCount);

  const openOverride = (item) => {
    setOverrideItem(item);
    setOverrideAmount(String(item.recommendedShipment));
    setOverrideReason("");
  };

  const saveOverride = async (event) => {
    event.preventDefault();
    setSavingOverride(true);
    setError("");
    setSyncMessage("");

    try {
      const updated = await overrideRecommendationAmount(
        overrideItem.id,
        Number(overrideAmount),
        overrideReason,
      );
      setData((current) => ({
        ...current,
        recommendations: current.recommendations.map((item) =>
          item.id === updated.id ? updated : item,
        ),
      }));
      setOverrideItem(null);
      setSyncMessage(
        `Shipment for ${updated.storeCode} / ${updated.productCode} was updated and saved.`,
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSavingOverride(false);
    }
  };

  const changeRecommendationStatus = async (item, status) => {
    setStatusUpdate({ id: item.id, status });
    setError("");
    setSyncMessage("");

    try {
      const updated = await updateRecommendationStatus(item.id, status);
      setData((current) => ({
        ...current,
        recommendations: current.recommendations.map((recommendation) =>
          recommendation.id === updated.id ? updated : recommendation,
        ),
      }));
      setSyncMessage(
        `Shipment for ${updated.storeCode} / ${updated.productCode} was ${
          status === "APPROVED" ? "approved" : "rejected"
        }.`,
      );
      return true;
    } catch (requestError) {
      setError(requestError.message);
      return false;
    } finally {
      setStatusUpdate(null);
    }
  };

  const rejectRecommendation = async () => {
    const rejected = await changeRecommendationStatus(rejectItem, "REJECTED");
    if (rejected) {
      setRejectItem(null);
    }
  };

  const openUpload = () => {
    setUploadFile(null);
    setImportResult(null);
    setError("");
    setUploadOpen(true);
  };

  const uploadHistoricalSales = async (event) => {
    event.preventDefault();
    if (!uploadFile) {
      return;
    }

    setUploading(true);
    setError("");
    setImportResult(null);

    try {
      const result = await importHistoricalSalesCsv(uploadFile);
      setImportResult(result);
      setSyncMessage(
        `${result.createdRecords} sales records imported and ${result.updatedRecords} updated.`,
      );
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Dashboard"
        collapsed={collapsed}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onAction={(action) => {
          if (action === "upload") {
            openUpload();
          }
        }}
      />

      <main className="dashboard">
        <header className="topbar">
          <h1>Dashboard</h1>

          <label className="search-box">
            <Search aria-hidden="true" size={15} strokeWidth={2} />
            <span className="sr-only">Search recommendations</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search..."
              type="search"
              value={query}
            />
          </label>

          <div className="avatar" aria-label="Signed in as SM">
            SM
          </div>
        </header>

        {(error || syncMessage) && (
          <div className={error ? "notice error" : "notice"} role="status">
            {error || syncMessage}
          </div>
        )}

        <section className="metrics-grid" aria-label="Depot summary">
          <Metric
            icon={ShieldCheck}
            label="Stockout Risks"
            note="Stores at risk"
            value={summary.urgent}
          />
          <Metric
            icon={Truck}
            label="Pending Shipments"
            note="Total recommendations"
            value={summary.pending}
          />
          <Metric
            icon={Boxes}
            label="Forecast Accuracy"
            note="Across current forecasts"
            value={summary.accuracy}
          />
          <Metric
            icon={BarChart3}
            label="Stores Supplied"
            note="Across depot"
            value={summary.stores}
          />
        </section>

        <section className="dashboard-grid">
          <section className="table-panel">
            <div className="panel-toolbar">
              <h2>Shipment Recommendations</h2>
              <div className="table-actions">
                <div className="filter-control" ref={filterControlRef}>
                  <button
                    aria-expanded={filterOpen}
                    aria-haspopup="dialog"
                    className={
                      filtersActive ? "tool-button active" : "tool-button"
                    }
                    onClick={() => setFilterOpen((current) => !current)}
                    type="button"
                  >
                    <SlidersHorizontal aria-hidden="true" size={13} />
                    Filter
                    {filtersActive && (
                      <span className="filter-count">{activeFilterCount}</span>
                    )}
                  </button>

                  {filterOpen && (
                    <section
                      aria-label="Recommendation filters"
                      className="filter-popover"
                      role="dialog"
                    >
                      <header>
                        <strong>Filter recommendations</strong>
                        <span>{filteredRecommendations.length} results</span>
                      </header>

                      <label>
                        Store
                        <select
                          onChange={(event) => setStoreFilter(event.target.value)}
                          value={storeFilter}
                        >
                          <option value="ALL">All stores</option>
                          {storeOptions.map(([code, label]) => (
                            <option key={code} value={code}>
                              {label}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label>
                        Priority
                        <select
                          onChange={(event) =>
                            setPriorityFilter(event.target.value)
                          }
                          value={priorityFilter}
                        >
                          <option value="ALL">All priorities</option>
                          <option value="URGENT">Urgent</option>
                          <option value="HIGH">High</option>
                          <option value="NORMAL">Normal</option>
                          <option value="LOW">Low</option>
                        </select>
                      </label>

                      <footer>
                        <button
                          disabled={!filtersActive}
                          onClick={() => {
                            setStoreFilter("ALL");
                            setPriorityFilter("ALL");
                          }}
                          type="button"
                        >
                          Clear filters
                        </button>
                        <button
                          onClick={() => setFilterOpen(false)}
                          type="button"
                        >
                          Done
                        </button>
                      </footer>
                    </section>
                  )}
                </div>
                <button
                  className="tool-button"
                  onClick={() => setDescending((current) => !current)}
                  title="Reverse shipment quantity within each priority"
                  type="button"
                >
                  <ArrowUpDown aria-hidden="true" size={13} />
                  Sort
                </button>
                <button
                  className="tool-button"
                  disabled={syncing}
                  onClick={handleSync}
                  type="button"
                >
                  {syncing ? (
                    <RefreshCw
                      aria-hidden="true"
                      className="spinning"
                      size={13}
                    />
                  ) : (
                    <Settings aria-hidden="true" size={13} />
                  )}
                  {syncing ? "Syncing" : "Settings"}
                </button>
              </div>
            </div>

            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Store</th>
                    <th>Product</th>
                    <th>Store Stock</th>
                    <th>Depot Stock</th>
                    <th>Predicted Demand</th>
                    <th>Recommended Shipment</th>
                    <th>Priority</th>
                    <th>Updated</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleRecommendations.map((item) => (
                    <tr key={item.id}>
                      <td>{item.storeCode}</td>
                      <td>{item.productCode}</td>
                      <td>{formatNumber(item.currentInventory)}</td>
                      <td>
                        {formatNumber(
                          depotInventoryByProduct.get(item.productId),
                        )}
                      </td>
                      <td>{formatNumber(item.predictedDemand)}</td>
                      <td>
                        <div className="shipment-value">
                          <span>{formatNumber(item.recommendedShipment)}</span>
                          {item.originalRecommendedShipment != null && (
                            <small
                              title={
                                `Original model amount: ${formatNumber(
                                  item.originalRecommendedShipment,
                                )} units. ` +
                                `Changed by ${item.overriddenBy ?? "an admin"}. ` +
                                `Reason: ${item.overrideReason ?? "Not provided"}`
                              }
                            >
                              Edited
                            </small>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className={`priority ${item.priority.toLowerCase()}`}>
                          {item.priority[0] + item.priority.slice(1).toLowerCase()}
                        </span>
                      </td>
                      <td>{formatUpdated(item.recommendationDate)}</td>
                      <td className="recommendation-actions">
                        <div className="action-buttons">
                          <button
                            aria-label={`${recommendationActionLabel(
                              item.status,
                            )} shipment for ${item.storeCode} ${item.productCode}`}
                            className={recommendationActionClass(item.status)}
                            disabled={
                              !isActionable(item.status) ||
                              statusUpdate?.id === item.id
                            }
                            onClick={() =>
                              changeRecommendationStatus(item, "APPROVED")
                            }
                            title={recommendationActionTitle(item.status)}
                            type="button"
                          >
                            {item.status === "REJECTED" ||
                            item.status === "CANCELLED" ? (
                              <Ban aria-hidden="true" size={13} />
                            ) : !isActionable(item.status) &&
                              item.status !== "APPROVED" ? (
                              <Truck aria-hidden="true" size={13} />
                            ) : (
                              <Check aria-hidden="true" size={13} />
                            )}
                            {statusUpdate?.id === item.id
                              ? "Saving"
                              : recommendationActionLabel(item.status)}
                          </button>
                          {isActionable(item.status) && (
                            <button
                              aria-label={`Reject shipment for ${item.storeCode} ${item.productCode}`}
                              className="reject-button"
                              disabled={statusUpdate?.id === item.id}
                              onClick={() => setRejectItem(item)}
                              title="Reject recommendation"
                              type="button"
                            >
                              <Ban aria-hidden="true" size={14} />
                            </button>
                          )}
                          <button
                            aria-label={`Edit shipment for ${item.storeCode} ${item.productCode}`}
                            className="override-button"
                            disabled={!isActionable(item.status)}
                            onClick={() => openOverride(item)}
                            title={
                              !isActionable(item.status)
                                ? "Completed recommendations are locked"
                                : "Edit shipment amount"
                            }
                            type="button"
                          >
                            <PencilLine aria-hidden="true" size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {!loading && visibleRecommendations.length === 0 && (
              <EmptyPanel>No recommendations match this view.</EmptyPanel>
            )}
            {loading && <EmptyPanel>Loading depot data...</EmptyPanel>}

            <footer className="pagination">
              <span>
                {filteredRecommendations.length} recommendation
                {filteredRecommendations.length === 1 ? "" : "s"}
              </span>
              <div aria-label="Table pages">
                <button
                  aria-label="Previous page"
                  className="page-button page-arrow"
                  disabled={currentPage === 1}
                  onClick={() => setPage((current) => Math.max(1, current - 1))}
                  type="button"
                >
                  <ChevronLeft aria-hidden="true" size={14} />
                </button>
                {visiblePages.map((item) =>
                  typeof item === "number" ? (
                    <button
                      aria-current={
                        currentPage === item ? "page" : undefined
                      }
                      aria-label={`Page ${item}`}
                      className={
                        currentPage === item
                          ? "page-button active"
                          : "page-button"
                      }
                      key={item}
                      onClick={() => setPage(item)}
                      type="button"
                    >
                      {item}
                    </button>
                  ) : (
                    <span
                      aria-hidden="true"
                      className="pagination-ellipsis"
                      key={item}
                    >
                      ...
                    </span>
                  ),
                )}
                <button
                  aria-label="Next page"
                  className="page-button page-arrow"
                  disabled={currentPage === pageCount}
                  onClick={() =>
                    setPage((current) => Math.min(pageCount, current + 1))
                  }
                  type="button"
                >
                  <ChevronRight aria-hidden="true" size={14} />
                </button>
              </div>
            </footer>
          </section>

          <aside className="right-rail">
            <section className="side-panel">
              <div className="side-panel-heading">
                <div>
                  <span>Model Coverage</span>
                  <strong>{data.forecasts.length} forecasts</strong>
                </div>
                <ChartNoAxesCombined aria-hidden="true" size={18} />
              </div>
              <div className="horizon-list">
                {[3, 7, 14, 30].map((horizon) => {
                  const count = data.forecasts.filter(
                    (forecast) => forecast.horizonDays === horizon,
                  ).length;
                  return (
                    <div key={horizon}>
                      <span>{horizon}-day plan</span>
                      <strong>{count}</strong>
                    </div>
                  );
                })}
              </div>
            </section>

            <section className="side-panel depot-panel">
              <div className="side-panel-heading">
                <div>
                  <span>Depot Inventory</span>
                  <strong>{data.depotInventory.length} products</strong>
                </div>
                <Warehouse aria-hidden="true" size={18} />
              </div>
              <div className="inventory-list">
                {data.depotInventory.slice(0, 5).map((item) => (
                  <div className="inventory-row" key={item.id}>
                    <div>
                      <strong>{item.productCode}</strong>
                      <span>{item.productName}</span>
                    </div>
                    <b>
                      {formatNumber(item.availableUnits ?? item.freeUnits)}
                    </b>
                  </div>
                ))}
              </div>
            </section>
          </aside>
        </section>
      </main>
      {uploadOpen && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (!uploading) {
              closeFromBackdrop(event, () => setUploadOpen(false));
            }
          }}
        >
          <section
            aria-labelledby="upload-dialog-title"
            aria-modal="true"
            className="upload-dialog"
            role="dialog"
          >
            <header>
              <div>
                <span>Historical data</span>
                <h2 id="upload-dialog-title">Import sales CSV</h2>
              </div>
              <button
                aria-label="Close import dialog"
                className="icon-button"
                disabled={uploading}
                onClick={() => setUploadOpen(false)}
                type="button"
              >
                <X aria-hidden="true" size={15} />
              </button>
            </header>
            <p>
              Upload the retail inventory CSV. Rows are matched by store, product, and date.
            </p>
            <form onSubmit={uploadHistoricalSales}>
              <label className="file-input">
                <span>CSV file</span>
                <input
                  accept=".csv,text/csv"
                  disabled={uploading}
                  onChange={(event) => setUploadFile(event.target.files?.[0] ?? null)}
                  type="file"
                />
              </label>
              {uploadFile && <small className="selected-file">Selected: {uploadFile.name}</small>}
              {importResult && (
                <div className="import-result" role="status">
                  <strong>Import complete</strong>
                  <span>
                    {importResult.processedRows} processed · {importResult.createdRecords} created · {importResult.updatedRecords} updated · {importResult.skippedRows} skipped
                  </span>
                  {importResult.errors?.length > 0 && (
                    <ul>
                      {importResult.errors.slice(0, 3).map((item) => <li key={item}>{item}</li>)}
                    </ul>
                  )}
                </div>
              )}
              <footer>
                <button
                  className="secondary-button"
                  disabled={uploading}
                  onClick={() => setUploadOpen(false)}
                  type="button"
                >
                  Cancel
                </button>
                <button className="save-button" disabled={!uploadFile || uploading} type="submit">
                  {uploading ? "Importing..." : "Import data"}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
      {overrideItem && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (!savingOverride) {
              closeFromBackdrop(event, () => setOverrideItem(null));
            }
          }}
        >
          <section aria-modal="true" className="override-dialog" role="dialog">
            <header>
              <div>
                <span>Admin action</span>
                <h2>Override shipment amount</h2>
              </div>
              <button
                aria-label="Close override dialog"
                className="icon-button"
                onClick={() => setOverrideItem(null)}
                type="button"
              >
                <X aria-hidden="true" size={17} />
              </button>
            </header>
            <p>
              {overrideItem.storeCode} / {overrideItem.productCode}. Model
              recommendation:{" "}
              {formatNumber(
                overrideItem.originalRecommendedShipment ??
                  overrideItem.recommendedShipment,
              )}{" "}
              units. Current shipment:{" "}
              {formatNumber(overrideItem.recommendedShipment)} units.
            </p>
            <form onSubmit={saveOverride}>
              <label>
                Shipment amount
                <input
                  min="0"
                  onChange={(event) => setOverrideAmount(event.target.value)}
                  required
                  type="number"
                  value={overrideAmount}
                />
              </label>
              <label>
                Reason for override
                <textarea
                  maxLength="500"
                  onChange={(event) => setOverrideReason(event.target.value)}
                  required
                  rows="3"
                  value={overrideReason}
                />
              </label>
              <footer>
                <button
                  className="secondary-button"
                  onClick={() => setOverrideItem(null)}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className="save-button"
                  disabled={savingOverride}
                  type="submit"
                >
                  {savingOverride ? "Saving..." : "Save override"}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
      {rejectItem && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (!statusUpdate) {
              closeFromBackdrop(event, () => setRejectItem(null));
            }
          }}
        >
          <section
            aria-labelledby="reject-dialog-title"
            aria-modal="true"
            className="override-dialog reject-dialog"
            role="dialog"
          >
            <header>
              <div>
                <span>Admin action</span>
                <h2 id="reject-dialog-title">Reject recommendation</h2>
              </div>
              <button
                aria-label="Close reject dialog"
                className="icon-button"
                onClick={() => setRejectItem(null)}
                type="button"
              >
                <X aria-hidden="true" size={17} />
              </button>
            </header>
            <p>
              Reject the shipment of{" "}
              {formatNumber(rejectItem.recommendedShipment)} units for{" "}
              {rejectItem.storeCode} / {rejectItem.productCode}? This removes it
              from the pending approval workflow.
            </p>
            <footer>
              <button
                className="secondary-button"
                onClick={() => setRejectItem(null)}
                type="button"
              >
                Keep recommendation
              </button>
              <button
                className="reject-confirm-button"
                disabled={
                  statusUpdate?.id === rejectItem.id &&
                  statusUpdate?.status === "REJECTED"
                }
                onClick={rejectRecommendation}
                type="button"
              >
                Reject recommendation
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}

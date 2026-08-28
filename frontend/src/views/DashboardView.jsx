import {
  ArrowUpDown,
  Ban,
  BarChart3,
  Boxes,
  ChartNoAxesCombined,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  PencilLine,
  RefreshCw,
  RotateCcw,
  Search,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Truck,
  Warehouse,
  X,
} from "lucide-react";
import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import AppSidebar from "../components/AppSidebar.jsx";
import MlStatusPanel from "../components/MlStatusPanel.jsx";
import RecommendationInsightsDialog from "../components/RecommendationInsightsDialog.jsx";
import RetryNotice from "../components/RetryNotice.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";
import {
  loadDashboardData,
  approveAndDispatchShipment,
  loadSettings,
  importHistoricalSalesCsv,
  overrideRecommendationAmount,
  loadMlStatus,
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

const SORT_OPTIONS = [
  { value: "PRIORITY_ASC", label: "Priority", note: "Urgent first" },
  { value: "PRIORITY_DESC", label: "Priority", note: "Low first" },
  { value: "SHIPMENT_DESC", label: "Shipment", note: "Highest quantity first" },
  { value: "SHIPMENT_ASC", label: "Shipment", note: "Lowest quantity first" },
  { value: "DEMAND_DESC", label: "Predicted demand", note: "Highest first" },
  { value: "DEMAND_ASC", label: "Predicted demand", note: "Lowest first" },
  { value: "STOCK_ASC", label: "Store stock", note: "Lowest first" },
  { value: "UPDATED_DESC", label: "Last updated", note: "Newest first" },
];

function compareRecommendations(left, right, sortBy) {
  const priorityDifference =
    (PRIORITY_ORDER[left.priority] ?? Number.MAX_SAFE_INTEGER) -
    (PRIORITY_ORDER[right.priority] ?? Number.MAX_SAFE_INTEGER);
  const shipmentDifference =
    Number(right.recommendedShipment) - Number(left.recommendedShipment);

  switch (sortBy) {
    case "PRIORITY_DESC":
      return -priorityDifference || shipmentDifference;
    case "SHIPMENT_DESC":
      return shipmentDifference || priorityDifference;
    case "SHIPMENT_ASC":
      return -shipmentDifference || priorityDifference;
    case "DEMAND_DESC":
      return (
        Number(right.predictedDemand) - Number(left.predictedDemand) ||
        priorityDifference
      );
    case "DEMAND_ASC":
      return (
        Number(left.predictedDemand) - Number(right.predictedDemand) ||
        priorityDifference
      );
    case "STOCK_ASC":
      return (
        Number(left.currentInventory) - Number(right.currentInventory) ||
        priorityDifference
      );
    case "UPDATED_DESC":
      return (
        new Date(right.recommendationDate).getTime() -
          new Date(left.recommendationDate).getTime() || priorityDifference
      );
    default:
      return priorityDifference || shipmentDifference;
  }
}

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

function toIsoDate(date) {
  return date.toISOString().slice(0, 10);
}

function shipmentDates(item) {
  const expected = new Date(`${item.recommendationDate}T12:00:00`);
  expected.setDate(expected.getDate() + Number(item.horizonDays ?? 0));
  const dispatch = new Date(expected);
  dispatch.setDate(
    dispatch.getDate() - Number(item.deliveryLeadTimeDays ?? 0),
  );
  const today = new Date();
  today.setHours(12, 0, 0, 0);

  if (dispatch < today) {
    dispatch.setTime(today.getTime());
  }
  if (expected < dispatch) {
    expected.setTime(dispatch.getTime());
  }

  return {
    dispatchDate: toIsoDate(dispatch),
    expectedDeliveryDate: toIsoDate(expected),
  };
}

function groupRecommendations(recommendations, recentImportKeySet) {
  const groups = new Map();

  recommendations.forEach((item) => {
    const normalizedStoreCode = String(item.storeCode ?? item.storeId).trim();
    const normalizedHorizon = Number(item.horizonDays);
    const key = [normalizedStoreCode, normalizedHorizon].join("::");
    const current = groups.get(key) ?? {
      key,
      storeId: item.storeId,
      storeCode: normalizedStoreCode,
      storeName: item.storeName,
      horizonDays: normalizedHorizon,
      recentlyImported: false,
      itemsByProduct: new Map(),
    };

    current.recentlyImported ||= recentImportKeySet.has(
      `${item.storeCode}::${item.productCode}`,
    );

    const productKey = String(item.productCode ?? item.productId).trim();
    const existing = current.itemsByProduct.get(productKey);
    const itemDate = String(item.recommendationDate ?? "");
    const existingDate = String(existing?.recommendationDate ?? "");
    const itemIsNewer =
      !existing ||
      itemDate > existingDate ||
      (itemDate === existingDate && Number(item.id ?? 0) > Number(existing.id ?? 0));

    if (itemIsNewer) {
      current.itemsByProduct.set(productKey, item);
    }
    groups.set(key, current);
  });

  return Array.from(groups.values()).map((group) => {
    const items = Array.from(group.itemsByProduct.values());
    const recommendationDate = items.reduce(
      (latest, item) =>
        String(item.recommendationDate ?? "") > latest
          ? String(item.recommendationDate)
          : latest,
      "",
    );
    const scheduleItem = { ...items[0], recommendationDate };
    const dates = shipmentDates(scheduleItem);
    const priority = items.reduce(
      (highest, item) =>
        (PRIORITY_ORDER[item.priority] ?? Number.MAX_SAFE_INTEGER) <
        (PRIORITY_ORDER[highest] ?? Number.MAX_SAFE_INTEGER)
          ? item.priority
          : highest,
      items[0]?.priority ?? "LOW",
    );

    return {
      ...group,
      itemsByProduct: undefined,
      items,
      recommendationDate,
      dispatchDate: dates.dispatchDate,
      expectedDeliveryDate: dates.expectedDeliveryDate,
      priority,
      recommendedShipment: items.reduce(
        (total, item) => total + Number(item.recommendedShipment ?? 0),
        0,
      ),
      predictedDemand: items.reduce(
        (total, item) => total + Number(item.predictedDemand ?? 0),
        0,
      ),
      currentInventory: items.reduce(
        (total, item) => total + Number(item.currentInventory ?? 0),
        0,
      ),
    };
  });
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

function closeFromBackdrop(event, close) {
  if (event.target === event.currentTarget) {
    close();
  }
}

export default function DashboardView({
  collapsed,
  onCollapse,
  onAction,
  onNavigate,
  onSignOut,
  permissions,
  profile,
  refreshRequest,
  recentlyImportedKeys = [],
  user,
}) {
  const [data, setData] = useState(EMPTY_DATA);
  const [, setWorkflowSettings] = useState({ allowOverrides: true });
  const [mlStatus, setMlStatus] = useState(null);
  const [mlStatusLoading, setMlStatusLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");
  const [syncMessage, setSyncMessage] = useState("");
  const [query, setQuery] = useState("");
  const [filterOpen, setFilterOpen] = useState(false);
  const [sortOpen, setSortOpen] = useState(false);
  const [storeFilter, setStoreFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [horizonFilter, setHorizonFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("UPDATED_DESC");
  const [page, setPage] = useState(1);
  const [overrideItem, setOverrideItem] = useState(null);
  const [insightItem, setInsightItem] = useState(null);
  const [overrideAmount, setOverrideAmount] = useState("");
  const [overrideReason, setOverrideReason] = useState("");
  const [savingOverride, setSavingOverride] = useState(false);
  const [rejectItem, setRejectItem] = useState(null);
  const [undoItem, setUndoItem] = useState(null);
  const [statusUpdate, setStatusUpdate] = useState(null);
  const [expandedGroup, setExpandedGroup] = useState(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const filterControlRef = useRef(null);
  const sortControlRef = useRef(null);

  const load = useCallback(async () => {
    setError("");
    setLoading(true);
    setMlStatusLoading(true);

    try {
      const [dashboardResult, statusResult, settingsResult] = await Promise.allSettled([
        loadDashboardData(),
        loadMlStatus(),
        loadSettings(),
      ]);

      if (dashboardResult.status === "rejected") {
        throw dashboardResult.reason;
      }

      setData(dashboardResult.value);
      setMlStatus(
        statusResult.status === "fulfilled" ? statusResult.value : null,
      );
      if (settingsResult.status === "fulfilled") {
        setWorkflowSettings(settingsResult.value);
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
      setMlStatusLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load, refreshRequest]);

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
    if (!sortOpen) {
      return undefined;
    }

    const closeSortFromOutside = (event) => {
      if (!sortControlRef.current?.contains(event.target)) {
        setSortOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeSortFromOutside);
    return () => {
      document.removeEventListener("pointerdown", closeSortFromOutside);
    };
  }, [sortOpen]);

  useEffect(() => {
    const closePopupFromKeyboard = (event) => {
      if (event.key !== "Escape") {
        return;
      }

      setFilterOpen(false);
      setSortOpen(false);
      if (!savingOverride) {
        setOverrideItem(null);
      }
      if (!statusUpdate) {
        setRejectItem(null);
        setUndoItem(null);
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
        (item) =>
          isActionable(item.status) &&
          Number(item.recommendedShipment) > 0,
      ),
    [data.recommendations],
  );

  const recentImportKeySet = useMemo(
    () => new Set(recentlyImportedKeys),
    [recentlyImportedKeys],
  );

  const recommendationGroups = useMemo(
    () => groupRecommendations(dashboardRecommendations, recentImportKeySet),
    [dashboardRecommendations, recentImportKeySet],
  );

  const recentlyImportedGroups = useMemo(
    () => recommendationGroups.filter((group) => group.recentlyImported),
    [recommendationGroups],
  );

  const summary = useMemo(() => {
    const urgent = recommendationGroups.filter(
      (group) => group.priority === "URGENT",
    ).length;
    const pending = recommendationGroups.length;
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
  }, [recommendationGroups, data.forecasts, data.storeInventory]);

  const filteredRecommendations = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return recommendationGroups
      .filter(
        (group) => storeFilter === "ALL" || group.storeCode === storeFilter,
      )
      .filter(
        (group) => priorityFilter === "ALL" || group.priority === priorityFilter,
      )
      .filter(
        (group) =>
          horizonFilter === "ALL" ||
          group.horizonDays === Number(horizonFilter),
      )
      .filter((group) => {
        if (!normalizedQuery) {
          return true;
        }

        return [
          group.storeCode,
          group.storeName,
          group.priority,
          group.horizonDays,
          ...group.items.flatMap((item) => [
            item.productCode,
            item.productName,
            item.category,
          ]),
        ].some((value) =>
          String(value ?? "")
            .toLowerCase()
            .includes(normalizedQuery),
        );
      })
      .sort(
        (left, right) =>
          Number(right.recentlyImported) - Number(left.recentlyImported) ||
          left.dispatchDate.localeCompare(right.dispatchDate) ||
          compareRecommendations(left, right, sortBy) ||
          left.horizonDays - right.horizonDays ||
          left.storeCode.localeCompare(right.storeCode),
      );
  }, [
    recommendationGroups,
    horizonFilter,
    priorityFilter,
    query,
    sortBy,
    storeFilter,
  ]);

  const storeOptions = useMemo(
    () =>
      Array.from(
        new Map(
          recommendationGroups.map((group) => [
            group.storeCode,
            `${group.storeCode} - ${group.storeName}`,
          ]),
        ),
      ),
    [recommendationGroups],
  );

  const filtersActive =
    storeFilter !== "ALL" ||
    priorityFilter !== "ALL" ||
    horizonFilter !== "ALL";
  const activeFilterCount =
    Number(storeFilter !== "ALL") +
    Number(priorityFilter !== "ALL") +
    Number(horizonFilter !== "ALL");

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
  }, [query, storeFilter, priorityFilter, horizonFilter, sortBy]);

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
          status === "APPROVED"
            ? "approved"
            : status === "PENDING"
              ? "returned to pending review"
              : "rejected"
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

  const undoApproval = async () => {
    const reopened = await changeRecommendationStatus(undoItem, "PENDING");
    if (reopened) {
      setUndoItem(null);
    }
  };

  const approveShipmentGroup = async (group) => {
    setStatusUpdate({ id: group.key, status: "DISPATCHING" });
    setError("");
    setSyncMessage("");

    try {
      const shipment = await approveAndDispatchShipment(
        group.items.map((item) => item.id),
      );
      setSyncMessage(
        `${shipment.shipmentNumber} was approved and dispatched to ${shipment.storeCode}.`,
      );
      setExpandedGroup(null);
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setStatusUpdate(null);
    }
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
        onAction={onAction}
        onSignOut={onSignOut}
        permissions={permissions}
        profile={profile}
        user={user}
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

          <HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} />
        </header>

        {error ? (
          <RetryNotice message={error} onRetry={load} />
        ) : syncMessage ? (
          <div className="notice" role="status">{syncMessage}</div>
        ) : null}

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

        <MlStatusPanel
          loading={mlStatusLoading}
          onRetry={load}
          status={mlStatus}
        />

        <section className="dashboard-grid">
          <section className="table-panel" id="shipment-recommendations">
            {recentlyImportedGroups.length > 0 && (
              <div className="recent-import-banner">
                <strong>
                  {recentlyImportedGroups.length} shipment plan
                  {recentlyImportedGroups.length === 1 ? "" : "s"} refreshed
                </strong>
                <span>
                  Highlighted rows relate to your latest import and clear after a refresh.
                </span>
              </div>
            )}
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
                    onClick={() => {
                      setSortOpen(false);
                      setFilterOpen((current) => !current);
                    }}
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
                            setHorizonFilter("ALL");
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
                <label className="shipment-horizon-filter">
                  <span>Horizon</span>
                  <select
                    aria-label="Filter shipments by planning horizon"
                    onChange={(event) => setHorizonFilter(event.target.value)}
                    value={horizonFilter}
                  >
                    <option value="ALL">All plans</option>
                    <option value="3">3 days</option>
                    <option value="7">7 days</option>
                    <option value="14">14 days</option>
                    <option value="30">30 days</option>
                  </select>
                </label>
                <div className="filter-control" ref={sortControlRef}>
                  <button
                    aria-expanded={sortOpen}
                    aria-haspopup="dialog"
                    className={
                      sortBy === "UPDATED_DESC"
                        ? "tool-button"
                        : "tool-button active"
                    }
                    onClick={() => {
                      setFilterOpen(false);
                      setSortOpen((current) => !current);
                    }}
                    type="button"
                  >
                    <ArrowUpDown aria-hidden="true" size={13} />
                    Sort
                  </button>

                  {sortOpen && (
                    <section
                      aria-label="Recommendation sorting"
                      className="filter-popover sort-popover"
                      role="dialog"
                    >
                      <header>
                        <strong>Sort recommendations</strong>
                        <span>
                          {SORT_OPTIONS.find((option) => option.value === sortBy)
                            ?.note}
                        </span>
                      </header>
                      <div aria-label="Sort options" className="sort-options" role="radiogroup">
                        {SORT_OPTIONS.map((option) => (
                          <button
                            aria-checked={sortBy === option.value}
                            className={
                              sortBy === option.value
                                ? "sort-option selected"
                                : "sort-option"
                            }
                            key={option.value}
                            onClick={() => {
                              setSortBy(option.value);
                              setSortOpen(false);
                            }}
                            role="radio"
                            type="button"
                          >
                            <span>
                              <strong>{option.label}</strong>
                              <small>{option.note}</small>
                            </span>
                            {sortBy === option.value && (
                              <Check aria-hidden="true" size={14} />
                            )}
                          </button>
                        ))}
                      </div>
                    </section>
                  )}
                </div>
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
              <table className="shipment-plan-table">
                <thead>
                  <tr>
                    <th aria-label="Expand shipment" className="shipment-expand-column" />
                    <th>Dispatch</th>
                    <th>Store</th>
                    <th>Horizon</th>
                    <th>Products</th>
                    <th>Total Units</th>
                    <th>Priority</th>
                    {permissions.canManageRecommendations && <th>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {visibleRecommendations.map((group, index) => {
                    const expanded = expandedGroup === group.key;
                    const showDayDivider =
                      index === 0 ||
                      visibleRecommendations[index - 1].dispatchDate !==
                        group.dispatchDate;
                    const columnCount = permissions.canManageRecommendations
                      ? 8
                      : 7;

                    return (
                      <Fragment key={group.key}>
                        {showDayDivider && (
                          <tr className="shipment-day-divider">
                            <td colSpan={columnCount}>
                              Dispatch {formatUpdated(group.dispatchDate)}
                            </td>
                          </tr>
                        )}
                        <tr
                          className={
                            group.recentlyImported
                              ? "shipment-group-row recently-imported-row"
                              : "shipment-group-row"
                          }
                        >
                          <td className="shipment-expand-column">
                            <button
                              aria-expanded={expanded}
                              aria-label={`${expanded ? "Collapse" : "Expand"} shipment for ${group.storeCode}`}
                              className="shipment-expand-button"
                              onClick={() =>
                                setExpandedGroup(expanded ? null : group.key)
                              }
                              type="button"
                            >
                              {expanded ? (
                                <ChevronUp aria-hidden="true" size={15} />
                              ) : (
                                <ChevronDown aria-hidden="true" size={15} />
                              )}
                            </button>
                          </td>
                          <td>
                            <div className="shipment-date-cell">
                              <strong>{formatUpdated(group.dispatchDate)}</strong>
                              <small>
                                Arrives {formatUpdated(group.expectedDeliveryDate)}
                              </small>
                            </div>
                          </td>
                          <td>
                            <div className="shipment-store-cell">
                              <strong>
                                {group.storeCode}
                                {group.recentlyImported && (
                                  <span className="recent-import-badge">New</span>
                                )}
                              </strong>
                              <small>{group.storeName}</small>
                            </div>
                          </td>
                          <td>{group.horizonDays}-day plan</td>
                          <td>
                            <div className="shipment-product-summary">
                              <strong>
                                {group.items.length} product
                                {group.items.length === 1 ? "" : "s"}
                              </strong>
                              <small>
                                {group.items
                                  .slice(0, 3)
                                  .map((item) => item.productCode)
                                  .join(", ")}
                                {group.items.length > 3 ? " +" : ""}
                              </small>
                            </div>
                          </td>
                          <td>{formatNumber(group.recommendedShipment)}</td>
                          <td>
                            <span className={`priority ${group.priority.toLowerCase()}`}>
                              {group.priority[0] +
                                group.priority.slice(1).toLowerCase()}
                            </span>
                          </td>
                          {permissions.canManageRecommendations && (
                            <td className="recommendation-actions">
                              <button
                                className="approve-button shipment-dispatch-button"
                                disabled={statusUpdate?.id === group.key}
                                onClick={() => approveShipmentGroup(group)}
                                type="button"
                              >
                                <Truck aria-hidden="true" size={13} />
                                {statusUpdate?.id === group.key
                                  ? "Dispatching"
                                  : "Approve & dispatch"}
                              </button>
                            </td>
                          )}
                        </tr>
                        {expanded && (
                          <tr className="shipment-group-detail">
                            <td colSpan={columnCount}>
                              <div className="shipment-group-detail-panel">
                                <table className="shipment-product-table">
                                  <thead>
                                    <tr>
                                      <th>Product</th>
                                      <th>Category</th>
                                      <th>Store Stock</th>
                                      <th>Depot Stock</th>
                                      <th>Predicted Demand</th>
                                      <th>Units</th>
                                      {permissions.canManageRecommendations && (
                                        <th>Actions</th>
                                      )}
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {group.items.map((item) => (
                                      <tr key={item.id}>
                                        <td>
                                          <strong>{item.productCode}</strong>
                                          <small>{item.productName}</small>
                                        </td>
                                        <td>{item.category}</td>
                                        <td>{formatNumber(item.currentInventory)}</td>
                                        <td>
                                          {formatNumber(
                                            depotInventoryByProduct.get(
                                              item.productId,
                                            ),
                                          )}
                                        </td>
                                        <td>{formatNumber(item.predictedDemand)}</td>
                                        <td>
                                          <div className="shipment-value">
                                            <span>
                                              {formatNumber(
                                                item.recommendedShipment,
                                              )}
                                            </span>
                                            {item.originalRecommendedShipment !=
                                              null && <small>Edited</small>}
                                          </div>
                                        </td>
                                        {permissions.canManageRecommendations && (
                                          <td className="shipment-product-actions">
                                            <div className="action-buttons">
                                              <button
                                                aria-label={`Reject ${item.productCode}`}
                                                className="reject-button"
                                                disabled={
                                                  statusUpdate?.id === item.id
                                                }
                                                onClick={() => setRejectItem(item)}
                                                title="Remove product from this shipment plan"
                                                type="button"
                                              >
                                                <Ban aria-hidden="true" size={14} />
                                              </button>
                                              <button
                                                aria-label={`Edit ${item.productCode}`}
                                                className="override-button"
                                                onClick={() => openOverride(item)}
                                                title="Edit shipment amount"
                                                type="button"
                                              >
                                                <PencilLine
                                                  aria-hidden="true"
                                                  size={14}
                                                />
                                              </button>
                                            </div>
                                          </td>
                                        )}
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {!loading && visibleRecommendations.length === 0 && (
              <EmptyPanel>No recommendations match this view.</EmptyPanel>
            )}
            {loading && <EmptyPanel>Loading depot data...</EmptyPanel>}

            <footer className="pagination">
              <span>
                {filteredRecommendations.length} shipment
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
      {insightItem && <RecommendationInsightsDialog depotAvailableUnits={depotInventoryByProduct.get(insightItem.productId)} onClose={() => setInsightItem(null)} recommendation={insightItem} />}
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
      {undoItem && (
        <div
          className="modal-backdrop"
          onClick={(event) => {
            if (!statusUpdate) {
              closeFromBackdrop(event, () => setUndoItem(null));
            }
          }}
        >
          <section
            aria-labelledby="undo-dialog-title"
            aria-modal="true"
            className="override-dialog confirmation-dialog"
            role="dialog"
          >
            <header>
              <div>
                <span>Admin action</span>
                <h2 id="undo-dialog-title">Undo approval</h2>
              </div>
              <button
                aria-label="Close undo approval dialog"
                className="icon-button"
                onClick={() => setUndoItem(null)}
                type="button"
              >
                <X aria-hidden="true" size={17} />
              </button>
            </header>
            <p>
              Return the shipment of {formatNumber(undoItem.recommendedShipment)}
              {" "}units for {undoItem.storeCode} / {undoItem.productCode} to
              pending review? You can approve or edit it again afterward.
            </p>
            <footer>
              <button
                className="secondary-button"
                onClick={() => setUndoItem(null)}
                type="button"
              >
                Keep approved
              </button>
              <button
                className="save-button undo-confirm-button"
                disabled={
                  statusUpdate?.id === undoItem.id &&
                  statusUpdate?.status === "PENDING"
                }
                onClick={undoApproval}
                type="button"
              >
                <RotateCcw aria-hidden="true" size={13} />
                Undo approval
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}

import { useCallback, useEffect, useState } from "react";
import UploadDataDialog from "./components/UploadDataDialog.jsx";
import DashboardView from "./views/DashboardView.jsx";
import DepotInventoryView from "./views/DepotInventoryView.jsx";
import ReportsView from "./views/ReportsView.jsx";
import SettingsView from "./views/SettingsView.jsx";
import StoreInventoryView from "./views/StoreInventoryView.jsx";
import ShipmentsView from "./views/ShipmentsView.jsx";

function pageFromHash() {
  const routes = {
    "#inventory": "Depot Inventory",
    "#shipments": "Shipments",
    "#reports": "Reports",
    "#settings": "Settings",
    "#store-inventory": "Store Inventory",
  };

  return routes[globalThis.location.hash] ?? "Dashboard";
}

export default function App() {
  const [page, setPage] = useState(pageFromHash);
  const [collapsed, setCollapsed] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [dashboardRefresh, setDashboardRefresh] = useState(0);

  useEffect(() => {
    const followHash = () => {
      setPage(pageFromHash());
    };

    globalThis.addEventListener("hashchange", followHash);
    return () => globalThis.removeEventListener("hashchange", followHash);
  }, []);

  const navigate = useCallback((destination) => {
    const hashes = {
      "Depot Inventory": "inventory",
      Shipments: "shipments",
      Reports: "reports",
      Settings: "settings",
      "Store Inventory": "store-inventory",
    };

    globalThis.location.hash = hashes[destination] ?? "";
    setPage(destination);
  }, []);

  const handleAction = useCallback((action) => {
    if (action === "upload") {
      setUploadOpen(true);
    }
  }, []);

  const closeUpload = useCallback(() => setUploadOpen(false), []);
  const handleImported = useCallback(
    () => setDashboardRefresh((current) => current + 1),
    [],
  );

  const sharedProps = {
    collapsed,
    onCollapse: () => setCollapsed((current) => !current),
    onAction: handleAction,
    onNavigate: navigate,
  };

  return (
    <>
      {page === "Shipments" ? (
        <ShipmentsView {...sharedProps} />
      ) : page === "Depot Inventory" ? (
        <DepotInventoryView {...sharedProps} />
      ) : page === "Reports" ? (
        <ReportsView {...sharedProps} />
      ) : page === "Settings" ? (
        <SettingsView {...sharedProps} />
      ) : page === "Store Inventory" ? (
        <StoreInventoryView {...sharedProps} />
      ) : (
        <DashboardView
          {...sharedProps}
          refreshRequest={dashboardRefresh}
        />
      )}
      <UploadDataDialog
        onClose={closeUpload}
        onImported={handleImported}
        open={uploadOpen}
      />
    </>
  );
}

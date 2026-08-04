import { useCallback, useEffect, useState } from "react";
import UploadDataDialog from "./components/UploadDataDialog.jsx";
import DashboardView from "./views/DashboardView.jsx";
import ShipmentsView from "./views/ShipmentsView.jsx";

export default function App() {
  const [page, setPage] = useState(
    globalThis.location.hash === "#shipments" ? "Shipments" : "Dashboard",
  );
  const [collapsed, setCollapsed] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [dashboardRefresh, setDashboardRefresh] = useState(0);

  useEffect(() => {
    const followHash = () => {
      setPage(
        globalThis.location.hash === "#shipments" ? "Shipments" : "Dashboard",
      );
    };

    globalThis.addEventListener("hashchange", followHash);
    return () => globalThis.removeEventListener("hashchange", followHash);
  }, []);

  const navigate = useCallback((destination) => {
    globalThis.location.hash =
      destination === "Shipments" ? "shipments" : "";
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

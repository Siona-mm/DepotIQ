import { useCallback, useEffect, useState } from "react";
import UploadDataDialog from "./components/UploadDataDialog.jsx";
import DashboardView from "./views/DashboardView.jsx";
import DepotInventoryView from "./views/DepotInventoryView.jsx";
import ShipmentsView from "./views/ShipmentsView.jsx";
import StoresView from "./views/StoresView.jsx";
import ProductsView from "./views/ProductsView.jsx";

function pageFromHash() {
  const routes = {
    "#inventory": "Depot Inventory",
    "#shipments": "Shipments",
    "#stores": "Stores",
    "#products": "Products",
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
      Stores: "stores",
      Products: "products",
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
      ) : page === "Stores" ? (
        <StoresView {...sharedProps} />
      ) : page === "Products" ? (
        <ProductsView {...sharedProps} />
      ) : page === "Depot Inventory" ? (
        <DepotInventoryView {...sharedProps} />
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

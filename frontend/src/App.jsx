import { useCallback, useEffect, useState } from "react";
import { loadAuthenticatedUser, signIn, signOut } from "./api/depotiqApi.js";
import { permissionsFor } from "./auth/permissions.js";
import UploadDataDialog from "./components/UploadDataDialog.jsx";
import DashboardView from "./views/DashboardView.jsx";
import DepotInventoryView from "./views/DepotInventoryView.jsx";
import ForecastsView from "./views/ForecastsView.jsx";
import ProductsView from "./views/ProductsView.jsx";
import ReportsView from "./views/ReportsView.jsx";
import SettingsView from "./views/SettingsView.jsx";
import StoreInventoryView from "./views/StoreInventoryView.jsx";
import StoresView from "./views/StoresView.jsx";
import ShipmentsView from "./views/ShipmentsView.jsx";
import LoginView from "./views/LoginView.jsx";

function pageFromHash() {
  const routes = {
    "#inventory": "Depot Inventory",
    "#shipments": "Shipments",
    "#reports": "Reports",
    "#settings": "Settings",
    "#store-inventory": "Store Inventory",
    "#stores": "Stores",
    "#products": "Products",
    "#forecasts": "Forecasts",
  };

  return routes[globalThis.location.hash] ?? "Dashboard";
}

export default function App() {
  const [user, setUser] = useState(undefined);
  const [page, setPage] = useState(pageFromHash);
  const [collapsed, setCollapsed] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [dashboardRefresh, setDashboardRefresh] = useState(0);

  useEffect(() => {
    loadAuthenticatedUser()
      .then(setUser)
      .catch(() => {
        signOut();
        setUser(null);
      });
  }, []);

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
      Stores: "stores",
      Products: "products",
      Forecasts: "forecasts",
    };

    globalThis.location.hash = hashes[destination] ?? "";
    setPage(destination);
  }, []);

  const permissions = permissionsFor(user);

  const handleAction = useCallback((action) => {
    if (action === "upload" && permissions.canImportData) {
      setUploadOpen(true);
    }
  }, [permissions.canImportData]);

  const closeUpload = useCallback(() => setUploadOpen(false), []);
  const handleImported = useCallback(
    () => setDashboardRefresh((current) => current + 1),
    [],
  );

  const handleSignIn = useCallback(async (username, password) => {
    const authenticatedUser = await signIn(username, password);
    setUser(authenticatedUser);
  }, []);

  const handleSignOut = useCallback(() => {
    signOut();
    setUser(null);
  }, []);

  if (user === undefined) {
    return <main className="auth-loading">Checking your session...</main>;
  }

  if (!user) {
    return <LoginView onSignIn={handleSignIn} />;
  }

  const sharedProps = {
    collapsed,
    onCollapse: () => setCollapsed((current) => !current),
    onAction: handleAction,
    onNavigate: navigate,
    onSignOut: handleSignOut,
    permissions,
    user,
  };
  const activePage = ["Stores", "Products"].includes(page) && !permissions.canViewCatalog
    ? "Dashboard"
    : page;

  return (
    <>
      {activePage === "Shipments" ? (
        <ShipmentsView {...sharedProps} />
      ) : activePage === "Depot Inventory" ? (
        <DepotInventoryView {...sharedProps} />
      ) : activePage === "Reports" ? (
        <ReportsView {...sharedProps} />
      ) : activePage === "Settings" ? (
        <SettingsView {...sharedProps} />
      ) : activePage === "Store Inventory" ? (
        <StoreInventoryView {...sharedProps} />
      ) : activePage === "Stores" ? (
        <StoresView {...sharedProps} />
      ) : activePage === "Products" ? (
        <ProductsView {...sharedProps} />
      ) : activePage === "Forecasts" ? (
        <ForecastsView {...sharedProps} />
      ) : (
        <DashboardView
          {...sharedProps}
          refreshRequest={dashboardRefresh}
        />
      )}
      {permissions.canImportData && (
        <UploadDataDialog
          onClose={closeUpload}
          onImported={handleImported}
          open={uploadOpen}
        />
      )}
    </>
  );
}

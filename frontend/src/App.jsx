import { useCallback, useEffect, useRef, useState } from "react";
import { loadAuthenticatedUser, loadImportHistory, loadProfile, signIn, signOut } from "./api/depotiqApi.js";
import { permissionsFor } from "./auth/permissions.js";
import DashboardView from "./views/DashboardView.jsx";
import DepotInventoryView from "./views/DepotInventoryView.jsx";
import ForecastsView from "./views/ForecastsView.jsx";
import HistoryView from "./views/HistoryView.jsx";
import ProductsView from "./views/ProductsView.jsx";
import ReportsView from "./views/ReportsView.jsx";
import SettingsView from "./views/SettingsView.jsx";
import StoreInventoryView from "./views/StoreInventoryView.jsx";
import StoresView from "./views/StoresView.jsx";
import ShipmentsView from "./views/ShipmentsView.jsx";
import LoginView from "./views/LoginView.jsx";
import ProfileView from "./views/ProfileView.jsx";
import UploadDataView from "./views/UploadDataView.jsx";

function pageFromHash() {
  const routes = {
    "#inventory": "Depot Inventory",
    "#shipments": "Shipments",
    "#reports": "Reports",
    "#settings": "Settings",
    "#store-inventory": "Store Inventory",
    "#profile": "Profile",
    "#upload-data": "Upload Data",
    "#stores": "Stores",
    "#products": "Products",
    "#forecasts": "Forecasts",
    "#history": "History",
  };

  return routes[globalThis.location.hash] ?? "Dashboard";
}

export default function App() {
  const [user, setUser] = useState(undefined);
  const [profile, setProfile] = useState(null);
  const [page, setPage] = useState(pageFromHash);
  const [collapsed, setCollapsed] = useState(false);
  const [recentImportKeys, setRecentImportKeys] = useState([]);
  const [lastImport, setLastImport] = useState(null);
  const [operationalDataRevision, setOperationalDataRevision] = useState(0);
  const latestImportIdRef = useRef(null);

  useEffect(() => {
    loadAuthenticatedUser()
      .then(async (authenticatedUser) => {
        if (authenticatedUser) {
          const authenticatedProfile = await loadProfile();
          setProfile(authenticatedProfile);
        }
        setUser(authenticatedUser);
      })
      .catch(() => {
        signOut();
        setProfile(null);
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
      Profile: "profile",
      "Upload Data": "upload-data",
      Stores: "stores",
      Products: "products",
      Forecasts: "forecasts",
      History: "history",
    };

    globalThis.location.hash = hashes[destination] ?? "";
    setPage(destination);
  }, []);

  useEffect(() => {
    if (!user) return undefined;

    let cancelled = false;
    const refreshAfterAirflowImport = async () => {
      try {
        const [latestImport] = await loadImportHistory();
        if (!latestImport || cancelled) return;

        if (latestImportIdRef.current !== null && latestImportIdRef.current !== latestImport.id) {
          setLastImport(latestImport);
          setOperationalDataRevision((current) => current + 1);
        }
        latestImportIdRef.current = latestImport.id;
      } catch {
        // Import history is supplementary; an unavailable endpoint must not log the user out.
      }
    };

    refreshAfterAirflowImport();
    const intervalId = globalThis.setInterval(refreshAfterAirflowImport, 10_000);
    return () => {
      cancelled = true;
      globalThis.clearInterval(intervalId);
    };
  }, [user]);

  const permissions = permissionsFor(user);

  const handleAction = useCallback((action) => {
    if (action === "upload" && permissions.canImportData) {
      navigate("Upload Data");
    }
  }, [navigate, permissions.canImportData]);

  const handleSignIn = useCallback(async (username, password) => {
    const authenticatedUser = await signIn(username, password);
    const authenticatedProfile = await loadProfile();
    setProfile(authenticatedProfile);
    setUser(authenticatedUser);
  }, []);

  const handleSignOut = useCallback(() => {
    signOut();
    globalThis.location.hash = "";
    setProfile(null);
    setUser(null);
  }, []);

  const dismissImportedRow = useCallback((key) => {
    setRecentImportKeys((current) => current.filter((item) => item !== key));
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
    onProfileUpdated: setProfile,
    onUserUpdated: setUser,
    onSignOut: handleSignOut,
    permissions,
    profile,
    user,
    recentlyImportedKeys: recentImportKeys,
    onImportCompleted: setRecentImportKeys,
    onDismissImportedRow: dismissImportedRow,
  };

  const protectedPages = {
    Stores: permissions.canViewCatalog,
    Products: permissions.canViewCatalog,
    Forecasts: permissions.canViewForecasts,
    "Upload Data": permissions.canImportData,
  };
  const activePage = protectedPages[page] === false ? "Dashboard" : page;

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
      ) : activePage === "Profile" ? (
        <ProfileView {...sharedProps} />
      ) : activePage === "Upload Data" ? (
        <UploadDataView {...sharedProps} />
      ) : activePage === "Stores" ? (
        <StoresView {...sharedProps} />
      ) : activePage === "Products" ? (
        <ProductsView {...sharedProps} />
      ) : activePage === "Forecasts" ? (
        <ForecastsView {...sharedProps} />
      ) : activePage === "History" ? (
        <HistoryView {...sharedProps} />
      ) : (
        <DashboardView
          {...sharedProps}
          refreshRequest={operationalDataRevision}
        />
      )}
    </>
  );
}

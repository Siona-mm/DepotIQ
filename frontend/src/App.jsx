import { useCallback, useEffect, useState } from "react";
import { loadAuthenticatedUser, loadProfile, signIn, signOut } from "./api/depotiqApi.js";
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
  const dashboardRefresh = 0;

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

  const handleAction = useCallback((action) => {
    if (action === "upload") {
      navigate("Upload Data");
    }
  }, [navigate]);

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
    onSignOut: handleSignOut,
    profile,
    user,
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
      ) : page === "Profile" ? (
        <ProfileView {...sharedProps} />
      ) : page === "Upload Data" ? (
        <UploadDataView {...sharedProps} />
      ) : page === "Stores" ? (
        <StoresView {...sharedProps} />
      ) : page === "Products" ? (
        <ProductsView {...sharedProps} />
      ) : page === "Forecasts" ? (
        <ForecastsView {...sharedProps} />
      ) : page === "History" ? (
        <HistoryView {...sharedProps} />
      ) : (
        <DashboardView
          {...sharedProps}
          refreshRequest={dashboardRefresh}
        />
      )}
    </>
  );
}

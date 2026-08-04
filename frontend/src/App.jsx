import { useCallback, useEffect, useState } from "react";
import DashboardView from "./views/DashboardView.jsx";
import ShipmentsView from "./views/ShipmentsView.jsx";

export default function App() {
  const [page, setPage] = useState(
    globalThis.location.hash === "#shipments" ? "Shipments" : "Dashboard",
  );
  const [collapsed, setCollapsed] = useState(false);

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

  const sharedProps = {
    collapsed,
    onCollapse: () => setCollapsed((current) => !current),
    onNavigate: navigate,
  };

  return page === "Shipments" ? (
    <ShipmentsView {...sharedProps} />
  ) : (
    <DashboardView {...sharedProps} />
  );
}

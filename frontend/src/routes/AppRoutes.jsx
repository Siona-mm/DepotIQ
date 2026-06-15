import { BrowserRouter, Route, Routes } from "react-router-dom";
import { PageRoutes } from "../constants/PageRoutes.js";
import DashboardView from "../views/DashboardView.jsx";

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={PageRoutes.DASHBOARD} element={<DashboardView />} />
        <Route path="*" element={<DashboardView />} />
      </Routes>
    </BrowserRouter>
  );
}


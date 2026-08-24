import React from "react";
import ReactDOM from "react-dom/client";
import "@fontsource-variable/geist/wght.css";
import App from "./App.jsx";
import AppErrorBoundary from "./components/AppErrorBoundary.jsx";
import "./styles/main.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AppErrorBoundary>
      <App />
    </AppErrorBoundary>
  </React.StrictMode>
);

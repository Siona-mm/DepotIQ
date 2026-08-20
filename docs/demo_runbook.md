# DepotIQ Demo Runbook

## Goal

Show how DepotIQ turns historical sales, inventory, and demand forecasts into an explainable replenishment decision.

## Start the demo

1. Start PostgreSQL and the ML service if they are part of the local setup.
2. Run the backend from `backend` with `mvn spring-boot:run`.
3. Run the frontend from `frontend` with `npm.cmd run dev`.
4. Open `http://localhost:5173` and sign in as `admin` / `admin123`.

## Suggested 4-minute flow

1. On **Dashboard**, open **Demo guide** and introduce the workflow.
2. Show an urgent or high-priority recommendation. Explain the predicted demand, current store stock, depot stock, and recommended shipment.
3. Open **Forecasts** to show that recommendations are supported by demand predictions and confidence ranges.
4. Approve a recommendation, then open **Shipments** to demonstrate delivery planning and tracking.
5. Open **Reports** to show operational risk and forecast coverage.
6. If time permits, open **Upload Data** or the **Stores** and **Products** catalog to show data management.

## Role demonstration

- `admin` / `admin123`: full demo access, including imports and catalog management.
- `manager` / `manager123`: operational workflow without imports or settings edits.
- `viewer` / `viewer123`: read-only dashboard, inventory, shipments, reports, and forecasts.

## Demo reset

Use a fresh local database to restore the Flyway seed data. The seed dataset includes stores, products, inventory, 90 days of sales history, forecasts, and shipment recommendations so the workflow is visible immediately.

## Talking points

- DepotIQ reduces stockout risk by combining demand forecasts with live store and depot inventory.
- Recommendations are explainable: the user can see the demand, stock position, safety buffer, and priority behind each action.
- Roles keep operational controls limited to the right users while retaining visibility for decision-makers.

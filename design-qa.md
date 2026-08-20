# DepotIQ Workspace Design QA

## Source

- References: the supplied Dashboard, Store Inventory, Stores, Forecasts,
  Upload Data, Reports, Profile, and Settings screenshots.
- Comparison viewport: desktop at 1264 x 720.
- Design language: Geist, monochrome surfaces, compact controls, dense tables,
  and restrained 5-8px radii.

## Verification

- The authenticated profile is shared by every page and uses the saved display
  name, job title, initials, and profile photo.
- Profile is removed from the navigation list and remains available through the
  top avatar and sidebar account block.
- Store and product dialogs use compact multi-column layouts and remain inside
  the desktop viewport.
- Clicking outside store, product, inventory, recommendation, shipment, upload,
  and report dialogs dismisses them when no save is running.
- Store type labels are readable title case rather than database enum values.
- Store Inventory includes store and status filters, product/store names,
  available-soon stock, and plain-language stock states without the old
  `HEALTHY` pill.
- Stores, Products, Store Inventory, and Forecasts have consistent space between
  their metric cards and tables. All `shown` labels are removed.
- Report export opens a column selector and exports the current filtered rows
  using only the selected columns.
- Upload Data is a full-width workspace without the Expected Columns side card.
- Settings has no Restore Defaults action. Its database-backed Save Settings
  action is positioned after all settings sections.
- The collapse control stays anchored near the bottom in expanded and collapsed
  sidebar states.
- Dropdowns use consistent borders, radii, spacing, and chevrons.
- ESLint and the Vite production build pass.

## Interaction Checks

- Store dialog opens from Add Store and closes on backdrop click.
- Profile and Settings links remain reachable from the account surfaces.
- Settings toggles remain interactive and the save action remains enabled.
- The authenticated route guard remains unchanged; signing out clears the
  session and returns to the login screen.

## Result

final result: passed

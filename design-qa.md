# DepotIQ Dashboard Design QA

## Source

- Reference: supplied DepotIQ dashboard screenshot.
- Comparison viewport: desktop dashboard at approximately 1175 x 763.
- Responsive viewport: 390 x 844.

## Verification

- Layout matches the reference hierarchy: fixed sidebar, compact top bar, four
  metric cards, dense recommendation table, pagination, and right-side panels.
- Geist Variable is loaded locally and is the computed body font.
- The interface uses only the approved monochrome palette: `#FAFAFA`,
  `#FFFFFF`, `#111111`, `#6B7280`, `#9CA3AF`, `#E5E7EB`, and `#F3F4F6`.
- Search filters recommendations by store, product, category, and priority.
- Filter limits the table to urgent recommendations.
- Sort reverses recommended-shipment order.
- Settings triggers the existing ML synchronization flow.
- Desktop columns fit without page-level horizontal overflow.
- Mobile has no page-level horizontal overflow; the dense table scrolls inside
  its own panel.
- Lint and production build pass.

## Intentional Differences

- The empty reference panels contain live model-coverage and depot-inventory
  information.
- Dashboard values come from the Spring Boot API instead of static mock data.
- Priority labels remain monochrome to respect the approved color palette.

## Result

final result: passed

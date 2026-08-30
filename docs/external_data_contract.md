# DepotIQ External Data Contract

DepotIQ accepts batch data from existing point-of-sale, ERP, inventory, and
warehouse systems. CSV upload is the first supported connector; future API
connectors should use the same field meanings and validation rules.

## CSV Record

One row represents the sales and latest inventory snapshot for one product at
one store on one date.

### Required columns

| Column | Type | Rules |
| --- | --- | --- |
| Date | Date | `YYYY-MM-DD` |
| Store ID | String | Existing DepotIQ store code; import the store catalog first |
| Product ID | String | Existing DepotIQ product code; import the product catalog first |
| Category | String | Must not be blank |
| Region | String | Must not be blank |
| Inventory Level | Integer | Zero or greater |
| Units Sold | Integer | Zero or greater |
| Units Ordered | Integer | Zero or greater |
| Price | Decimal | Nonnegative, at most 2 decimal places |
| Discount | Decimal | 0 to 100, at most 2 decimal places |
| Weather Condition | String | Must not be blank |
| Holiday/Promotion | Boolean | `0`, `1`, `false`, or `true` |
| Seasonality | String | Must not be blank |

The original retail dataset may also contain `Demand Forecast` and
`Competitor Pricing`. DepotIQ accepts and ignores those columns because model
outputs must not be imported as training inputs.

### Optional integration columns

| Column | Purpose |
| --- | --- |
| Source System | Identifies the POS, ERP, or other source system |
| External Record ID | Permanent unique identifier assigned by that source |

If the optional fields are absent, DepotIQ uses `CSV_UPLOAD` and derives an
identifier from the store, product, and date.

## Example

```csv
Date,Store ID,Product ID,Category,Region,Inventory Level,Units Sold,Units Ordered,Demand Forecast,Price,Discount,Weather Condition,Holiday/Promotion,Competitor Pricing,Seasonality,Source System,External Record ID
2026-08-21,S001,P0001,Groceries,North,80,25,20,31.2,7.99,0,Clear,0,8.25,Summer,STORE_POS_DEMO,SALE-2026-1001
```

## Database behavior

- Sales values are inserted or updated in `sales_records`.
- Inventory and incoming quantities are updated in `store_inventory`.
- Store and product metadata are matched by their business codes.
- Every successful import is summarized in `import_audit_logs`.
- Missing headers, blank required values, malformed rows, and invalid values reject the entire file. No partial import is committed.
- Sales uploads never create placeholder stores/products or overwrite catalog details.
- Forecast and shipment recommendation tables are never populated directly
  from uploaded model-output columns.

## Duplicate handling

Sales are matched by the store-product-date combination. Source System and
External Record ID are stored as provenance; they do not replace that match key.

## Store and product catalogs

In **Upload Data**, select **Store catalog** or **Product catalog**. Catalog
imports match External Store ID or External SKU case-insensitively. DepotIQ
generates internal codes for new entries and preserves codes on updates. Use
those internal codes in sales and refill CSVs. Duplicate identifiers within a
catalog file are rejected.

Store columns (all required, including every cell):

```text
External Store ID,Name,Store Type,Region,Has Warehouse,Storage Capacity,Delivery Lead Time Days,Preferred Horizon Days
```

Store Type is Small, Medium, Large, or Warehouse Store. Capacity and lead time
must be positive integers. Preferred Horizon Days is 3, 7, 14, or 30.

Product columns (all required, including every cell):

```text
External SKU,Name,Category,Brand,Supplier Code,Unit Cost,Price,Weight Kg,Shelf Life Days,Perishable
```

Costs and prices must be nonnegative (at most 2 decimal places), weight must be
positive (at most 3 decimal places), and shelf life must be a nonnegative integer.
Perishable products require at least 1 shelf-life day. For nonperishable products
without a shelf-life limit, use 0. Boolean fields accept true/false, yes/no, or 1/0
in catalog uploads. Catalog uploads update details but never change inventory.

## Adding or refilling depot products

Select **Add or refill depot products**, enter a **Delivery reference (Receipt ID)**,
and upload a CSV containing the complete product columns plus `Units Received`.
The older `Initial Units` header is accepted as an alias. Quantities are positive
whole numbers and always mean units to **add**, not replacement stock totals.
Do not supply both quantity headers.

The same file can mix new and existing External SKUs. New products receive a
generated DepotIQ code. Existing products receive additional stock and updated
complete product details. Existing reserved units are preserved. No existing
stock causes the file to be rejected merely because the product is already stocked.

Use a unique delivery reference for each actual delivery. Retrying the same
reference/product/quantity skips it, including catalog updates. Changing its
quantity rejects the entire file. Earlier initial-stock imports did not record
receipt references; using a new reference now adds the specified units again.

API: POST multipart `file` and `receiptId` to `/api/imports/depot-products`.
An optional Receipt ID CSV column can identify receipts per row instead of the
multipart reference. This shares receipt duplicate protection with the refill
endpoint, so switching upload modes cannot count the same receipt twice.

## Refilling depot stock

Select **Refill depot stock**, or POST multipart field `file` to
`/api/imports/depot-refills`:

```csv
Receipt ID,Product ID,Units Received
DELIVERY-20260830-001,P0001,250
DELIVERY-20260830-001,P0002,100
```

All three columns and values are required. Product ID must be an existing DepotIQ
code. Alternatively supply an `External SKU` column to match supplier SKUs
case-insensitively; do not supply both identifier columns. Units Received must be a positive whole number. Received units are added to
physical stock atomically; existing reserved units are preserved. A catalog
product without a depot entry gets a new inventory entry with zero reservations.

A receipt ID identifies a delivery. Use one row per product per receipt. The same
receipt/product with the same quantity is skipped on re-upload; a different
quantity rejects the entire file. Receipt IDs are case-sensitive. Only assign a
new receipt ID for an actual new delivery. The audit history reports applied
receipt rows as updated and previously received rows as skipped. Receipt records
are retained to prevent duplicates, so referenced products cannot be deleted
while their receipt history exists.

## Templates and rollout

All five sample files are available in `frontend/public/` and `data/sample/` and
are linked from the upload page. Replace example identifiers and quantities with
real data. The sales/refill samples use seeded demo codes; newly imported catalogs
may have different internal codes. Old minimal product files now fail validation.
Existing incomplete records are not backfilled with invented values: upload
complete catalog details to correct them.

Rebuild and restart the backend and frontend once to apply this change. Flyway
migration V16 creates the receipt table on backend startup. The model trainer
does not need to run for this update:

```sh
docker compose up -d --build --no-deps backend frontend
```

The database and ML services should already be running for that command. All CSV
imports remain administrator-only.

## Business code consistency

Store codes use `S` plus at least three digits (`S001`, `S011`, `S1000`). Product
codes use `P` plus at least four (`P0001`, `P0109`). Codes expand naturally beyond
those widths; sequence gaps are allowed and never filled by renumbering records.

Migration V17 converts legacy padding such as `S0011` to `S011` while preserving
store IDs, external identifiers, names, stock, sales, and shipment relationships.
It refuses ambiguous numeric identities instead of merging distinct stores. Sales
CSV and model-result imports accept the legacy padding and resolve it to the
canonical store. Both code counters advance past previously imported codes, and
the generator skips occupied values if a counter later falls behind.

The store form accepts the same 3/7/14/30-day horizons as CSV imports. External
Store ID and External SKU are optional for manual entry; they connect records to
an external catalog and are not operating details. Provided identifiers must be
unique and at most 100 characters. Catalog CSV uploads still require them for
matching rows. Existing records without these identifiers are not marked
incomplete for that reason.

## Manual catalog entry

Add and edit forms require store operating values and product name, category,
brand, supplier, unit cost, price, weight, shelf life, and explicit warehouse/
perishable choices. Blank required values are rejected by both the form and API,
not converted into zero. Only internal store/product codes are generated
automatically. Rows with missing or invalid operating/catalog values list the
specific fields to check instead of showing a generic completeness badge.

Currency supports two decimal places and weight three; weight must be positive.
Shelf life is a whole number of days, at least one for perishable products.
Non-perishable products may explicitly use zero. Store capacities and lead times
must be positive whole numbers. The API rejects fractional quantities and day
counts instead of truncating them. Depot reserved units cannot exceed available
units. Duplicate external SKUs are rejected without changing existing records.

Creating or editing catalog details does not change stock. Use depot receipt
uploads to add stock, or inventory forms to correct stock counts. Existing
incomplete records remain intact until their real details are supplied through
Edit or a complete catalog CSV.

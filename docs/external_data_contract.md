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
| Store ID | String | Existing or externally supplied store code |
| Product ID | String | Existing or externally supplied product code |
| Category | String | Must not be blank |
| Region | String | Store region |
| Inventory Level | Integer | Zero or greater |
| Units Sold | Integer | Zero or greater |
| Units Ordered | Integer | Zero or greater |
| Price | Decimal | Valid numeric value |
| Discount | Decimal | Valid numeric value |
| Weather Condition | String | May be blank |
| Holiday/Promotion | Boolean | `0`, `1`, `false`, or `true` |
| Seasonality | String | May be blank |

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
- Every file import is summarized in `import_audit_logs`.
- Forecast and shipment recommendation tables are never populated directly
  from uploaded model-output columns.

## Duplicate handling

When `Source System` and `External Record ID` are supplied, that pair uniquely
identifies the source record. Re-importing it updates the existing sales row.
Without external identifiers, the store-product-date combination is used.

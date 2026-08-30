const productColumns = ["External SKU", "Name", "Category", "Brand", "Supplier Code", "Unit Cost", "Price", "Weight Kg", "Shelf Life Days", "Perishable"];

export const importTypes = {
  "sales-records": {
    label: "Sales and store inventory",
    columns: ["Date", "Store ID", "Product ID", "Category", "Region", "Inventory Level", "Units Sold", "Units Ordered", "Price", "Discount", "Weather Condition", "Holiday/Promotion", "Seasonality"],
    template: "sample_sales_inventory_import.csv",
    help: "Use existing DepotIQ Store IDs and Product IDs from the catalog. Add new stores and products using the catalog uploads first. Dates use YYYY-MM-DD; quantities are whole numbers; discount is 0 to 100; booleans use true/false or 1/0.",
    outcome: "Saves sales history and the latest store inventory snapshot. Requests updated forecasts and shipment recommendations without retraining the model.",
    destination: "store-inventory", destinationLabel: "View store inventory",
  },
  stores: {
    label: "Store catalog",
    columns: ["External Store ID", "Name", "Store Type", "Region", "Has Warehouse", "Storage Capacity", "Delivery Lead Time Days", "Preferred Horizon Days"],
    template: "sample_store_catalog.csv",
    help: "Matches External Store ID and generates a DepotIQ store code for new stores. Store Type: Small, Medium, Large, or Warehouse Store. Capacity and lead time must be positive; planning horizon must be 3, 7, 14, or 30 days.",
    outcome: "Adds or updates complete store details. Existing internal store codes stay unchanged.",
    destination: "stores", destinationLabel: "View stores",
  },
  products: {
    label: "Product catalog",
    columns: productColumns,
    template: "sample_product_catalog.csv",
    help: "Matches External SKU and generates a DepotIQ product code for new products. All product details are required. Weight must be positive. Nonperishable products may use 0 shelf-life days; perishable products need at least 1.",
    outcome: "Adds or updates complete product details without changing stock quantities.",
    destination: "products", destinationLabel: "View products",
  },
  "depot-products": {
    label: "Add or refill depot products",
    columns: [...productColumns, "Units Received"],
    columnAliases: { "Units Received": ["Initial Units"] },
    template: "sample_depot_products.csv",
    help: "Mix new and existing products in one file. External SKU matches existing products; units received are added to their stock. The older Initial Units column is also accepted and means units to ADD, not a replacement total. Quantities must be positive whole numbers. Supply complete product details.",
    outcome: "Creates new products or refills existing depot stock, preserving reservations. Use a different delivery reference for each new delivery; retrying the same reference and quantity skips stock already received. Earlier initial-stock imports had no receipt reference: using a new reference now will ADD the quantity again.",
    destination: "inventory", destinationLabel: "View depot inventory",
  },
  "depot-refills": {
    label: "Refill depot stock",
    columns: ["Receipt ID", "Product ID", "Units Received"],
    columnAliases: { "Product ID": ["External SKU"] },
    template: "sample_depot_refill.csv",
    help: "Use a Product ID column with DepotIQ codes, or an External SKU column with your supplier SKUs, and a positive whole number of units received. Give each delivery a unique Receipt ID. One receipt can contain several products, with one row per product. Receipt IDs are case-sensitive.",
    outcome: "Adds received units to physical depot stock and preserves reservations. Re-uploading the same receipt and product skips it; changing its quantity rejects the file. Use a new receipt ID only for a new delivery.",
    destination: "inventory", destinationLabel: "View depot inventory",
  },
};

export function parseCsv(text) {
  const rows = [];
  let row = [];
  let field = "";
  let quoted = false;
  let closedQuote = false;
  const input = text.replace(/^\uFEFF/, "");
  for (let index = 0; index < input.length; index += 1) {
    const character = input[index];
    if (quoted) {
      if (character === '"' && input[index + 1] === '"') { field += '"'; index += 1; }
      else if (character === '"') { quoted = false; closedQuote = true; }
      else field += character;
    } else if (character === "," || character === "\n" || character === "\r") {
      row.push(field.trim()); field = ""; closedQuote = false;
      if (character !== ",") {
        if (character === "\r" && input[index + 1] === "\n") index += 1;
        if (row.length > 1 || row[0]) rows.push(row);
        row = [];
      }
    } else if (character === '"' && !field && !closedQuote) quoted = true;
    else {
      if (closedQuote && character.trim()) throw new Error("Invalid CSV quoting. Check the characters after a closing quote.");
      if (character === '"') throw new Error("Invalid CSV quoting. Quote the entire field and double any quotes inside it.");
      field += character;
    }
  }
  if (quoted) throw new Error("Invalid CSV: a quoted field is not closed.");
  if (field || row.length || closedQuote) { row.push(field.trim()); rows.push(row); }
  return rows;
}

export function validateCsv(text, columns, aliases = {}) {
  const [header = [], ...rows] = parseCsv(text);
  const normalized = header.map((column) => {
    const canonical = Object.entries(aliases).find(([, alternatives]) => alternatives.some((alias) => alias.toLowerCase() === column.toLowerCase()));
    return (canonical?.[0] ?? column).toLowerCase();
  });
  if (header.some((column) => !column) || new Set(normalized).size !== header.length) {
    throw new Error("Column headers must be non-empty and unique.");
  }
  const missing = columns.filter((column) => !normalized.includes(column.toLowerCase()));
  if (missing.length) throw new Error(`Missing required columns: ${missing.join(", ")}`);
  if (!rows.length) throw new Error("The CSV must contain at least one data row.");
  for (const [index, row] of rows.entries()) {
    if (row.length !== header.length) throw new Error(`Row ${index + 2}: Column count does not match the header.`);
    const blank = columns.filter((column) => !row[normalized.indexOf(column.toLowerCase())]);
    if (blank.length) throw new Error(`Row ${index + 2}: Required values are blank: ${blank.join(", ")}. Nothing was imported.`);
  }
  return rows.length;
}

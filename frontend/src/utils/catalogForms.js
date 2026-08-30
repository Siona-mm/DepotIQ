export const EMPTY_STORE = {
  name: "", externalStoreId: "", storeType: "", region: "", hasWarehouse: "",
  storageCapacity: "", deliveryLeadTimeDays: "", preferredHorizonDays: "",
};

export const EMPTY_PRODUCT = {
  name: "", category: "", brand: "", supplierCode: "", externalSku: "",
  unitCost: "", price: "", weightKg: "", shelfLifeDays: "", perishable: "",
};

export const PRODUCT_FIELDS = [
  { key: "name", label: "Name", maxLength: 150 },
  { key: "category", label: "Category", maxLength: 100 },
  { key: "brand", label: "Brand", maxLength: 100 },
  { key: "supplierCode", label: "Supplier code", maxLength: 100 },
  { key: "externalSku", label: "External SKU / barcode (optional)", maxLength: 100, optional: true },
  { key: "unitCost", label: "Unit cost", type: "number", min: 0, max: 9999999999.99, step: "0.01" },
  { key: "price", label: "Price", type: "number", min: 0, max: 9999999999.99, step: "0.01" },
  { key: "weightKg", label: "Weight (kg)", type: "number", min: 0.001, max: 9999999.999, step: "0.001" },
  { key: "shelfLifeDays", label: "Shelf life (days)", type: "number", min: 0, max: 2147483647, step: "1" },
];

export function catalogFormValues(empty, record = {}) {
  return Object.fromEntries(Object.keys(empty).map((key) => [key, record[key] == null ? "" : String(record[key])]));
}

function requiredText(value, label, maxLength) {
  const text = String(value ?? "").trim();
  if (!text) throw new Error(`${label} is required.`);
  if (text.length > maxLength) throw new Error(`${label} must be ${maxLength} characters or fewer.`);
  return text;
}

function optionalText(value, label, maxLength) {
  const text = String(value ?? "").trim();
  if (text.length > maxLength) throw new Error(`${label} must be ${maxLength} characters or fewer.`);
  return text;
}

function requiredNumber(value, label, min, max, decimals = 0) {
  const text = String(value ?? "").trim();
  if (!text) throw new Error(`${label} is required.`);
  const pattern = decimals ? new RegExp(`^\\d+(?:\\.\\d{1,${decimals}})?$`) : /^\d+$/;
  if (!pattern.test(text)) throw new Error(`${label} must be ${decimals ? `a number with up to ${decimals} decimal places` : "a whole number"}.`);
  const number = Number(text);
  if (!Number.isFinite(number) || number < min || number > max) throw new Error(`${label} must be between ${min} and ${max}.`);
  return number;
}

function requiredBoolean(value, label) {
  if (value === true || value === "true") return true;
  if (value === false || value === "false") return false;
  throw new Error(`Choose Yes or No for ${label}.`);
}

export function buildStorePayload(form) {
  const payload = {
    externalStoreId: optionalText(form.externalStoreId, "External store ID", 100),
    name: requiredText(form.name, "Name", 150),
    storeType: requiredText(form.storeType, "Store type", 100),
    region: requiredText(form.region, "Region", 100),
    hasWarehouse: requiredBoolean(form.hasWarehouse, "Has warehouse"),
    storageCapacity: requiredNumber(form.storageCapacity, "Storage capacity", 1, 2147483647),
    deliveryLeadTimeDays: requiredNumber(form.deliveryLeadTimeDays, "Delivery lead time", 1, 2147483647),
    preferredHorizonDays: requiredNumber(form.preferredHorizonDays, "Preferred horizon", 1, 30),
  };
  if (!["SMALL", "MEDIUM", "LARGE", "WAREHOUSE_STORE"].includes(payload.storeType)) throw new Error("Select a valid store type.");
  if (![3, 7, 14, 30].includes(payload.preferredHorizonDays)) throw new Error("Preferred horizon must be 3, 7, 14, or 30 days.");
  return payload;
}

export function buildProductPayload(form) {
  const perishable = requiredBoolean(form.perishable, "Perishable product");
  return {
    name: requiredText(form.name, "Name", 150),
    category: requiredText(form.category, "Category", 100),
    brand: requiredText(form.brand, "Brand", 100),
    supplierCode: requiredText(form.supplierCode, "Supplier code", 100),
    externalSku: optionalText(form.externalSku, "External SKU / barcode", 100),
    unitCost: requiredNumber(form.unitCost, "Unit cost", 0, 9999999999.99, 2),
    price: requiredNumber(form.price, "Price", 0, 9999999999.99, 2),
    weightKg: requiredNumber(form.weightKg, "Weight (kg)", 0.001, 9999999.999, 3),
    shelfLifeDays: requiredNumber(form.shelfLifeDays, "Shelf life (days)", perishable ? 1 : 0, 2147483647),
    perishable,
  };
}

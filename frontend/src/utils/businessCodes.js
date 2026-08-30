const codeCollator = new Intl.Collator("en", { numeric: true, sensitivity: "base" });

export const compareBusinessCodes = (left, right) => codeCollator.compare(String(left ?? ""), String(right ?? ""));

const hasText = (value) => typeof value === "string" && value.trim().length > 0;
const isBoolean = (value) => value === true || value === false;
function isNumberInRange(value, min, max, whole = false) {
  if (value == null || String(value).trim() === "" || typeof value === "boolean") return false;
  const number = Number(value);
  return Number.isFinite(number) && number >= min && number <= max && (!whole || Number.isInteger(number));
}

// Integration identifiers are separate from operating/catalog completeness.
// List actionable fields instead of applying form submission rules to old rows.
export function storeDetailIssues(store) {
  const issues = [];
  if (!hasText(store.name) || /^Imported Store /i.test(store.name.trim())) issues.push("store name");
  if (!["SMALL", "MEDIUM", "LARGE", "WAREHOUSE_STORE"].includes(store.storeType)) issues.push("store type");
  if (!hasText(store.region)) issues.push("region");
  if (!isBoolean(store.hasWarehouse)) issues.push("warehouse status");
  if (!isNumberInRange(store.storageCapacity, 1, 2147483647, true)) issues.push("storage capacity");
  if (!isNumberInRange(store.deliveryLeadTimeDays, 1, 2147483647, true)) issues.push("delivery lead time");
  if (![3, 7, 14, 30].includes(Number(store.preferredHorizonDays))) issues.push("planning horizon");
  return issues;
}

export function productDetailIssues(product) {
  const issues = [];
  if (!hasText(product.name) || /^Imported Product /i.test(product.name.trim())) issues.push("product name");
  for (const [field, label] of [["category", "category"], ["brand", "brand"], ["supplierCode", "supplier code"]]) {
    if (!hasText(product[field])) issues.push(label);
  }
  if (!isNumberInRange(product.unitCost, 0, 9999999999.99)) issues.push("unit cost");
  if (!isNumberInRange(product.price, 0, 9999999999.99)) issues.push("price");
  if (!isNumberInRange(product.weightKg, 0.001, 9999999.999)) issues.push("weight");
  if (!isNumberInRange(product.shelfLifeDays, product.perishable === true ? 1 : 0, 2147483647, true)) issues.push("shelf life");
  if (!isBoolean(product.perishable)) issues.push("perishable status");
  return issues;
}

export const storeNeedsDetails = (store) => storeDetailIssues(store).length > 0;
export const productNeedsDetails = (product) => productDetailIssues(product).length > 0;

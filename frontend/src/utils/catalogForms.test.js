import assert from "node:assert/strict";
import test from "node:test";
import { buildProductPayload, buildStorePayload, catalogFormValues, EMPTY_PRODUCT, EMPTY_STORE } from "./catalogForms.js";
import { productNeedsDetails } from "./businessCodes.js";

const product = { name: " Rice ", category: "Food", brand: "Grain Co", supplierCode: "SUP-1", externalSku: " RICE-5KG ", unitCost: "3.25", price: "5.95", weightKg: "5.001", shelfLifeDays: "90", perishable: "true" };
const store = { name: " North Market ", externalStoreId: " POS-NORTH ", storeType: "SMALL", region: "North", hasWarehouse: "false", storageCapacity: "1200", deliveryLeadTimeDays: "2", preferredHorizonDays: "7" };

test("new forms require explicit values instead of invented defaults", () => {
  assert.ok(Object.values(EMPTY_STORE).every((value) => value === ""));
  assert.ok(Object.values(EMPTY_PRODUCT).every((value) => value === ""));
});

test("blank, null, omitted and whitespace details cannot become zero or false", () => {
  for (const [complete, build] of [[product, buildProductPayload], [store, buildStorePayload]]) {
    for (const key of Object.keys(complete).filter((key) => !["externalStoreId", "externalSku"].includes(key))) {
      for (const empty of ["", "  ", null, undefined]) assert.throws(() => build({ ...complete, [key]: empty }), undefined, `Reject empty ${key}`);
    }
  }
});

test("payloads preserve every entered detail, trim text and exclude generated fields", () => {
  assert.deepEqual(buildProductPayload({ ...product, id: 1, productCode: "P0041" }), {
    name: "Rice", category: "Food", brand: "Grain Co", supplierCode: "SUP-1", externalSku: "RICE-5KG",
    unitCost: 3.25, price: 5.95, weightKg: 5.001, shelfLifeDays: 90, perishable: true,
  });
  assert.deepEqual(buildStorePayload({ ...store, id: 1, storeCode: "S011" }), {
    name: "North Market", externalStoreId: "POS-NORTH", storeType: "SMALL", region: "North", hasWarehouse: false,
    storageCapacity: 1200, deliveryLeadTimeDays: 2, preferredHorizonDays: 7,
  });
});

test("explicit zero costs and non-perishable shelf life are valid, positive weight is required", () => {
  const payload = buildProductPayload({ ...product, unitCost: "0", price: "0", shelfLifeDays: "0", perishable: "false" });
  assert.equal(payload.unitCost, 0); assert.equal(payload.perishable, false);
  assert.throws(() => buildProductPayload({ ...product, shelfLifeDays: "0" }), /Shelf life/);
  assert.throws(() => buildProductPayload({ ...product, weightKg: "0" }), /Weight/);
});

test("rejects fractions, overflow and invalid precision before posting", () => {
  for (const partial of [{ shelfLifeDays: "1.5" }, { shelfLifeDays: "2147483648" }, { unitCost: "1.001" }, { price: "-1" }, { weightKg: "0.0001" }, { unitCost: "10000000000" }, { brand: "x".repeat(101) }]) {
    assert.throws(() => buildProductPayload({ ...product, ...partial }));
  }
  for (const partial of [{ storageCapacity: "1.5" }, { storageCapacity: "0" }, { deliveryLeadTimeDays: "2147483648" }, { preferredHorizonDays: "2" }, { storeType: "OTHER" }]) {
    assert.throws(() => buildStorePayload({ ...store, ...partial }));
  }
});

test("editing keeps actual false and zero values but leaves missing fields empty", () => {
  const form = catalogFormValues(EMPTY_PRODUCT, { ...buildProductPayload(product), perishable: false, shelfLifeDays: 0, unitCost: null, brand: null });
  assert.equal(form.perishable, "false"); assert.equal(form.shelfLifeDays, "0");
  assert.equal(form.unitCost, ""); assert.equal(form.brand, "");
});

test("incomplete product badges distinguish missing data from valid zero values", () => {
  const complete = buildProductPayload({ ...product, perishable: "false", shelfLifeDays: "0", price: "0" });
  assert.equal(productNeedsDetails(complete), false);
  for (const partial of [{ price: null }, { brand: "" }, { weightKg: 0 }, { perishable: true }]) {
    assert.equal(productNeedsDetails({ ...complete, ...partial }), true);
  }
});

test("manual forms can create and edit without an external catalog connection", () => {
  for (const empty of [undefined, null, "", "  "]) {
    assert.equal(buildStorePayload({ ...store, externalStoreId: empty }).externalStoreId, "");
    assert.equal(buildProductPayload({ ...product, externalSku: empty }).externalSku, "");
  }
  assert.throws(() => buildStorePayload({ ...store, externalStoreId: "x".repeat(101) }), /External store ID/);
  assert.throws(() => buildProductPayload({ ...product, externalSku: "x".repeat(101) }), /External SKU/);
});

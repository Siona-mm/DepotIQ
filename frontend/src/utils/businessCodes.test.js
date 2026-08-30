import assert from "node:assert/strict";
import test from "node:test";
import { compareBusinessCodes, storeDetailIssues, storeNeedsDetails, productDetailIssues, productNeedsDetails } from "./businessCodes.js";

test("sorts store and product codes by their numeric value", () => {
  assert.deepEqual(["S017", "S0011", "S002", "S1000", "S999"].sort(compareBusinessCodes), ["S002", "S0011", "S017", "S999", "S1000"]);
  assert.deepEqual(["P0109", "P0041", "P0002"].sort(compareBusinessCodes), ["P0002", "P0041", "P0109"]);
});

test("flags legacy placeholders and missing operating inputs without inventing data", () => {
  const complete = {name: "North Market", externalStoreId: "POS-NORTH", storeType: "MEDIUM", hasWarehouse: false, region: "North", storageCapacity: 1200, deliveryLeadTimeDays: 2, preferredHorizonDays: 7};
  assert.equal(storeNeedsDetails(complete), false);
  for (const partial of [{name: "Imported Store S012"}, {storeType: null}, {hasWarehouse: null}, {region: " "}, {storageCapacity: 0}, {deliveryLeadTimeDays: 0}, {preferredHorizonDays: 2}]) {
    assert.equal(storeNeedsDetails({...complete, ...partial}), true);
  }
});

test("complete legacy stores need no external integration ID", () => {
  const downtown = { name: "Downtown Small Store", storeType: "SMALL", region: "North", hasWarehouse: false, storageCapacity: 500, deliveryLeadTimeDays: 2, preferredHorizonDays: 3 };
  for (const externalStoreId of [undefined, null, "", "POS-1"]) {
    assert.deepEqual(storeDetailIssues({ ...downtown, externalStoreId }), []);
    assert.equal(storeNeedsDetails({ ...downtown, externalStoreId }), false);
  }
  assert.deepEqual(storeDetailIssues({ ...downtown, name: "Imported Store S012", storageCapacity: 0, deliveryLeadTimeDays: 0 }), ["store name", "storage capacity", "delivery lead time"]);
});

test("product warnings identify missing metadata and do not demand an external SKU", () => {
  const product = { name: "Rice", category: "Food", brand: "Grain Co", supplierCode: "SUP-1", unitCost: 0, price: 0, weightKg: 5, shelfLifeDays: 0, perishable: false };
  assert.deepEqual(productDetailIssues(product), []);
  assert.equal(productNeedsDetails({ ...product, externalSku: null }), false);
  assert.deepEqual(productDetailIssues({ ...product, brand: " ", unitCost: null, perishable: true }), ["brand", "unit cost", "shelf life"]);
  assert.deepEqual(productDetailIssues({ ...product, price: "", perishable: null }), ["price", "perishable status"]);
});

import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import { URL } from "node:url";
import { importTypes, parseCsv, validateCsv } from "./csvImport.js";

for (const [type, config] of Object.entries(importTypes)) {
  const sample = readFileSync(new URL(`../../public/${config.template}`, import.meta.url), "utf8");
  test(`${type}: sample has all required values`, () => {
    assert.ok(validateCsv(sample, config.columns) > 0);
  });
  test(`${type}: rejects each missing column and blank required value`, () => {
    const [header, values] = parseCsv(sample);
    for (const required of config.columns) {
      const index = header.indexOf(required);
      assert.throws(() => validateCsv(header.filter((_, i) => i !== index).join(","), config.columns), /Missing required columns/);
      const blank = [...values]; blank[index] = "   ";
      assert.throws(() => validateCsv(`${header.join(",")}\n${blank.join(",")}`, config.columns), /Required values are blank/);
    }
  });
}

test("handles BOM, CRLF, quoted commas, escaped quotes, and embedded newlines", () => {
  assert.deepEqual(parseCsv('\uFEFFName,Value\r\n"Milk, fresh","A ""quoted""\nvalue"\r\n'), [["Name", "Value"], ["Milk, fresh", 'A "quoted"\nvalue']]);
});

test("rejects malformed quoting, duplicate headers, uneven rows, and header-only files", () => {
  assert.throws(() => validateCsv('Name\n"unfinished', ["Name"]), /not closed/);
  assert.throws(() => validateCsv('Name\n"closed"bad', ["Name"]), /quoting/);
  assert.throws(() => validateCsv("Name,name\nA,B", ["Name"]), /unique/);
  assert.throws(() => validateCsv("Name,Value\nA", ["Name"]), /Column count/);
  assert.throws(() => validateCsv("Name\n", ["Name"]), /data row/);
});


test("combined depot upload accepts the existing Initial Units files", () => {
  const config = importTypes["depot-products"];
  const sample = readFileSync(new URL(`../../public/${config.template}`, import.meta.url), "utf8");
  assert.ok(validateCsv(sample.replace("Units Received", "Initial Units"), config.columns, config.columnAliases) > 0);
  assert.throws(() => validateCsv(sample.replace("Units Received", "Initial Units").replace(",500", ", "), config.columns, config.columnAliases), /Required values are blank/);
});

test("refill accepts External SKU and rejects ambiguous duplicate identifier columns", () => {
  const config = importTypes["depot-refills"];
  assert.equal(validateCsv("Receipt ID,External SKU,Units Received\nR1,SKU-RICE,10", config.columns, config.columnAliases), 1);
  assert.throws(() => validateCsv("Receipt ID,Product ID,External SKU,Units Received\nR1,P1,SKU-RICE,10", config.columns, config.columnAliases), /unique/);
});

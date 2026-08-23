import { Download, FileSpreadsheet, FileUp, Upload, X } from "lucide-react";
import { useState } from "react";
import { importHistoricalSalesCsv } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import UserAvatar from "../components/UserAvatar.jsx";

function importedInventoryKeys(csvText) {
  const [headerLine, ...rows] = csvText.trim().split(/\r?\n/);
  const headers = headerLine.split(",").map((header) => header.trim());
  const storeIndex = headers.indexOf("Store ID");
  const productIndex = headers.indexOf("Product ID");

  if (storeIndex < 0 || productIndex < 0) return [];

  return [...new Set(rows.map((row) => {
    const values = row.split(",");
    return `${values[storeIndex]?.trim()}::${values[productIndex]?.trim()}`;
  }).filter((key) => !key.includes("undefined") && key !== "::"))];
}

export default function UploadDataView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user, onImportCompleted }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const selectFile = (event) => { setFile(event.target.files?.[0] ?? null); setError(""); setResult(null); };
  const submit = async (event) => { event.preventDefault(); if (!file) return; setUploading(true); setError(""); setResult(null); try { const response = await importHistoricalSalesCsv(file); onImportCompleted?.({ keys: importedInventoryKeys(await file.text()), result: response }); setResult(response); } catch (requestError) { setError(requestError.message); } finally { setUploading(false); } };
  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}><AppSidebar activePage="Upload Data" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} profile={profile} user={user} />
    <main className="dashboard upload-page"><header className="topbar"><h1>Upload Data</h1><div /><UserAvatar onClick={() => onNavigate("Profile")} profile={profile} user={user} /></header><div className="page-heading"><div><span>Historical data</span><h2>Import store sales records</h2></div></div>
      {(error || result) && <div className={error ? "notice error" : "notice"}>{error || <><span>{`Import complete: ${result.processedRows} processed, ${result.createdRecords} created, ${result.updatedRecords} updated, ${result.skippedRows} skipped. Forecasts and shipment recommendations are refreshing with the latest data.`}</span><span className="notice-actions"><a className="notice-action" href="#store-inventory">View imported inventory</a><a className="notice-action" href="#shipments">View shipment recommendations</a></span></>}</div>}
      <section className="upload-page-grid"><form className="upload-page-card" onSubmit={submit}><header><FileUp size={20} /><div><h2>Sales and inventory CSV</h2><p>Choose a CSV export from your store, POS, or warehouse system.</p></div></header><div className="upload-body"><label className={file ? "upload-dropzone file-selected" : "upload-dropzone"}><FileSpreadsheet size={30} /><strong>{file ? "CSV ready to import" : "Choose a CSV file"}</strong>{file ? <div className="selected-file-details"><span><b>Name</b>{file.name}</span><span><b>Type</b>{file.type || "text/csv"}</span><span><b>Size</b>{Math.ceil(file.size / 1024)} KB</span></div> : <span>CSV files up to 50 MB</span>}<span className="file-picker-button">{file ? "Choose another file" : "Browse files"}</span><input accept=".csv,text/csv" disabled={uploading} onChange={selectFile} type="file" /></label><div className="upload-test-file"><span>Want to test it first?</span><a download href="/sample_sales_inventory_import.csv"><Download size={14} />Download sample CSV</a></div>{file && <button className="clear-file-button" onClick={() => setFile(null)} type="button"><X size={14} />Remove selected file</button>}</div><footer><button className="primary-button" disabled={!file || uploading} type="submit"><Upload size={16} />{uploading ? "Importing data and refreshing plans..." : "Import data"}</button></footer></form><aside className="upload-summary"><span>Import outcome</span><h2>What updates next</h2><ul><li><strong>Sales history</strong><small>New rows are saved against each store and product.</small></li><li><strong>Store inventory</strong><small>The latest stock and incoming units replace the current snapshot.</small></li><li><strong>Forecasts and shipments</strong><small>The latest data is sent to the running forecasting service to refresh demand plans and shipment recommendations. The model is not retrained.</small></li><li><strong>Review state</strong><small>Imported rows appear as New in Store Inventory until reviewed.</small></li></ul></aside></section>
    </main></div>;
}

import { Download, FileSpreadsheet, FileUp, Upload, X } from "lucide-react";
import { useState } from "react";
import { importCsv } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";
import { importTypes, validateCsv } from "../utils/csvImport.js";

export default function UploadDataView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user, onImportCompleted }) {
  const [type, setType] = useState("sales-records");
  const [file, setFile] = useState(null);
  const [receiptId, setReceiptId] = useState("");
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [inputKey, setInputKey] = useState(0);
  const config = importTypes[type];

  const resetFile = () => {
    setFile(null); setError(""); setResult(null); setInputKey((current) => current + 1);
  };
  const selectFile = (event) => {
    const selected = event.target.files?.[0] ?? null;
    setError(""); setResult(null); setFile(null);
    if (!selected) return;
    if (!selected.name.toLowerCase().endsWith(".csv")) { setError("Only CSV files are supported."); return; }
    if (selected.size > 50 * 1024 * 1024) { setError("The CSV file exceeds the maximum upload size of 50 MB."); return; }
    setFile(selected);
  };
  const submit = async (event) => {
    event.preventDefault();
    if (!file || uploading) return;
    setUploading(true); setError(""); setResult(null);
    try {
      validateCsv(await file.text(), config.columns, config.columnAliases);
      const response = await importCsv(file, type, type === "depot-products" ? receiptId : "");
      setResult(response);
      setFile(null);
      setReceiptId("");
      setInputKey((current) => current + 1);
      onImportCompleted?.({ keys: response.importedInventoryKeys ?? [], result: response });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar activePage="Upload Data" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} profile={profile} user={user} />
      <main className="dashboard upload-page">
        <header className="topbar"><h1>Upload Data</h1><div /><HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} /></header>
        <div className="page-heading"><div><span>Catalog and inventory</span><h2>Import complete data or receive stock</h2></div></div>
        {error && <div className="notice error csv-import-error" role="alert">{error}</div>}
        {result && <div className="notice" role="status">
          <div>
            <strong>{type === "depot-refills" ? "Stock receipt import complete" : "Import complete"}</strong>
            <p>{type === "depot-refills"
              ? `${result.updatedRecords} receipt rows applied; ${result.skippedRows} already received and skipped.`
              : `${result.processedRows} rows processed; ${result.createdRecords} created; ${result.updatedRecords} updated; ${result.skippedRows ?? 0} already received and skipped.`}</p>
            {result.planningRefreshRequested && <p>Forecast and shipment refresh was requested with the latest data.</p>}
          </div>
          <a className="notice-action" href={`#${config.destination}`}>{config.destinationLabel}</a>
        </div>}
        <section className="upload-page-grid">
          <form className="upload-page-card" onSubmit={submit}>
            <header><FileUp size={20} /><div><h2>{config.label} CSV</h2><p>Every required column and value must be provided. Invalid files save no changes.</p></div></header>
            <div className="upload-body">
              <label className="upload-type-field"><span>Upload type</span>
                <select disabled={uploading} value={type} onChange={(event) => { setType(event.target.value); resetFile(); }}>
                  {Object.entries(importTypes).map(([key, value]) => <option key={key} value={key}>{value.label}</option>)}
                </select>
              </label>
              {type === "depot-products" && <label className="upload-type-field">
                <span>Delivery reference (Receipt ID)</span>
                <input style={{ padding: "11px", border: "1px solid #d1d5db", borderRadius: "6px", font: "inherit" }} disabled={uploading} required maxLength={100} value={receiptId} onChange={(event) => setReceiptId(event.target.value)} placeholder="e.g. DELIVERY-2026-08-30-002" />
                <small>New delivery: use a new reference. Retrying a delivery: reuse its reference to avoid adding stock twice. This also works with your existing Initial Units CSV.</small>
              </label>}
              <label className={file ? "upload-dropzone file-selected" : "upload-dropzone"}>
                <FileSpreadsheet size={30} /><strong>{file ? "CSV selected" : "Choose a CSV file"}</strong>
                {file ? <div className="selected-file-details"><span><b>Name</b>{file.name}</span><span><b>Size</b>{Math.ceil(file.size / 1024)} KB</span></div> : <span>CSV files up to 50 MB</span>}
                <span className="file-picker-button">{file ? "Choose another file" : "Browse files"}</span>
                <input key={inputKey} accept=".csv,text/csv" disabled={uploading} onChange={selectFile} type="file" aria-label="CSV file" />
              </label>
              <div className="upload-test-file"><span>Start with the required columns</span><a download href={`/${config.template}`}><Download size={14} />Download sample CSV</a></div>
              {file && <button className="clear-file-button" disabled={uploading} onClick={resetFile} type="button"><X size={14} />Remove selected file</button>}
              <div className="upload-column-guide"><h3>Required columns - no blank values</h3><p>{config.columns.join(", ")}</p><p>{config.help}</p></div>
            </div>
            <footer><button className="primary-button" disabled={!file || uploading} type="submit"><Upload size={16} />{uploading ? "Validating and importing..." : type === "depot-refills" ? "Receive stock" : "Import data"}</button></footer>
          </form>
          <aside className="upload-summary"><span>Import outcome</span><h2>What updates next</h2><ul>
            <li><strong>{config.label}</strong><small>{config.outcome}</small></li>
            <li><strong>Complete records only</strong><small>Missing columns, blank required cells, and invalid values reject the file. Fix the reported rows, then upload again. Existing records remain unchanged if the import fails.</small></li>
            <li><strong>Review before uploading</strong><small>Sample rows are examples. Replace them with your real identifiers and quantities. Use 0 or false where valid, never an empty required cell.</small></li>
          </ul></aside>
        </section>
      </main>
    </div>
  );
}

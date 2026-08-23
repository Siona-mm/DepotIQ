import { Download, FileSpreadsheet, FileUp, Upload, X } from "lucide-react";
import { useState } from "react";
import { importHistoricalSalesCsv } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import UserAvatar from "../components/UserAvatar.jsx";

export default function UploadDataView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const selectFile = (event) => { setFile(event.target.files?.[0] ?? null); setError(""); setResult(null); };
  const submit = async (event) => { event.preventDefault(); if (!file) return; setUploading(true); setError(""); setResult(null); try { setResult(await importHistoricalSalesCsv(file)); } catch (requestError) { setError(requestError.message); } finally { setUploading(false); } };
  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}><AppSidebar activePage="Upload Data" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} profile={profile} user={user} />
    <main className="dashboard upload-page"><header className="topbar"><h1>Upload Data</h1><div /><UserAvatar onClick={() => onNavigate("Profile")} profile={profile} user={user} /></header><div className="page-heading"><div><span>Historical data</span><h2>Import store sales records</h2></div></div>
      {(error || result) && <div className={error ? "notice error" : "notice"}>{error || `Import complete: ${result.processedRows} processed, ${result.createdRecords} created, ${result.updatedRecords} updated, ${result.skippedRows} skipped.`}</div>}
      <section className="upload-page-grid"><form className="upload-page-card" onSubmit={submit}><header><FileUp size={20} /><div><h2>Sales and inventory CSV</h2><p>Choose a CSV export from your store, POS, or warehouse system.</p></div></header><div className="upload-body"><label className={file ? "upload-dropzone file-selected" : "upload-dropzone"}><FileSpreadsheet size={30} /><strong>{file ? "CSV ready to import" : "Choose a CSV file"}</strong><span>{file ? `${file.name} - ${Math.ceil(file.size / 1024)} KB` : "CSV files up to 50 MB"}</span><span className="file-picker-button">Browse files</span><input accept=".csv,text/csv" disabled={uploading} onChange={selectFile} type="file" /></label><div className="upload-test-file"><span>Want to test it first?</span><a download href="/sample_sales_inventory_import.csv"><Download size={14} />Download sample CSV</a></div>{file && <button className="clear-file-button" onClick={() => setFile(null)} type="button"><X size={14} />Remove selected file</button>}</div><footer><button className="primary-button" disabled={!file || uploading} type="submit"><Upload size={16} />{uploading ? "Importing data..." : "Import data"}</button></footer></form></section>
    </main></div>;
}

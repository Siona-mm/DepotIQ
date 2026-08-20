import { FileSpreadsheet, FileUp, Upload } from "lucide-react";
import { useState } from "react";
import { importHistoricalSalesCsv } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import UserAvatar from "../components/UserAvatar.jsx";

export default function UploadDataView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user }) {
  const [file, setFile] = useState(null), [uploading, setUploading] = useState(false), [result, setResult] = useState(null), [error, setError] = useState("");
  const submit = async (event) => { event.preventDefault(); if (!file) return; setUploading(true); setError(""); setResult(null); try { setResult(await importHistoricalSalesCsv(file)); } catch (requestError) { setError(requestError.message); } finally { setUploading(false); } };
  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}><AppSidebar activePage="Upload Data" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} profile={profile} user={user} />
    <main className="dashboard upload-page"><header className="topbar"><h1>Upload Data</h1><div /><UserAvatar onClick={() => onNavigate("Profile")} profile={profile} user={user} /></header><div className="page-heading"><div><span>Historical data</span><h2>Import store sales records</h2></div></div>
      {(error || result) && <div className={error ? "notice error" : "notice"}>{error || `Import complete: ${result.processedRows} processed, ${result.createdRecords} created, ${result.updatedRecords} updated, ${result.skippedRows} skipped.`}</div>}
      <section className="upload-page-grid"><form className="upload-page-card" onSubmit={submit}><header><FileUp size={20} /><div><h2>Sales CSV file</h2><p>Rows are matched by store, product, and date. Existing sales records are updated.</p></div></header><label className="upload-dropzone"><FileSpreadsheet size={28} /><strong>{file ? file.name : "Select a CSV file"}</strong><span>{file ? `${Math.ceil(file.size / 1024)} KB selected` : "CSV files only"}</span><input accept=".csv,text/csv" disabled={uploading} onChange={(event) => setFile(event.target.files?.[0] ?? null)} type="file" /></label><footer><button className="primary-button" disabled={!file || uploading} type="submit"><Upload size={16} />{uploading ? "Importing..." : "Import data"}</button></footer></form></section>
    </main></div>;
}

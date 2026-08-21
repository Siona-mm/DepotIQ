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
      <section className="upload-page-grid"><form className="upload-page-card" onSubmit={submit}><header><FileUp size={20} /><div><h2>Sales and inventory CSV</h2><p>Each row updates historical sales and the latest store inventory snapshot.</p></div></header><label className="upload-dropzone"><FileSpreadsheet size={28} /><strong>{file ? file.name : "Select a CSV file"}</strong><span>{file ? `${Math.ceil(file.size / 1024)} KB selected` : "CSV files only"}</span><input accept=".csv,text/csv" disabled={uploading} onChange={(event) => setFile(event.target.files?.[0] ?? null)} type="file" /></label><footer><button className="primary-button" disabled={!file || uploading} type="submit"><Upload size={16} />{uploading ? "Importing..." : "Import data"}</button></footer></form>
        <aside className="upload-help"><h2>Prepare the CSV</h2><p>Required columns:</p><code>Date, Store ID, Product ID, Category, Region, Inventory Level, Units Sold, Units Ordered, Price, Discount, Weather Condition, Holiday/Promotion, Seasonality</code><p>Use dates in <strong>YYYY-MM-DD</strong> format and non-negative whole numbers for unit quantities. Files can be up to <strong>50 MB</strong>.</p><p><strong>Source System</strong> and <strong>External Record ID</strong> are optional. Include them when the file comes from another POS, ERP, or warehouse system so repeated imports update the same records.</p><p>Unknown store and product codes are created as imported catalog entries. Invalid rows are skipped and listed in the import result.</p></aside>
      </section>
    </main></div>;
}

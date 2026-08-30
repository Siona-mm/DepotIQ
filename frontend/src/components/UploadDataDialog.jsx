import { X } from "lucide-react";
import { useEffect, useState } from "react";
import { importTypes, validateCsv } from "../utils/csvImport.js";
import { importHistoricalSalesCsv } from "../api/depotiqApi.js";

export default function UploadDataDialog({ onClose, onImported, open }) {
  const [uploadFile, setUploadFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) {
      return;
    }

    setUploadFile(null);
    setImportResult(null);
    setError("");
  }, [open]);

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    const closeFromEscape = (event) => {
      if (event.key === "Escape" && !uploading) {
        onClose();
      }
    };

    document.addEventListener("keydown", closeFromEscape);
    return () => document.removeEventListener("keydown", closeFromEscape);
  }, [onClose, open, uploading]);

  if (!open) {
    return null;
  }

  const uploadHistoricalSales = async (event) => {
    event.preventDefault();
    if (!uploadFile) {
      return;
    }

    setUploading(true);
    setError("");
    setImportResult(null);

    try {
      validateCsv(await uploadFile.text(), importTypes["sales-records"].columns);
      const result = await importHistoricalSalesCsv(uploadFile);
      setImportResult(result);
      onImported();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={(event) => {
        if (event.target === event.currentTarget && !uploading) {
          onClose();
        }
      }}
    >
      <section
        aria-labelledby="upload-dialog-title"
        aria-modal="true"
        className="upload-dialog"
        role="dialog"
      >
        <header>
          <div>
            <span>Historical data</span>
            <h2 id="upload-dialog-title">Import sales CSV</h2>
          </div>
          <button
            aria-label="Close import dialog"
            className="icon-button"
            disabled={uploading}
            onClick={onClose}
            type="button"
          >
            <X aria-hidden="true" size={15} />
          </button>
        </header>
        <p>
          Upload sales for existing DepotIQ store and product IDs. All required
          columns and values must be present; invalid files save no changes.
        </p>
        <form onSubmit={uploadHistoricalSales}>
          <label className="file-input">
            <span>CSV file</span>
            <input
              accept=".csv,text/csv"
              disabled={uploading}
              onChange={(event) =>
                setUploadFile(event.target.files?.[0] ?? null)
              }
              type="file"
            />
          </label>
          {uploadFile && (
            <small className="selected-file">Selected: {uploadFile.name}</small>
          )}
          {error && (
            <div className="notice error" role="alert">
              {error}
            </div>
          )}
          {importResult && (
            <div className="import-result" role="status">
              <strong>Import complete</strong>
              <span>
                {importResult.processedRows} processed Ã‚·{" "}
                {importResult.createdRecords} created Ã‚·{" "}
                {importResult.updatedRecords} updated Ã‚·{" "}
                {importResult.skippedRows} skipped
              </span>
              {importResult.errors?.length > 0 && (
                <ul>
                  {importResult.errors.slice(0, 3).map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
          <footer>
            <button
              className="secondary-button"
              disabled={uploading}
              onClick={onClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="save-button"
              disabled={!uploadFile || uploading}
              type="submit"
            >
              {uploading ? "Importing..." : "Import data"}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

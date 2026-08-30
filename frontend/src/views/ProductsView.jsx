import { PackageSearch, PencilLine, Plus, Search, Tags, Trash2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { createProduct, deleteProduct, loadProducts, updateProduct } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";
import { compareBusinessCodes, productDetailIssues, productNeedsDetails } from "../utils/businessCodes.js";
import { buildProductPayload, catalogFormValues, EMPTY_PRODUCT, PRODUCT_FIELDS } from "../utils/catalogForms.js";

const money = (value) => value == null ? "Not set" : new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(Number(value));

export default function ProductsView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, permissions, profile, user }) {
  const [products, setProducts] = useState([]), [loading, setLoading] = useState(true), [error, setError] = useState(""), [query, setQuery] = useState("");
  const [message, setMessage] = useState("");
  const [formOpen, setFormOpen] = useState(false), [editing, setEditing] = useState(null), [form, setForm] = useState(EMPTY_PRODUCT), [saving, setSaving] = useState(false);
  const load = useCallback(async () => { try { setError(""); setProducts(await loadProducts()); } catch (requestError) { setError(requestError.message); } finally { setLoading(false); } }, []);
  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => { const normalized = query.toLowerCase().trim(); return products.filter((product) => !normalized || [product.productCode, product.externalSku, product.name, product.category, product.brand, product.supplierCode].some((value) => String(value ?? "").toLowerCase().includes(normalized))).sort((left, right) => compareBusinessCodes(left.productCode, right.productCode)); }, [products, query]);
  const incompleteCount = products.filter(productNeedsDetails).length;
  const openForm = (product = null) => { setEditing(product); setForm(catalogFormValues(EMPTY_PRODUCT, product ?? {})); setError(""); setFormOpen(true); };
  const closeForm = () => { if (!saving) setFormOpen(false); };
  const setField = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  const save = async (event) => {
    event.preventDefault(); setSaving(true); setError("");
    try {
      const payload = buildProductPayload(form);
      const saved = editing ? await updateProduct(editing.id, payload) : await createProduct(payload);
      setProducts((current) => editing ? current.map((product) => product.id === saved.id ? saved : product) : [...current, saved]);
      setFormOpen(false);
      setMessage(`${saved.productCode} was ${editing ? "updated" : "created"} with complete product details.`);
    } catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  };
  const remove = async (product) => { if (!globalThis.confirm(`Delete ${product.productCode}?`)) return; try { await deleteProduct(product.id); setProducts((current) => current.filter((item) => item.id !== product.id)); } catch (requestError) { setError(requestError.message); } };

  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Products" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} permissions={permissions} profile={profile} user={user} />
    <main className="dashboard products-page">
      <header className="topbar"><h1>Products</h1><label className="search-box"><Search size={15} /><span className="sr-only">Search products</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Search product, code, category, or brand..." value={query} /></label><HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} /></header>
      {!formOpen && (error || message) && <div className={error ? "notice error" : "notice"} role="status">{error || message}</div>}
      {incompleteCount > 0 && <div className="notice" role="status">{incompleteCount} product{incompleteCount === 1 ? " needs" : "s need"} catalog details corrected. Each marked row lists the fields to check; use Edit to correct them.</div>}
      <section className="metrics-grid"><article className="metric-card"><PackageSearch size={20} /><div><span>Total Products</span><strong>{products.length}</strong><small>In product catalog</small></div></article><article className="metric-card"><Tags size={20} /><div><span>Categories</span><strong>{new Set(products.map((product) => product.category)).size}</strong><small>Product groups</small></div></article></section>
      <section className="table-panel products-table-panel">
        <div className="panel-toolbar"><h2>Product catalog</h2><div className="table-actions"><button className="tool-button" onClick={() => openForm()} type="button"><Plus size={14} />Add product</button></div></div>
        <div className="table-scroll"><table>
          <thead><tr><th>Code</th><th>External SKU</th><th>Product</th><th>Category</th><th>Brand</th><th>Supplier</th><th>Unit cost</th><th>Price</th><th>Weight</th><th>Shelf life</th><th>Perishable</th><th>Actions</th></tr></thead>
          <tbody>{visible.map((product) => <tr key={product.id}>
            <td>{product.productCode}</td><td title={product.externalSku || "No external catalog SKU linked"}>{product.externalSku || "—"}</td>
            <td className="product-directory-name"><strong title={product.name}>{product.name || "Not set"}</strong>{productNeedsDetails(product) && <span className="store-details-warning product-details-warning" title={`Check: ${productDetailIssues(product).join(", ")}`}>Check: {productDetailIssues(product).join(", ")}</span>}</td>
            <td title={product.category}>{product.category || "Not set"}</td><td title={product.brand}>{product.brand || "Not set"}</td><td title={product.supplierCode}>{product.supplierCode || "Not set"}</td>
            <td>{money(product.unitCost)}</td><td>{money(product.price)}</td><td>{product.weightKg > 0 ? `${product.weightKg} kg` : "Not set"}</td>
            <td>{product.shelfLifeDays == null || (product.perishable && product.shelfLifeDays < 1) ? "Not set" : `${product.shelfLifeDays} days`}</td>
            <td>{product.perishable == null ? "Not set" : product.perishable ? "Yes" : "No"}</td>
            <td><div className="action-buttons"><button aria-label={`Edit ${product.productCode}`} className="icon-button" onClick={() => openForm(product)} type="button"><PencilLine size={14} /></button><button aria-label={`Delete ${product.productCode}`} className="reject-button" onClick={() => remove(product)} type="button"><Trash2 size={14} /></button></div></td>
          </tr>)}</tbody>
        </table></div>
        {loading && <div className="panel-empty">Loading products...</div>}
        {!loading && !visible.length && <div className="panel-empty">No products match your search.</div>}
      </section>
    </main>
    {formOpen && <div className="modal-backdrop" onClick={(event) => { if (event.target === event.currentTarget) closeForm(); }}>
      <section aria-modal="true" aria-labelledby="product-form-title" className="override-dialog product-form-dialog" role="dialog">
        <header><div><span>Product catalog</span><h2 id="product-form-title">{editing ? "Edit product" : "Add product"}</h2></div><button aria-label="Close product dialog" className="icon-button" disabled={saving} onClick={closeForm} type="button"><X size={16} /></button></header>
        <form onSubmit={save}>
          <p className="catalog-form-help">Product details are required; the external SKU is optional unless you use catalog CSV uploads. DepotIQ generates the product code. Creating a product does not add stock.</p>
          {error && <div className="notice error catalog-form-error" role="alert">{error}</div>}
          <div className="dialog-field-grid">
            <label className="generated-code-field">Product code<input disabled value={editing?.productCode || "Generated automatically"} /><small>Assigned by DepotIQ when the product is created.</small></label>
            {PRODUCT_FIELDS.map(({ key, label, type = "text", min, max, step, maxLength, optional }) => <label key={key}>{label}
              <input min={key === "shelfLifeDays" && form.perishable === "true" ? 1 : min} max={max} maxLength={maxLength} onChange={(event) => setField(key, event.target.value)} required={!optional} step={step} type={type} value={form[key]} />
              {key === "externalSku" && <small>Use this same SKU in catalog and depot CSV uploads.</small>}
              {key === "shelfLifeDays" && <small>Whole days. Use 0 only for non-perishable products.</small>}
            </label>)}
            <label>Perishable product<select required value={form.perishable} onChange={(event) => setField("perishable", event.target.value)}><option value="" disabled>Select Yes or No</option><option value="true">Yes</option><option value="false">No</option></select></label>
          </div>
          <footer><button className="secondary-button" disabled={saving} onClick={closeForm} type="button">Cancel</button><button className="save-button" disabled={saving} type="submit">{saving ? "Saving..." : "Save product"}</button></footer>
        </form>
      </section>
    </div>}
  </div>;
}

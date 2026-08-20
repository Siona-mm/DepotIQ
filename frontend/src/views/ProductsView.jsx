import { PackageSearch, PencilLine, Plus, Search, Tags, Trash2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { createProduct, deleteProduct, loadProducts, updateProduct } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import UserAvatar from "../components/UserAvatar.jsx";

const money = (value) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(Number(value ?? 0));
const EMPTY_PRODUCT = { productCode: "", name: "", category: "", brand: "", supplierCode: "", unitCost: 0, price: 0, weightKg: 0, shelfLifeDays: 0, perishable: false };
const FIELDS = [
  ["productCode", "Product code", "text"], ["name", "Name", "text"], ["category", "Category", "text"],
  ["brand", "Brand", "text"], ["supplierCode", "Supplier code", "text"], ["unitCost", "Unit cost", "number"],
  ["price", "Price", "number"], ["weightKg", "Weight (kg)", "number"], ["shelfLifeDays", "Shelf life (days)", "number"],
];

export default function ProductsView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, profile, user }) {
  const [products, setProducts] = useState([]), [loading, setLoading] = useState(true), [error, setError] = useState(""), [query, setQuery] = useState("");
  const [formOpen, setFormOpen] = useState(false), [editing, setEditing] = useState(null), [form, setForm] = useState(EMPTY_PRODUCT), [saving, setSaving] = useState(false);
  const load = useCallback(async () => { try { setError(""); setProducts(await loadProducts()); } catch (requestError) { setError(requestError.message); } finally { setLoading(false); } }, []);
  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => { const normalized = query.toLowerCase().trim(); return !normalized ? products : products.filter((product) => [product.productCode, product.name, product.category, product.brand].some((value) => String(value ?? "").toLowerCase().includes(normalized))); }, [products, query]);
  const openForm = (product = null) => { setEditing(product); setForm(product ? { ...product } : EMPTY_PRODUCT); setError(""); setFormOpen(true); };
  const closeForm = () => { if (!saving) setFormOpen(false); };
  const setField = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  const save = async (event) => { event.preventDefault(); setSaving(true); setError(""); try { const payload = { ...form, unitCost: Number(form.unitCost), price: Number(form.price), weightKg: Number(form.weightKg), shelfLifeDays: Number(form.shelfLifeDays) }; const saved = editing ? await updateProduct(editing.id, (({ productCode: _productCode, ...value }) => value)(payload)) : await createProduct(payload); setProducts((current) => editing ? current.map((product) => product.id === saved.id ? saved : product) : [...current, saved]); setFormOpen(false); } catch (requestError) { setError(requestError.message); } finally { setSaving(false); } };
  const remove = async (product) => { if (!globalThis.confirm(`Delete ${product.productCode}?`)) return; try { await deleteProduct(product.id); setProducts((current) => current.filter((item) => item.id !== product.id)); } catch (requestError) { setError(requestError.message); } };

  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Products" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} />
    <main className="dashboard products-page">
      <header className="topbar"><h1>Products</h1><label className="search-box"><Search size={15} /><span className="sr-only">Search products</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Search product, code, category, or brand..." value={query} /></label><UserAvatar onClick={() => onNavigate("Profile")} profile={profile} user={user} /></header>
      {error && <div className="notice error">{error}</div>}
      <section className="metrics-grid"><article className="metric-card"><PackageSearch size={20} /><div><span>Total Products</span><strong>{products.length}</strong><small>In product catalog</small></div></article><article className="metric-card"><Tags size={20} /><div><span>Categories</span><strong>{new Set(products.map((product) => product.category)).size}</strong><small>Product groups</small></div></article></section>
      <section className="table-panel products-table-panel"><div className="panel-toolbar"><h2>Product catalog</h2><div className="table-actions"><button className="tool-button" onClick={() => openForm()} type="button"><Plus size={14} />Add product</button></div></div><div className="table-scroll"><table><thead><tr><th>Code</th><th>Product</th><th>Category</th><th>Brand</th><th>Supplier</th><th>Price</th><th>Perishable</th><th>Actions</th></tr></thead><tbody>{visible.map((product) => <tr key={product.id}><td>{product.productCode}</td><td>{product.name}</td><td>{product.category}</td><td>{product.brand || "—"}</td><td>{product.supplierCode || "—"}</td><td>{money(product.price)}</td><td>{product.perishable ? "Yes" : "No"}</td><td><div className="action-buttons"><button aria-label={`Edit ${product.productCode}`} className="icon-button" onClick={() => openForm(product)} type="button"><PencilLine size={14} /></button><button aria-label={`Delete ${product.productCode}`} className="reject-button" onClick={() => remove(product)} type="button"><Trash2 size={14} /></button></div></td></tr>)}</tbody></table></div>{loading && <div className="panel-empty">Loading products...</div>}{!loading && !visible.length && <div className="panel-empty">No products match your search.</div>}</section>
    </main>
    {formOpen && <div className="modal-backdrop" onClick={(event) => { if (event.target === event.currentTarget) closeForm(); }}><section aria-modal="true" className="override-dialog product-form-dialog" role="dialog"><header><div><span>Product catalog</span><h2>{editing ? "Edit product" : "Add product"}</h2></div><button aria-label="Close product dialog" className="icon-button" disabled={saving} onClick={closeForm} type="button"><X size={16} /></button></header><form onSubmit={save}><div className="dialog-field-grid">{FIELDS.map(([key, label, type]) => <label key={key}>{label}<input disabled={editing && key === "productCode"} min={type === "number" ? "0" : undefined} onChange={(event) => setField(key, event.target.value)} required={["productCode", "name", "category"].includes(key)} step={type === "number" ? "0.01" : undefined} type={type} value={form[key] ?? ""} /></label>)}</div><label className="checkbox-field"><input checked={form.perishable} onChange={(event) => setField("perishable", event.target.checked)} type="checkbox" />Perishable product</label><footer><button className="secondary-button" onClick={closeForm} type="button">Cancel</button><button className="save-button" disabled={saving} type="submit">{saving ? "Saving..." : "Save product"}</button></footer></form></section></div>}
  </div>;
}

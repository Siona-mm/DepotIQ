import { PackageSearch, Search, Tags } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { loadProducts } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

const money = (value) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(Number(value ?? 0));

export default function ProductsView({ collapsed, onAction, onCollapse, onNavigate }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const load = useCallback(async () => { try { setError(""); setProducts(await loadProducts()); } catch (e) { setError(e.message); } finally { setLoading(false); } }, []);
  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => { const q = query.toLowerCase().trim(); return !q ? products : products.filter((p) => [p.productCode, p.name, p.category, p.brand].some((v) => String(v ?? "").toLowerCase().includes(q))); }, [products, query]);
  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Products" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} />
    <main className="dashboard products-page"><header className="topbar"><h1>Products</h1><label className="search-box"><Search size={15} /><span className="sr-only">Search products</span><input onChange={(e) => setQuery(e.target.value)} placeholder="Search product, code, category, or brand..." value={query} /></label></header>
      {error && <div className="notice error">{error}</div>}
      <section className="metrics-grid"><article className="metric-card"><PackageSearch size={20} /><div><span>Total Products</span><strong>{products.length}</strong><small>In product catalog</small></div></article><article className="metric-card"><Tags size={20} /><div><span>Categories</span><strong>{new Set(products.map((p) => p.category)).size}</strong><small>Product groups</small></div></article></section>
      <section className="table-panel products-table-panel"><div className="panel-toolbar"><h2>Product catalog</h2><span>{visible.length} shown</span></div><div className="table-scroll"><table><thead><tr><th>Code</th><th>Product</th><th>Category</th><th>Brand</th><th>Supplier</th><th>Price</th><th>Perishable</th></tr></thead><tbody>{visible.map((p) => <tr key={p.id}><td>{p.productCode}</td><td>{p.name}</td><td>{p.category}</td><td>{p.brand || "—"}</td><td>{p.supplierCode || "—"}</td><td>{money(p.price)}</td><td>{p.perishable ? "Yes" : "No"}</td></tr>)}</tbody></table></div>{loading && <div className="panel-empty">Loading products...</div>}{!loading && !visible.length && <div className="panel-empty">No products match your search.</div>}</section>
    </main></div>;
}

import { useState, useEffect } from 'react';
import { getInventoryView, addProduct, addStock } from '../services/api';
import useInventoryFilter from '../hooks/useInventoryFilter';

const InventoryManager = () => {
    const [products, setProducts] = useState([]);
    const [newProduct, setNewProduct] = useState({ sku: '', name: '', type: 'BOOK', basePrice: '', attributes: {} });
    const [stock, setStock] = useState({ sku: '', quantity: '' });
    const [message, setMessage] = useState('');

    // Use shared hook for filtering and column management
    const {
        searchQuery, setSearchQuery,
        searchField, setSearchField,
        visibleColumns, toggleColumn,
        isColumnSelectorOpen, setIsColumnSelectorOpen,
        filteredItems: filteredProducts
    } = useInventoryFilter(products);

    useEffect(() => {
        loadProducts();
    }, []);

    const loadProducts = async () => {
        try {
            const data = await getInventoryView();
            setProducts(data);
        } catch (e) {
            console.error("Failed to load products");
        }
    }

    const handleAddProduct = async (e) => {
        e.preventDefault();
        try {
            // Map FRONTEND type to BACKEND type if needed
            // Backend expects 'BOOK', 'PENCIL' (for stationery), 'APPAREL'
            // Frontend 'STATIONERY' maps to 'PENCIL'
            const payload = { ...newProduct };
            if (payload.type === 'STATIONERY') {
                payload.type = 'PENCIL'; // Internal backend mapping
            }

            await addProduct(payload);
            setMessage('Product created successfully');
            setNewProduct({ sku: '', name: '', type: 'BOOK', basePrice: '', attributes: {} });
            loadProducts();
        } catch (err) {
            setMessage('Failed to create product');
        }
    };

    const handleAddStock = async (e) => {
        e.preventDefault();
        try {
            await addStock(stock.sku, parseInt(stock.quantity));
            setMessage(`Stock added for ${stock.sku}`);
            setStock({ sku: '', quantity: '' });
            loadProducts();
        } catch (err) {
            setMessage('Failed to add stock');
        }
    };



    return (
        <div>
            <h2>Inventory Management (Headquarters)</h2>
            {message && <p style={{ padding: '10px', background: '#dff9fb', color: '#27ae60', borderRadius: '5px' }}>{message}</p>}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2rem' }}>
                {/* Add Product Form */}
                <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                    <h3>Add New Product</h3>
                    <form onSubmit={handleAddProduct} style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1rem' }}>
                        <input type="text" placeholder="SKU" value={newProduct.sku} onChange={e => setNewProduct({ ...newProduct, sku: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <input type="text" placeholder="Name" value={newProduct.name} onChange={e => setNewProduct({ ...newProduct, name: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <select value={newProduct.type} onChange={e => setNewProduct({ ...newProduct, type: e.target.value, attributes: {} })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}>
                            <option value="BOOK">Book</option>
                            <option value="STATIONERY">Stationery</option>
                        </select>
                        <input type="number" placeholder="Base Price" value={newProduct.basePrice} onChange={e => setNewProduct({ ...newProduct, basePrice: e.target.value })} required step="0.01" style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />

                        {/* Dynamic Attributes */}
                        {newProduct.type === 'BOOK' && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', background: '#f8f9fa', padding: '10px', borderRadius: '4px' }}>
                                <h4>Book Details</h4>
                                <input type="text" placeholder="Author" value={newProduct.attributes.author || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, author: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input type="text" placeholder="ISBN" value={newProduct.attributes.isbn || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, isbn: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input type="text" placeholder="Publisher" value={newProduct.attributes.publisher || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, publisher: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                            </div>
                        )}
                        {newProduct.type === 'STATIONERY' && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', background: '#f8f9fa', padding: '10px', borderRadius: '4px' }}>
                                <h4>Stationery Details</h4>
                                <input type="text" placeholder="Brand" value={newProduct.attributes.brand || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, brand: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input type="text" placeholder="Hardness (e.g. HB)" value={newProduct.attributes.hardness || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, hardness: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input type="text" placeholder="Material" value={newProduct.attributes.material || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, material: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                            </div>
                        )}

                        <button type="submit" style={{ padding: '10px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Create Product</button>
                    </form>
                </div>

                {/* Add Stock Form */}
                <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                    <h3>Add Stock</h3>
                    <form onSubmit={handleAddStock} style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1rem' }}>
                        <input type="text" placeholder="SKU" value={stock.sku} onChange={e => setStock({ ...stock, sku: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <input type="number" placeholder="Quantity" value={stock.quantity} onChange={e => setStock({ ...stock, quantity: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <button type="submit" style={{ padding: '10px', background: '#2ecc71', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Add Stock</button>
                    </form>
                </div>
            </div>

            {/* Search Bar */}
            <div style={{ marginBottom: '1rem', background: 'white', padding: '10px', borderRadius: '8px', display: 'flex', alignItems: 'center' }}>
                <select
                    value={searchField}
                    onChange={e => setSearchField(e.target.value)}
                    style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd', marginRight: '10px' }}
                >
                    <option value="all">All</option>
                    <option value="name">Name</option>
                    <option value="sku">SKU</option>
                    <option value="author">Author</option>
                    <option value="isbn">ISBN</option>
                    <option value="brand">Brand</option>
                </select>
                <input
                    type="text"
                    placeholder="Search..."
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                    style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd', width: '300px' }}
                />
            </div>

            <div style={{ marginTop: '1rem', background: 'white', padding: '1rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                        <tr style={{ background: '#f8f9fa', userSelect: 'none' }}>
                            {visibleColumns.sku && <th style={{ padding: '12px', textAlign: 'left' }}>SKU</th>}
                            {visibleColumns.name && <th style={{ padding: '12px', textAlign: 'left' }}>Name</th>}
                            {visibleColumns.price && <th style={{ padding: '12px', textAlign: 'left' }}>Price</th>}
                            {visibleColumns.type && <th style={{ padding: '12px', textAlign: 'left' }}>Type</th>}

                            {visibleColumns.author && <th style={{ padding: '12px', textAlign: 'left' }}>Author</th>}
                            {visibleColumns.isbn && <th style={{ padding: '12px', textAlign: 'left' }}>ISBN</th>}
                            {visibleColumns.brand && <th style={{ padding: '12px', textAlign: 'left' }}>Brand</th>}
                            {visibleColumns.hardness && <th style={{ padding: '12px', textAlign: 'left' }}>Hardness</th>}

                            {visibleColumns.stock && <th style={{ padding: '12px', textAlign: 'left' }}>Stock</th>}

                            {/* Column Selection Header */}
                            <th style={{ padding: '12px', textAlign: 'right', position: 'relative' }}>
                                <button
                                    onClick={() => setIsColumnSelectorOpen(!isColumnSelectorOpen)}
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.2rem' }}
                                >
                                    ⚙️
                                </button>
                                {isColumnSelectorOpen && (
                                    <div style={{
                                        position: 'absolute',
                                        right: 0,
                                        top: '100%',
                                        background: 'white',
                                        border: '1px solid #ddd',
                                        borderRadius: '8px',
                                        padding: '10px',
                                        boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                                        zIndex: 10,
                                        width: '200px',
                                        textAlign: 'left'
                                    }}>
                                        <div style={{ fontWeight: 'bold', marginBottom: '8px', borderBottom: '1px solid #eee', paddingBottom: '4px' }}>Columns</div>
                                        {Object.keys(visibleColumns).map(col => (
                                            <label key={col} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 0', cursor: 'pointer' }}>
                                                <input type="checkbox" checked={visibleColumns[col]} onChange={() => toggleColumn(col)} />
                                                {col.charAt(0).toUpperCase() + col.slice(1)}
                                            </label>
                                        ))}
                                    </div>
                                )}
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredProducts.map(p => (
                            <tr key={p.id} style={{ borderBottom: '1px solid #eee' }}>
                                {visibleColumns.sku && <td style={{ padding: '12px' }}>{p.sku}</td>}
                                {visibleColumns.name && <td style={{ padding: '12px' }}>{p.name}</td>}
                                {visibleColumns.price && <td style={{ padding: '12px' }}>₹{p.basePrice || p.price}</td>}
                                {visibleColumns.type && <td style={{ padding: '12px' }}>{p.type || 'BUNDLE'}</td>}

                                {visibleColumns.author && <td style={{ padding: '12px' }}>{p.attributes?.author || '-'}</td>}
                                {visibleColumns.isbn && <td style={{ padding: '12px' }}>{p.attributes?.isbn || '-'}</td>}
                                {visibleColumns.brand && <td style={{ padding: '12px' }}>{p.attributes?.brand || '-'}</td>}
                                {visibleColumns.hardness && <td style={{ padding: '12px' }}>{p.attributes?.hardness || '-'}</td>}

                                {visibleColumns.stock && <td style={{ padding: '12px', fontWeight: 'bold', color: p.quantity > 0 ? '#27ae60' : '#e74c3c' }}>
                                    {p.quantity}
                                </td>}
                                <td style={{ padding: '12px' }}></td> {/* Empty cell for the actions column */}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default InventoryManager;

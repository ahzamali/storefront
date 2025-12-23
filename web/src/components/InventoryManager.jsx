import { useState, useEffect } from 'react';
import { getInventoryView, addProduct, addStock, getBundles, createBundle, getStores, allocateStock, returnStock } from '../services/api';
import useInventoryFilter from '../hooks/useInventoryFilter';

const InventoryManager = () => {
    const [products, setProducts] = useState([]);
    const [bundles, setBundles] = useState([]);
    const [stores, setStores] = useState([]);
    const [selectedStoreId, setSelectedStoreId] = useState(''); // '' means HQ
    const [stock, setStock] = useState({ sku: '', quantity: '' });
    const [message, setMessage] = useState('');
    const [newProduct, setNewProduct] = useState({ sku: '', name: '', type: 'BOOK', basePrice: '', attributes: {} });
    const [selectedItems, setSelectedItems] = useState(new Set());

    // User Role Logic
    const userRole = localStorage.getItem('userRole');
    console.log('Current User Role from LocalStorage:', userRole);
    // Normalize logic: remove ROLE_ prefix if present to handle both conventions
    const normalizedRole = userRole ? userRole.replace('ROLE_', '') : '';
    const canEdit = ['SUPER_ADMIN', 'ADMIN', 'STORE_ADMIN'].includes(normalizedRole);
    console.log('Can Edit:', canEdit);

    // Modal states
    const [isBundleModalOpen, setIsBundleModalOpen] = useState(false);
    const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
    const [transferTargetStore, setTransferTargetStore] = useState('');
    const [transferQty, setTransferQty] = useState(1); // Simple global qty for now

    const [newBundle, setNewBundle] = useState({ name: '', description: '', price: '' });

    // Use shared hook for filtering and column management
    const {
        searchQuery, setSearchQuery,
        searchField, setSearchField,
        visibleColumns, toggleColumn,
        isColumnSelectorOpen, setIsColumnSelectorOpen,
        filteredItems: filteredProducts
    } = useInventoryFilter([...products, ...bundles]);

    useEffect(() => {
        loadInventory();
        loadStores();
    }, [selectedStoreId]); // Reload when store changes

    const loadStores = async () => {
        try {
            const data = await getStores();
            setStores(data);
        } catch (e) {
            console.error("Failed to load stores");
        }
    }

    const loadInventory = async () => {
        try {
            const [prodData, bundleData] = await Promise.all([
                getInventoryView(selectedStoreId || null),
                getBundles()
            ]);
            setProducts(prodData);
            // Mark bundles for visual distinction
            setBundles(bundleData.map(b => ({ ...b, type: 'BUNDLE', quantity: 'N/A' })));
        } catch (e) {
            console.error("Failed to load inventory", e);
            setMessage("Failed to load inventory");
        }
    }

    const toggleSelection = (sku) => {
        const newSet = new Set(selectedItems);
        if (newSet.has(sku)) {
            newSet.delete(sku);
        } else {
            newSet.add(sku);
        }
        setSelectedItems(newSet);
    };

    const toggleSelectAll = () => {
        if (selectedItems.size === filteredProducts.length) {
            setSelectedItems(new Set());
        } else {
            const allSkus = new Set(filteredProducts.map(p => p.sku));
            setSelectedItems(allSkus);
        }
    };

    const handleCreateBundle = async (e) => {
        e.preventDefault();
        try {
            const items = Array.from(selectedItems).map(sku => {
                // Determine if it's a product (bundles cannot be nested in bundles for now per typical logic, or if they can, we assume flat sku list)
                // We'll just pass SKU and quantity 1 for now as simplified MVP
                // Ideally we might want to ask quantity per item
                return { productSku: sku, quantity: 1 };
            });

            const payload = {
                sku: 'BNDL-' + Math.random().toString(36).substr(2, 6).toUpperCase(),
                name: newBundle.name,
                description: newBundle.description,
                price: parseFloat(newBundle.price),
                items: items
            };

            await createBundle(payload);
            setMessage('Bundle created successfully');
            setIsBundleModalOpen(false);
            setNewBundle({ name: '', description: '', price: '' });
            setSelectedItems(new Set());
            loadInventory();
        } catch (err) {
            console.error(err);
            setMessage('Failed to create bundle');
        }
    };

    const handleTransfer = async () => {
        if (!selectedStoreId && !transferTargetStore) {
            alert("Please select a target store");
            return;
        }

        try {
            const items = Array.from(selectedItems).map(sku => ({ sku, quantity: parseInt(transferQty) }));

            if (!selectedStoreId) {
                // HQ -> Store
                await allocateStock(transferTargetStore, items);
                setMessage("Transferred to store successfully");
            } else {
                // Store -> HQ (Return)
                // Logic: Button should trigger this directly without modal if it's return? 
                // Or maybe reuse generic transfer logic?
                // Let's implement Return separately below
            }

            setIsTransferModalOpen(false);
            setSelectedItems(new Set());
            loadInventory();
        } catch (e) {
            setMessage('Transfer failed: ' + (e.response?.data?.message || e.message));
        }
    };

    const handleReturnToHQ = async () => {
        if (!window.confirm("Return selected items to HQ?")) return;
        try {
            // For return, we also need quantity. For now assume 1 or ask? 
            // Let's assume 1 for simplicity of this MVP button, or prompt
            const qty = prompt("Quantity to return per item:", "1");
            if (!qty) return;

            const items = Array.from(selectedItems).map(sku => ({ sku, quantity: parseInt(qty) }));
            await returnStock(selectedStoreId, items);
            setMessage("Returned stock to HQ");
            setSelectedItems(new Set());
            loadInventory();
        } catch (e) {
            setMessage('Return failed: ' + (e.response?.data?.message || e.message));
        }
    };

    // ... (Existing addProduct logic remain same)
    const handleAddProduct = async (e) => {
        e.preventDefault();
        try {
            const payload = { ...newProduct };
            if (payload.type === 'STATIONERY') {
                payload.type = 'PENCIL';
            }

            await addProduct(payload);
            setMessage('Product created successfully');
            setNewProduct({ sku: '', name: '', type: 'BOOK', basePrice: '', attributes: {} });
            loadInventory();
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
            loadInventory();
        } catch (err) {
            setMessage('Failed to add stock');
        }
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2>Inventory Management</h2>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <strong>View Store:</strong>
                    <select
                        value={selectedStoreId}
                        onChange={e => setSelectedStoreId(e.target.value)}
                        style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                    >
                        <option value="">Headquarters (Master)</option>
                        {stores.map(s => (
                            <option key={s.id} value={s.id}>{s.name} ({s.type})</option>
                        ))}
                    </select>
                </div>
            </div>

            {message && <p style={{ padding: '10px', background: '#dff9fb', color: '#27ae60', borderRadius: '5px' }}>{message}</p>}

            {/* Bundle Creation Modal */}
            {isBundleModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
                }}>
                    <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '400px' }}>
                        <h3>Create Bundle</h3>
                        <p>{selectedItems.size} items selected</p>

                        {/* bundling logic helper */}
                        {(() => {
                            const selectedProductObjects = filteredProducts.filter(p => selectedItems.has(p.sku));
                            const totalPrice = selectedProductObjects.reduce((sum, p) => sum + (parseFloat(p.basePrice || p.price) || 0), 0);

                            return (
                                <div style={{ marginBottom: '10px', fontSize: '0.9rem', color: '#555' }}>
                                    <strong>Total Item Price: </strong> ₹{totalPrice.toFixed(2)}
                                </div>
                            );
                        })()}

                        <form onSubmit={handleCreateBundle} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <input type="text" placeholder="Bundle Name" value={newBundle.name} onChange={e => setNewBundle({ ...newBundle, name: e.target.value })} required style={{ padding: '8px', border: '1px solid #ddd' }} />
                            <input type="text" placeholder="Description" value={newBundle.description} onChange={e => setNewBundle({ ...newBundle, description: e.target.value })} style={{ padding: '8px', border: '1px solid #ddd' }} />

                            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                                <div style={{ flex: 1 }}>
                                    <label style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Discount (%)</label>
                                    <input
                                        type="number"
                                        min="0" max="100"
                                        placeholder="0%"
                                        onChange={e => {
                                            const discount = parseFloat(e.target.value) || 0;
                                            const selectedProductObjects = filteredProducts.filter(p => selectedItems.has(p.sku));
                                            const totalPrice = selectedProductObjects.reduce((sum, p) => sum + (parseFloat(p.basePrice || p.price) || 0), 0);
                                            const discountedPrice = totalPrice * (1 - discount / 100);
                                            setNewBundle({ ...newBundle, price: discountedPrice.toFixed(2) });
                                        }}
                                        style={{ width: '100%', padding: '8px', border: '1px solid #ddd' }}
                                    />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <label style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Final Price</label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        placeholder="Price"
                                        value={newBundle.price}
                                        onChange={e => setNewBundle({ ...newBundle, price: e.target.value })}
                                        required
                                        style={{ width: '100%', padding: '8px', border: '1px solid #ddd' }}
                                    />
                                </div>
                            </div>

                            <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                                <button type="submit" style={{ flex: 1, height: '40px', padding: '0 10px', background: '#2ecc71', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Create</button>
                                <button type="button" onClick={() => setIsBundleModalOpen(false)} style={{ flex: 1, height: '40px', padding: '0 10px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Cancel</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Transfer Modal (HQ -> Store) */}
            {isTransferModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
                }}>
                    <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '400px' }}>
                        <h3>Transfer Stock to Store</h3>
                        <p>{selectedItems.size} items selected</p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <select
                                value={transferTargetStore}
                                onChange={e => setTransferTargetStore(e.target.value)}
                                style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                            >
                                <option value="">Select Target Store...</option>
                                {stores.filter(s => s.type !== 'MASTER').map(s => (
                                    <option key={s.id} value={s.id}>{s.name}</option>
                                ))}
                            </select>
                            <input
                                type="number" min="1" placeholder="Quantity per item"
                                value={transferQty}
                                onChange={e => setTransferQty(e.target.value)}
                                style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                            />
                            <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                                <button onClick={handleTransfer} style={{ flex: 1, height: '40px', padding: '0 10px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Transfer</button>
                                <button onClick={() => setIsTransferModalOpen(false)} style={{ flex: 1, height: '40px', padding: '0 10px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Cancel</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2rem' }}>
                {/* Add Product Form */}
                <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)', opacity: canEdit ? 1 : 0.6, pointerEvents: canEdit ? 'auto' : 'none' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h3>Add New Product</h3>
                        {!canEdit && <span style={{ fontSize: '0.8rem', color: '#e74c3c' }}>Admin Only</span>}
                    </div>
                    <form onSubmit={handleAddProduct} style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1rem' }}>
                        <input disabled={!canEdit} type="text" placeholder="SKU" value={newProduct.sku} onChange={e => setNewProduct({ ...newProduct, sku: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <input disabled={!canEdit} type="text" placeholder="Name" value={newProduct.name} onChange={e => setNewProduct({ ...newProduct, name: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <select disabled={!canEdit} value={newProduct.type} onChange={e => setNewProduct({ ...newProduct, type: e.target.value, attributes: {} })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}>
                            <option value="BOOK">Book</option>
                            <option value="STATIONERY">Stationery</option>
                        </select>
                        <input disabled={!canEdit} type="number" placeholder="Base Price" value={newProduct.basePrice} onChange={e => setNewProduct({ ...newProduct, basePrice: e.target.value })} required step="0.01" style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />

                        {/* Dynamic Attributes */}
                        {newProduct.type === 'BOOK' && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', background: '#f8f9fa', padding: '10px', borderRadius: '4px' }}>
                                <h4>Book Details</h4>
                                <input disabled={!canEdit} type="text" placeholder="Author" value={newProduct.attributes.author || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, author: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input disabled={!canEdit} type="text" placeholder="ISBN" value={newProduct.attributes.isbn || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, isbn: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input disabled={!canEdit} type="text" placeholder="Publisher" value={newProduct.attributes.publisher || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, publisher: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                            </div>
                        )}
                        {newProduct.type === 'STATIONERY' && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', background: '#f8f9fa', padding: '10px', borderRadius: '4px' }}>
                                <h4>Stationery Details</h4>
                                <input disabled={!canEdit} type="text" placeholder="Brand" value={newProduct.attributes.brand || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, brand: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input disabled={!canEdit} type="text" placeholder="Hardness (e.g. HB)" value={newProduct.attributes.hardness || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, hardness: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                                <input disabled={!canEdit} type="text" placeholder="Material" value={newProduct.attributes.material || ''} onChange={e => setNewProduct({ ...newProduct, attributes: { ...newProduct.attributes, material: e.target.value } })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                            </div>
                        )}

                        <button disabled={!canEdit} type="submit" style={{ padding: '10px', background: canEdit ? '#3498db' : '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: canEdit ? 'pointer' : 'not-allowed' }}>Create Product</button>
                    </form>
                </div>

                {/* Add Stock Form */}
                <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)', opacity: canEdit ? 1 : 0.6, pointerEvents: canEdit ? 'auto' : 'none' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h3>Add Stock {selectedStoreId ? '(to Selected Store)' : '(to HQ)'}</h3>
                        {!canEdit && <span style={{ fontSize: '0.8rem', color: '#e74c3c' }}>Admin Only</span>}
                    </div>
                    <p style={{ fontSize: '0.8rem', color: '#666' }}>
                        Warning: Direct stock addition usually happens at HQ.
                    </p>
                    <form onSubmit={handleAddStock} style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1rem' }}>
                        <input disabled={!canEdit} type="text" placeholder="SKU" value={stock.sku} onChange={e => setStock({ ...stock, sku: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <input disabled={!canEdit} type="number" placeholder="Quantity" value={stock.quantity} onChange={e => setStock({ ...stock, quantity: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <button disabled={!canEdit} type="submit" style={{ padding: '10px', background: canEdit ? '#2ecc71' : '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: canEdit ? 'pointer' : 'not-allowed' }}>Add Stock</button>
                    </form>
                </div>
            </div>

            {/* Search Bar & Actions */}
            <div style={{ marginBottom: '1rem', background: 'white', padding: '10px', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center' }}>
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

                <div style={{ display: 'flex', gap: '10px' }}>
                    {/* Only show these at HQ */}
                    {!selectedStoreId && (
                        <>
                            <button
                                disabled={selectedItems.size === 0 || !canEdit}
                                onClick={() => setIsBundleModalOpen(true)}
                                style={{
                                    padding: '8px 16px',
                                    background: (selectedItems.size > 0 && canEdit) ? '#8e44ad' : '#bdc3c7',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '4px',
                                    cursor: (selectedItems.size > 0 && canEdit) ? 'pointer' : 'not-allowed'
                                }}
                            >
                                {canEdit ? `Create Bundle (${selectedItems.size})` : 'Create Bundle (Admin Only)'}
                            </button>

                            <button
                                disabled={selectedItems.size === 0 || !canEdit}
                                onClick={() => setIsTransferModalOpen(true)}
                                style={{
                                    padding: '8px 16px',
                                    background: (selectedItems.size > 0 && canEdit) ? '#3498db' : '#bdc3c7',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '4px',
                                    cursor: (selectedItems.size > 0 && canEdit) ? 'pointer' : 'not-allowed'
                                }}
                            >
                                {canEdit ? 'Transfer to Store' : 'Transfer (Admin Only)'}
                            </button>
                        </>
                    )}

                    {/* Only show/enable Return at Store View */}
                    {selectedStoreId && (
                        <button
                            disabled={selectedItems.size === 0 || !canEdit}
                            onClick={handleReturnToHQ}
                            style={{
                                padding: '8px 16px',
                                background: (selectedItems.size > 0 && canEdit) ? '#e67e22' : '#bdc3c7',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: (selectedItems.size > 0 && canEdit) ? 'pointer' : 'not-allowed'
                            }}
                        >
                            {canEdit ? 'Return to HQ' : 'Return (Admin Only)'}
                        </button>
                    )}
                </div>
            </div>

            <div style={{ marginTop: '1rem', background: 'white', padding: '1rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', color: '#333' }}>
                    <thead>
                        <tr style={{ background: '#f8f9fa', userSelect: 'none' }}>
                            <th style={{ padding: '12px', width: '40px', textAlign: 'center' }}>
                                <input
                                    type="checkbox"
                                    onChange={toggleSelectAll}
                                    checked={filteredProducts.length > 0 && selectedItems.size === filteredProducts.length}
                                />
                            </th>
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
                            <tr key={p.id || p.sku} style={{ borderBottom: '1px solid #eee', background: p.type === 'BUNDLE' ? '#fbf7ff' : 'white' }}>
                                <td style={{ padding: '12px', textAlign: 'center' }}>
                                    {p.type !== 'BUNDLE' && (
                                        <input
                                            type="checkbox"
                                            checked={selectedItems.has(p.sku)}
                                            onChange={() => toggleSelection(p.sku)}
                                        />
                                    )}
                                </td>
                                {visibleColumns.sku && <td style={{ padding: '12px' }}>{p.sku}</td>}
                                {visibleColumns.name && <td style={{ padding: '12px' }}>{p.name}</td>}
                                {visibleColumns.price && <td style={{ padding: '12px' }}>₹{p.basePrice || p.price}</td>}
                                {visibleColumns.type && <td style={{ padding: '12px' }}>
                                    <span style={{
                                        padding: '2px 6px', borderRadius: '4px', fontSize: '0.8rem',
                                        background: p.type === 'BUNDLE' ? '#9b59b6' : '#e67e22',
                                        color: 'white'
                                    }}>
                                        {p.type || 'BUNDLE'}
                                    </span>
                                </td>}

                                {visibleColumns.author && <td style={{ padding: '12px' }}>{p.attributes?.author || '-'}</td>}
                                {visibleColumns.isbn && <td style={{ padding: '12px' }}>{p.attributes?.isbn || '-'}</td>}
                                {visibleColumns.brand && <td style={{ padding: '12px' }}>{p.attributes?.brand || '-'}</td>}
                                {visibleColumns.hardness && <td style={{ padding: '12px' }}>{p.attributes?.hardness || '-'}</td>}

                                {visibleColumns.stock && <td style={{ padding: '12px', fontWeight: 'bold', color: p.quantity > 0 ? '#27ae60' : '#e74c3c' }}>
                                    {p.quantity}
                                </td>}
                                <td style={{ padding: '12px' }}></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default InventoryManager;

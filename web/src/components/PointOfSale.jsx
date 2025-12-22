import { useState, useEffect } from 'react';
import { getAllStores, getStoreInventory, getBundles, createOrder } from '../services/api';
import useInventoryFilter from '../hooks/useInventoryFilter';

const PointOfSale = ({ userId, userRole, userStoreIds }) => {
    const [stores, setStores] = useState([]);
    const [selectedStoreId, setSelectedStoreId] = useState(null);
    const [inventory, setInventory] = useState([]);
    const [bundles, setBundles] = useState([]);
    const [cart, setCart] = useState([]);
    const [customer, setCustomer] = useState({ name: '', phone: '' });
    const [view, setView] = useState('selection'); // 'selection' | 'verify'
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);

    // Use shared hook for filtering and column management
    const {
        searchQuery, setSearchQuery,
        searchField, setSearchField,
        visibleColumns, toggleColumn,
        isColumnSelectorOpen, setIsColumnSelectorOpen,
        filteredItems: filteredInventory
    } = useInventoryFilter(inventory);

    // Load stores on mount
    useEffect(() => {
        loadStores();
        loadBundles();
    }, []);

    // Load inventory when store selection changes
    useEffect(() => {
        if (selectedStoreId) {
            loadInventory(selectedStoreId);
        } else {
            setInventory([]);
        }
    }, [selectedStoreId]);

    const loadStores = async () => {
        try {
            setLoading(true);
            const allStores = await getAllStores();

            // Filter stores based on user role
            let availableStores;
            if (userRole === 'SUPER_ADMIN') {
                availableStores = allStores;
            } else if (userStoreIds && userStoreIds.length > 0) {
                availableStores = allStores.filter(s => userStoreIds.includes(s.id));
            } else {
                availableStores = [];
            }

            setStores(availableStores);

            // Auto-select first store if available
            if (availableStores.length > 0 && !selectedStoreId) {
                setSelectedStoreId(availableStores[0].id);
            }
        } catch (err) {
            setError('Failed to load stores: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadInventory = async (storeId) => {
        try {
            setLoading(true);
            // Load ALL inventory for client-side filtering
            const stockLevels = await getStoreInventory(storeId, "");
            setInventory(stockLevels);
        } catch (err) {
            setError('Failed to load inventory: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadBundles = async () => {
        try {
            const bundlesData = await getBundles();
            setBundles(bundlesData);
        } catch (err) {
            console.error('Failed to load bundles:', err);
        }
    };

    const addToCart = (item, isBundle = false) => {
        const product = isBundle ? null : item.product;
        const bundle = isBundle ? item : null;
        const sku = isBundle ? `BUNDLE-${bundle.id}` : product.sku;

        const newCart = [...cart];
        const existing = newCart.find(i => i.sku === sku);

        // Check stock availability
        const availableStock = isBundle ? Infinity : item.quantity;

        if (existing) {
            // For bundles, we might not track stock strictly here, or we'd need to check components
            if (!isBundle && existing.quantity >= availableStock) {
                setError(`Only ${availableStock} units available`);
                return;
            }
            existing.quantity += 1;
        } else {
            if (!isBundle && availableStock <= 0) {
                setError('Product out of stock');
                return;
            }

            newCart.push({
                sku: sku,
                name: isBundle ? bundle.name : product.name,
                basePrice: isBundle ? bundle.discountPrice : product.basePrice,
                quantity: 1,
                product: product,
                bundle: bundle,
                isBundle: isBundle
            });
        }
        setCart(newCart);
        setError('');
    };

    // Helper for bundle quick add
    const addBundleToCart = (bundle) => {
        addToCart(bundle, true);
    }


    const removeFromCart = (sku) => {
        setCart(cart.filter(i => i.sku !== sku));
    };

    const calculateTotal = () => {
        return cart.reduce((sum, item) => sum + (item.basePrice * item.quantity), 0);
    };

    const handleVerify = () => {
        if (cart.length === 0) {
            setError('Cart is empty');
            return;
        }
        if (!customer.name) {
            setError('Customer Name is required');
            return;
        }
        if (!selectedStoreId) {
            setError('Please select a store');
            return;
        }
        setError('');
        setView('verify');
    };

    const handleSubmit = async () => {
        try {
            const orderItems = cart.map(item => ({
                sku: item.isBundle ? null : item.product.sku,
                bundleId: item.isBundle ? item.bundle.id : null,
                quantity: item.quantity
            }));

            const orderPayload = {
                storeId: selectedStoreId,
                customerName: customer.name,
                customerPhone: customer.phone,
                items: orderItems
            };

            await createOrder(orderPayload);
            setSuccess('Order created successfully!');
            setCart([]);
            setCustomer({ name: '', phone: '' });
            setView('selection');
            setTimeout(() => setSuccess(''), 3000);

            // Reload inventory to reflect updated stock
            loadInventory(selectedStoreId);
        } catch (err) {
            setError('Failed to create order: ' + (err.response?.data?.message || err.message));
            setView('selection');
        }
    };

    // Helper to render product cells based on visibility
    const renderProductCell = (product, col) => {
        if (!product) return '-';
        switch (col) {
            case 'sku': return product.sku;
            case 'name': return product.name;
            case 'price': return `₹${product.basePrice}`;
            case 'type': return product.type || 'PRODUCT';
            case 'author': return product.attributes?.author || '-';
            case 'isbn': return product.attributes?.isbn || '-';
            case 'brand': return product.attributes?.brand || '-';
            case 'hardness': return product.attributes?.hardness || '-';
            case 'stock': return null; // Handled separately
            default: return null;
        }
    };

    if (view === 'verify') {
        return (
            <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', maxWidth: '600px', margin: 'auto' }}>
                <h2>Verify Sale</h2>
                <p><strong>Store:</strong> {stores.find(s => s.id === selectedStoreId)?.name || 'Unknown'}</p>
                <p><strong>Customer:</strong> {customer.name} {customer.phone && `(${customer.phone})`}</p>
                <hr />
                <table style={{ width: '100%', marginBottom: '1rem' }}>
                    <thead>
                        <tr style={{ textAlign: 'left' }}><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr>
                    </thead>
                    <tbody>
                        {cart.map(item => (
                            <tr key={item.sku}>
                                <td>{item.name}</td>
                                <td>{item.quantity}</td>
                                <td>₹{item.basePrice}</td>
                                <td>₹{(item.basePrice * item.quantity).toFixed(2)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <h3 style={{ textAlign: 'right' }}>Total: ₹{calculateTotal().toFixed(2)}</h3>

                <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
                    <button onClick={() => setView('selection')} style={{ flex: 1, padding: '10px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Back</button>
                    <button onClick={handleSubmit} style={{ flex: 1, padding: '10px', background: '#27ae60', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}>Confirm Sale</button>
                </div>
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', height: '85vh' }}>
            {/* Store Selector & Search */}
            <div style={{ background: 'white', padding: '1rem', borderRadius: '8px', display: 'flex', gap: '1rem', alignItems: 'center' }}>
                <div style={{ flex: 1 }}>
                    <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Select Store:</label>
                    <select
                        value={selectedStoreId || ''}
                        onChange={(e) => setSelectedStoreId(Number(e.target.value))}
                        style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                        disabled={stores.length <= 1}
                    >
                        {stores.length === 0 && <option>No stores available</option>}
                        {stores.map(store => (
                            <option key={store.id} value={store.id}>{store.name}</option>
                        ))}
                    </select>
                </div>

                {/* Enhanced Search Bar */}
                <div style={{ flex: 2, display: 'flex', flexDirection: 'column' }}>
                    <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Search Inventory:</label>
                    <div style={{ display: 'flex', gap: '10px' }}>
                        <select
                            value={searchField}
                            onChange={e => setSearchField(e.target.value)}
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd', width: '120px' }}
                        >
                            <option value="all">All Fields</option>
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
                            onChange={(e) => setSearchQuery(e.target.value)}
                            style={{ flex: 1, padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                        />
                    </div>
                </div>
            </div>

            {error && <div style={{ color: '#c0392b', background: '#fadbd8', padding: '1rem', borderRadius: '4px' }}>{error}</div>}
            {success && <div style={{ color: '#27ae60', background: '#d5f4e6', padding: '1rem', borderRadius: '4px' }}>{success}</div>}

            <div style={{ display: 'flex', gap: '1rem', flex: 1, overflow: 'hidden' }}>
                {/* Left: Inventory Table with Dynamic Columns */}
                <div style={{ flex: 2, background: 'white', borderRadius: '8px', padding: '1rem', display: 'flex', flexDirection: 'column' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                        <h3>Available Inventory</h3>
                    </div>

                    {loading && <p>Loading...</p>}

                    {!selectedStoreId && <p style={{ color: '#7f8c8d' }}>Please select a store to view inventory</p>}

                    {selectedStoreId && filteredInventory.length === 0 && !loading && (
                        <p style={{ color: '#7f8c8d' }}>No inventory found.</p>
                    )}

                    {selectedStoreId && filteredInventory.length > 0 && (
                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                <thead style={{ position: 'sticky', top: 0, background: '#ecf0f1', zIndex: 1 }}>
                                    <tr>
                                        {/* Dynamic Headers */}
                                        {Object.keys(visibleColumns).map(col => visibleColumns[col] && (
                                            <th key={col} style={{ padding: '10px', textAlign: col === 'price' || col === 'stock' ? 'right' : 'left', borderBottom: '2px solid #bdc3c7' }}>
                                                {col.charAt(0).toUpperCase() + col.slice(1)}
                                            </th>
                                        ))}

                                        <th style={{ padding: '10px', textAlign: 'center', borderBottom: '2px solid #bdc3c7', position: 'relative' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px' }}>
                                                Action
                                                <button
                                                    onClick={() => setIsColumnSelectorOpen(!isColumnSelectorOpen)}
                                                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.2rem', padding: '0' }}
                                                    title="Select Columns"
                                                >
                                                    ⚙️
                                                </button>
                                            </div>
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
                                                    zIndex: 100, // High z-index to float over table
                                                    width: '200px',
                                                    textAlign: 'left',
                                                    color: 'black',
                                                    fontWeight: 'normal'
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
                                    {filteredInventory.map(stockLevel => (
                                        <tr key={stockLevel.product.sku} style={{ borderBottom: '1px solid #ecf0f1' }}>

                                            {Object.keys(visibleColumns).map(col => {
                                                if (!visibleColumns[col]) return null;
                                                if (col === 'stock') {
                                                    return (
                                                        <td key={col} style={{ padding: '10px', textAlign: 'right', color: stockLevel.quantity > 0 ? '#27ae60' : '#e74c3c' }}>
                                                            {stockLevel.quantity}
                                                        </td>
                                                    );
                                                }
                                                return (
                                                    <td key={col} style={{ padding: '10px', textAlign: col === 'price' ? 'right' : 'left' }}>
                                                        {renderProductCell(stockLevel.product, col)}
                                                    </td>
                                                );
                                            })}

                                            <td style={{ padding: '10px', textAlign: 'center' }}>
                                                <button
                                                    onClick={() => addToCart(stockLevel)}
                                                    disabled={stockLevel.quantity === 0}
                                                    style={{
                                                        padding: '5px 15px',
                                                        background: stockLevel.quantity > 0 ? '#3498db' : '#bdc3c7',
                                                        color: 'white',
                                                        border: 'none',
                                                        borderRadius: '4px',
                                                        cursor: stockLevel.quantity > 0 ? 'pointer' : 'not-allowed'
                                                    }}
                                                >
                                                    Add
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* Bundles Section */}
                    {bundles.length > 0 && (
                        <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '2px solid #ecf0f1' }}>
                            <h4>Quick Bundles</h4>
                            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                {bundles.map(bundle => (
                                    <button
                                        key={bundle.id}
                                        onClick={() => addBundleToCart(bundle)}
                                        style={{
                                            padding: '8px 12px',
                                            background: '#9b59b6',
                                            color: 'white',
                                            border: 'none',
                                            borderRadius: '4px',
                                            cursor: 'pointer',
                                            fontSize: '0.9rem'
                                        }}
                                    >
                                        {bundle.name}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                {/* Right: Cart & Customer */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 10px rgba(0,0,0,0.1)' }}>
                    <h3>Current Sale</h3>

                    <div style={{ marginBottom: '1rem' }}>
                        <input
                            placeholder="Customer Name *"
                            value={customer.name}
                            onChange={e => setCustomer({ ...customer, name: e.target.value })}
                            style={{ width: '100%', padding: '8px', marginBottom: '0.5rem', borderRadius: '4px', border: '1px solid #ddd' }}
                        />
                        <input
                            placeholder="Phone Number (Optional)"
                            value={customer.phone}
                            onChange={e => setCustomer({ ...customer, phone: e.target.value })}
                            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                        />
                    </div>

                    <div style={{ flex: 1, overflowY: 'auto', borderTop: '1px solid #eee', borderBottom: '1px solid #eee', padding: '1rem 0' }}>
                        {cart.length === 0 && <p style={{ color: '#95a5a6', textAlign: 'center' }}>Cart is empty</p>}
                        {cart.map(item => (
                            <div key={item.sku} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', alignItems: 'center' }}>
                                <div>
                                    <div style={{ fontWeight: 'bold' }}>{item.name}</div>
                                    <div style={{ fontSize: '0.8rem', color: '#95a5a6' }}>₹{item.basePrice} x {item.quantity}</div>
                                    {item.isBundle && <span style={{ fontSize: '0.7em', color: 'purple', marginLeft: '5px' }}>(Bundle)</span>}
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <span>₹{(item.basePrice * item.quantity).toFixed(2)}</span>
                                    <button onClick={() => removeFromCart(item.sku)} style={{ background: 'none', border: 'none', color: '#e74c3c', cursor: 'pointer', fontSize: '1.5rem' }}>×</button>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div style={{ marginTop: '1rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '1rem' }}>
                            <span>Total</span>
                            <span>₹{calculateTotal().toFixed(2)}</span>
                        </div>
                        <button onClick={handleVerify} style={{ width: '100%', padding: '12px', background: '#2c3e50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '1rem' }}>
                            Proceed to Checkout
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PointOfSale;

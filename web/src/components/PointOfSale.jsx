import { useState, useEffect } from 'react';
import { getAllStores, getStoreInventory, getBundles, createOrder } from '../services/api';

const PointOfSale = ({ userId, userRole, userStoreIds }) => {
    const [stores, setStores] = useState([]);
    const [selectedStoreId, setSelectedStoreId] = useState(null);
    const [inventory, setInventory] = useState([]);
    const [bundles, setBundles] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [cart, setCart] = useState([]);
    const [customer, setCustomer] = useState({ name: '', phone: '' });
    const [view, setView] = useState('selection'); // 'selection' | 'verify'
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);

    // Load stores on mount
    useEffect(() => {
        loadStores();
        loadBundles();
    }, []);

    // Load inventory when store selection changes
    useEffect(() => {
        if (selectedStoreId) {
            loadInventory(selectedStoreId, searchQuery);
        }
    }, [selectedStoreId, searchQuery]);

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

    const loadInventory = async (storeId, search = '') => {
        try {
            setLoading(true);
            const stockLevels = await getStoreInventory(storeId, search);
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

    const addToCart = (stockLevel) => {
        const product = stockLevel.product;
        const newCart = [...cart];

        const existing = newCart.find(i => i.sku === product.sku);
        if (existing) {
            if (existing.quantity < stockLevel.quantity) {
                existing.quantity += 1;
            } else {
                setError(`Only ${stockLevel.quantity} units available`);
                return;
            }
        } else {
            if (stockLevel.quantity > 0) {
                newCart.push({ ...product, quantity: 1, availableStock: stockLevel.quantity });
            } else {
                setError('Product out of stock');
                return;
            }
        }
        setCart(newCart);
        setError('');
    };

    const addBundleToCart = (bundle) => {
        const newCart = [...cart];

        bundle.items.forEach(bundleItem => {
            const stockLevel = inventory.find(s => s.product.sku === bundleItem.productSku);
            if (stockLevel && stockLevel.product) {
                const product = stockLevel.product;
                const existing = newCart.find(i => i.sku === product.sku);
                if (existing) {
                    existing.quantity += bundleItem.quantity;
                } else {
                    newCart.push({ ...product, quantity: bundleItem.quantity, availableStock: stockLevel.quantity });
                }
            }
        });

        setCart(newCart);
    };

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
            const orderPayload = {
                storeId: selectedStoreId,
                customerName: customer.name,
                customerPhone: customer.phone,
                items: cart.map(i => ({ sku: i.sku, quantity: i.quantity }))
            };

            await createOrder(orderPayload);
            setSuccess('Order created successfully!');
            setCart([]);
            setCustomer({ name: '', phone: '' });
            setView('selection');
            setTimeout(() => setSuccess(''), 3000);

            // Reload inventory to reflect updated stock
            loadInventory(selectedStoreId, searchQuery);
        } catch (err) {
            setError('Failed to create order: ' + (err.response?.data?.message || err.message));
            setView('selection');
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
                <div style={{ flex: 1 }}>
                    <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Search Inventory:</label>
                    <input
                        type="text"
                        placeholder="Search by name, SKU..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                    />
                </div>
            </div>

            {error && <div style={{ color: '#c0392b', background: '#fadbd8', padding: '1rem', borderRadius: '4px' }}>{error}</div>}
            {success && <div style={{ color: '#27ae60', background: '#d5f4e6', padding: '1rem', borderRadius: '4px' }}>{success}</div>}

            <div style={{ display: 'flex', gap: '1rem', flex: 1, overflow: 'hidden' }}>
                {/* Left: Inventory Table */}
                <div style={{ flex: 2, background: 'white', borderRadius: '8px', padding: '1rem', display: 'flex', flexDirection: 'column' }}>
                    <h3>Available Inventory</h3>

                    {loading && <p>Loading...</p>}

                    {!selectedStoreId && <p style={{ color: '#7f8c8d' }}>Please select a store to view inventory</p>}

                    {selectedStoreId && inventory.length === 0 && !loading && (
                        <p style={{ color: '#7f8c8d' }}>No inventory found{searchQuery && ' for this search'}</p>
                    )}

                    {selectedStoreId && inventory.length > 0 && (
                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                <thead style={{ position: 'sticky', top: 0, background: '#ecf0f1' }}>
                                    <tr>
                                        <th style={{ padding: '10px', textAlign: 'left', borderBottom: '2px solid #bdc3c7' }}>SKU</th>
                                        <th style={{ padding: '10px', textAlign: 'left', borderBottom: '2px solid #bdc3c7' }}>Product Name</th>
                                        <th style={{ padding: '10px', textAlign: 'right', borderBottom: '2px solid #bdc3c7' }}>Price</th>
                                        <th style={{ padding: '10px', textAlign: 'right', borderBottom: '2px solid #bdc3c7' }}>Stock</th>
                                        <th style={{ padding: '10px', textAlign: 'center', borderBottom: '2px solid #bdc3c7' }}>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {inventory.map(stockLevel => (
                                        <tr key={stockLevel.product.sku} style={{ borderBottom: '1px solid #ecf0f1' }}>
                                            <td style={{ padding: '10px' }}>{stockLevel.product.sku}</td>
                                            <td style={{ padding: '10px' }}>{stockLevel.product.name}</td>
                                            <td style={{ padding: '10px', textAlign: 'right' }}>₹{stockLevel.product.basePrice}</td>
                                            <td style={{ padding: '10px', textAlign: 'right', color: stockLevel.quantity > 0 ? '#27ae60' : '#e74c3c' }}>
                                                {stockLevel.quantity}
                                            </td>
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
                                    <div>{item.name}</div>
                                    <div style={{ fontSize: '0.8rem', color: '#95a5a6' }}>₹{item.basePrice} x {item.quantity}</div>
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

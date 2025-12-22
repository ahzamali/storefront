import { useState, useEffect } from 'react';
import { getAllProducts, getBundles, createOrder } from '../services/api';

const PointOfSale = ({ userStoreId }) => { // Expect userStoreId from parent
    const [products, setProducts] = useState([]);
    const [bundles, setBundles] = useState([]);
    const [cart, setCart] = useState([]);
    const [customer, setCustomer] = useState({ name: '', phone: '' });
    const [view, setView] = useState('selection'); // 'selection' | 'verify'
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [currentStoreId, setCurrentStoreId] = useState(userStoreId);

    useEffect(() => {
        loadInventory();
        // If userStoreId is missing (e.g. Super Admin), explicitly set or prompt (omitted for brevity, defaulting to generic/null if not passed)
    }, []);

    const loadInventory = async () => {
        try {
            const [pData, bData] = await Promise.all([getAllProducts(), getBundles()]);
            setProducts(pData);
            setBundles(bData);
        } catch (err) {
            setError('Failed to load inventory.');
        }
    };

    const addToCart = (item, type = 'PRODUCT') => {
        const newCart = [...cart];

        if (type === 'BUNDLE') {
            // Explode bundle
            item.items.forEach(bundleItem => {
                const product = products.find(p => p.sku === bundleItem.productSku);
                if (product) {
                    processCartItem(newCart, product, bundleItem.quantity);
                }
            });
        } else {
            processCartItem(newCart, item, 1);
        }
        setCart(newCart);
    };

    const processCartItem = (cartList, product, qty) => {
        const existing = cartList.find(i => i.sku === product.sku);
        if (existing) {
            existing.quantity += qty;
        } else {
            cartList.push({ ...product, quantity: qty });
        }
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
        setError('');
        setView('verify');
    };

    const handleSubmit = async () => {
        try {
            const orderPayload = {
                storeId: currentStoreId, // Currently relying on prop, or user must select if null
                customerName: customer.name,
                customerPhone: customer.customerPhone, // Typo correction in next edit if needed, verifying DTO
                items: cart.map(i => ({ productSku: i.sku, quantity: i.quantity }))
            };

            // Fix for phone key mismatch
            orderPayload.customerPhone = customer.phone;

            await createOrder(orderPayload);
            setSuccess('Order created successfully!');
            setCart([]);
            setCustomer({ name: '', phone: '' });
            setView('selection');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError('Failed to create order: ' + (err.response?.data?.message || err.message));
            setView('selection');
        }
    };

    if (view === 'verify') {
        return (
            <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', maxWidth: '600px', margin: 'auto' }}>
                <h2>Verify Sale</h2>
                <p><strong>Customer:</strong> {customer.name} ({customer.phone})</p>
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
                                <td>${item.basePrice}</td>
                                <td>${(item.basePrice * item.quantity).toFixed(2)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <h3 style={{ textAlign: 'right' }}>Total: ${calculateTotal().toFixed(2)}</h3>

                <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
                    <button onClick={() => setView('selection')} style={{ flex: 1, padding: '10px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Back</button>
                    <button onClick={handleSubmit} style={{ flex: 1, padding: '10px', background: '#27ae60', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}>Confirm Sale</button>
                </div>
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', gap: '2rem', height: '80vh' }}>
            {/* Left: Inventory */}
            <div style={{ flex: 2, overflowY: 'auto' }}>
                <h3>Select Items</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: '1rem' }}>
                    {/* Products */}
                    {products.map(p => (
                        <div key={p.id} onClick={() => addToCart(p)} style={{ padding: '10px', border: '1px solid #ddd', borderRadius: '8px', cursor: 'pointer', background: 'white', transition: 'box-shadow 0.2s' }}>
                            <div style={{ fontWeight: 'bold' }}>{p.name}</div>
                            <div style={{ color: '#7f8c8d', fontSize: '0.9rem' }}>${p.basePrice}</div>
                            <div style={{ fontSize: '0.8rem', color: '#bdc3c7' }}>Product</div>
                        </div>
                    ))}
                    {/* Bundles */}
                    {bundles.map(b => (
                        <div key={'b' + b.id} onClick={() => addToCart(b, 'BUNDLE')} style={{ padding: '10px', border: '2px solid #3498db', borderRadius: '8px', cursor: 'pointer', background: '#ebf5fb' }}>
                            <div style={{ fontWeight: 'bold', color: '#2980b9' }}>{b.name}</div>
                            <div style={{ color: '#7f8c8d', fontSize: '0.9rem' }}>Bundle</div>
                            <div style={{ fontSize: '0.8rem' }}>Contains {b.items.length} items</div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Right: Cart & Customer */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 10px rgba(0,0,0,0.1)' }}>
                <h3>Current Sale</h3>
                {error && <div style={{ color: '#c0392b', marginBottom: '1rem' }}>{error}</div>}
                {success && <div style={{ color: '#27ae60', marginBottom: '1rem' }}>{success}</div>}

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
                    {cart.map(item => (
                        <div key={item.sku} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                            <div>
                                <div>{item.name}</div>
                                <div style={{ fontSize: '0.8rem', color: '#95a5a6' }}>${item.basePrice} x {item.quantity}</div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span>${(item.basePrice * item.quantity).toFixed(2)}</span>
                                <button onClick={() => removeFromCart(item.sku)} style={{ background: 'none', border: 'none', color: '#e74c3c', cursor: 'pointer' }}>×</button>
                            </div>
                        </div>
                    ))}
                </div>

                <div style={{ marginTop: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '1rem' }}>
                        <span>Total</span>
                        <span>${calculateTotal().toFixed(2)}</span>
                    </div>
                    <button onClick={handleVerify} style={{ width: '100%', padding: '12px', background: '#2c3e50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '1rem' }}>
                        Proceed via POS
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PointOfSale;

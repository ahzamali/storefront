import { useState, useEffect } from 'react';
import { getOrders, getStores } from '../services/api';

const OrderManager = () => {
    const [orders, setOrders] = useState([]);
    const [filteredOrders, setFilteredOrders] = useState([]);
    const [stores, setStores] = useState([]);
    const [selectedStoreId, setSelectedStoreId] = useState('');
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [viewOrder, setViewOrder] = useState(null); // Selected order for detail view

    useEffect(() => {
        loadStores();
        loadOrders();
    }, []);

    // Reload orders when store selection changes
    useEffect(() => {
        loadOrders();
    }, [selectedStoreId]);

    useEffect(() => {
        if (!searchQuery) {
            setFilteredOrders(orders);
        } else {
            const lowerQuery = searchQuery.toLowerCase();
            const filtered = orders.filter(order =>
                order.id.toString().includes(lowerQuery) ||
                (order.customer && order.customer.name && order.customer.name.toLowerCase().includes(lowerQuery)) ||
                (order.store && order.store.name.toLowerCase().includes(lowerQuery))
            );
            setFilteredOrders(filtered);
        }
    }, [searchQuery, orders]);

    const loadStores = async () => {
        try {
            const data = await getStores();
            setStores(data);
        } catch (e) {
            console.error("Failed to load stores");
        }
    };

    const loadOrders = async () => {
        try {
            setLoading(true);
            const data = await getOrders(selectedStoreId);
            setOrders(data);
            setFilteredOrders(data);
        } catch (err) {
            setError('Failed to load orders.');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div>Loading orders...</div>;
    if (error) return <div style={{ color: 'red' }}>{error}</div>;

    return (
        <div style={{ padding: '20px' }}>
            <h2>Order Dashboard</h2>

            {/* Search and Filter */}
            <div style={{ marginBottom: '20px', display: 'flex', gap: '10px' }}>
                <select
                    value={selectedStoreId}
                    onChange={(e) => setSelectedStoreId(e.target.value)}
                    style={{ padding: '10px', borderRadius: '4px', border: '1px solid #ddd' }}
                >
                    <option value="">All Stores</option>
                    {stores.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.type})</option>
                    ))}
                </select>
                <input
                    type="text"
                    placeholder="Search by ID, Customer, or Store..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    style={{ padding: '10px', width: '300px', borderRadius: '4px', border: '1px solid #ddd' }}
                />
            </div>

            {/* Orders Table */}
            <div style={{ overflowX: 'auto', background: 'white', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px', color: '#333' }}>
                    <thead>
                        <tr style={{ background: '#f8f9fa', color: '#666', textAlign: 'left', borderBottom: '2px solid #eee' }}>
                            <th style={{ padding: '12px' }}>Order ID</th>
                            <th style={{ padding: '12px' }}>Date</th>
                            <th style={{ padding: '12px' }}>Store</th>
                            <th style={{ padding: '12px' }}>Customer</th>
                            <th style={{ padding: '12px' }}>Items</th>
                            <th style={{ padding: '12px' }}>Total Amount</th>
                            <th style={{ padding: '12px' }}>Discount</th>
                            <th style={{ padding: '12px' }}>Status</th>
                            <th style={{ padding: '12px' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredOrders.length > 0 ? (
                            filteredOrders.map(order => (
                                <tr key={order.id} style={{ borderBottom: '1px solid #eee' }}>
                                    <td style={{ padding: '12px' }}>#{order.id}</td>
                                    <td style={{ padding: '12px' }}>{new Date(order.createdAt).toLocaleString()}</td>
                                    <td style={{ padding: '12px' }}>
                                        <span style={{ background: '#e3f2fd', color: '#1565c0', padding: '4px 8px', borderRadius: '4px', fontSize: '0.9em' }}>
                                            {order.store.name}
                                        </span>
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        {order.customer ? (
                                            <div>
                                                <div>{order.customer.name}</div>
                                                <div style={{ fontSize: '0.8em', color: '#888' }}>{order.customer.phone}</div>
                                            </div>
                                        ) : (
                                            <span style={{ color: '#aaa' }}>Guest</span>
                                        )}
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        {order.orderLines && order.orderLines.length > 0 ? (
                                            <div style={{ fontSize: '0.9em' }}>
                                                {order.orderLines.length} item(s)
                                            </div>
                                        ) : '-'}
                                    </td>
                                    <td style={{ padding: '12px', fontWeight: 'bold' }}>₹{order.totalAmount}</td>
                                    <td style={{ padding: '12px', color: '#e74c3c' }}>
                                        {order.discount && order.discount > 0 ? `-₹${order.discount}` : '-'}
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        <span style={{
                                            background: order.status === 'COMPLETED' ? '#d4edda' : '#fff3cd',
                                            color: order.status === 'COMPLETED' ? '#155724' : '#856404',
                                            padding: '4px 8px',
                                            borderRadius: '4px',
                                            fontSize: '0.9em'
                                        }}>
                                            {order.status}
                                        </span>
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        <button
                                            onClick={() => setViewOrder(order)}
                                            style={{
                                                padding: '5px 10px',
                                                background: '#3498db',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer',
                                                fontSize: '0.9em'
                                            }}
                                        >
                                            View
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="9" style={{ padding: '20px', textAlign: 'center', color: '#888' }}>No orders found.</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Order Detail Modal */}
            {viewOrder && (
                <div style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    background: 'rgba(0,0,0,0.5)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 1000
                }}>
                    <div style={{
                        background: 'white',
                        padding: '20px',
                        borderRadius: '8px',
                        width: '80%',
                        maxWidth: '800px',
                        maxHeight: '90vh',
                        overflowY: 'auto',
                        boxShadow: '0 4px 10px rgba(0,0,0,0.1)',
                        textAlign: 'left' // Explicit left alignment
                    }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid #eee', paddingBottom: '10px' }}>
                            <h3 style={{ margin: 0, color: '#333' }}>Order #{viewOrder.id}</h3>
                            <button
                                onClick={() => setViewOrder(null)}
                                style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: '#666' }}
                            >
                                ×
                            </button>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px', textAlign: 'left' }}>
                            <div>
                                <h4 style={{ color: '#555', marginBottom: '10px' }}>Order Info</h4>
                                <p style={{ margin: '5px 0', color: '#333' }}><strong>Date:</strong> {new Date(viewOrder.createdAt).toLocaleString()}</p>
                                <p style={{ margin: '5px 0', color: '#333' }}><strong>Status:</strong> {viewOrder.status}</p>
                                <p style={{ margin: '5px 0', color: '#333' }}><strong>Store:</strong> {viewOrder.store.name}</p>
                            </div>
                            <div>
                                <h4 style={{ color: '#555', marginBottom: '10px' }}>Customer Info</h4>
                                <p style={{ margin: '5px 0', color: '#333' }}><strong>Name:</strong> {viewOrder.customer ? viewOrder.customer.name : 'Guest'}</p>
                                <p style={{ margin: '5px 0', color: '#333' }}><strong>Phone:</strong> {viewOrder.customer ? viewOrder.customer.phone : '-'}</p>
                            </div>
                        </div>

                        <h4 style={{ color: '#555', marginBottom: '10px', textAlign: 'left' }}>Items</h4>
                        <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px', color: '#333', border: '1px solid #eee' }}>
                            <thead style={{ background: '#f8f9fa' }}>
                                <tr>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Item</th>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>SKU</th>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Qty</th>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Price</th>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Total</th>
                                </tr>
                            </thead>
                            <tbody>
                                {viewOrder.orderLines && viewOrder.orderLines.map((line, idx) => (
                                    <tr key={idx} style={{ borderBottom: '1px solid #eee' }}>
                                        <td style={{ padding: '10px' }}>
                                            {line.product ? line.product.name : (line.bundle ? line.bundle.name + ' (Bundle)' : 'Unknown')}
                                        </td>
                                        <td style={{ padding: '10px' }}>
                                            {line.product ? line.product.sku : (line.bundle ? 'BUNDLE' : '-')}
                                        </td>
                                        <td style={{ padding: '10px', textAlign: 'left' }}>{line.quantity}</td>
                                        <td style={{ padding: '10px', textAlign: 'left' }}>₹{line.unitPrice}</td>
                                        <td style={{ padding: '10px', textAlign: 'left' }}>₹{(line.unitPrice * line.quantity).toFixed(2)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        <div style={{ textAlign: 'left', borderTop: '1px solid #eee', paddingTop: '15px' }}>
                            {viewOrder.discount && viewOrder.discount > 0 && (
                                <p style={{ margin: '5px 0', color: '#e74c3c' }}>Discount: -₹{viewOrder.discount}</p>
                            )}
                            <h3 style={{ margin: '10px 0', color: '#333' }}>Total: ₹{viewOrder.totalAmount}</h3>
                        </div>

                        <div style={{ textAlign: 'left', marginTop: '20px' }}>
                            <button
                                onClick={() => setViewOrder(null)}
                                style={{
                                    padding: '10px 20px',
                                    background: '#95a5a6',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '4px',
                                    cursor: 'pointer'
                                }}
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OrderManager;

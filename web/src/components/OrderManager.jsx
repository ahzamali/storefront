import { useState, useEffect } from 'react';
import { getOrders } from '../services/api';

const OrderManager = () => {
    const [orders, setOrders] = useState([]);
    const [filteredOrders, setFilteredOrders] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        loadOrders();
    }, []);

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

    const loadOrders = async () => {
        try {
            setLoading(true);
            const data = await getOrders();
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

            {/* Search */}
            <div style={{ marginBottom: '20px' }}>
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
                            <th style={{ padding: '12px' }}>Status</th>
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
                                    <td style={{ padding: '12px', fontWeight: 'bold' }}>${order.totalAmount}</td>
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
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="7" style={{ padding: '20px', textAlign: 'center', color: '#888' }}>No orders found.</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default OrderManager;

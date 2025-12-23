import React, { useState, useEffect } from 'react';
import { getReconciliationReport, getStores } from '../services/api';

const ReconciliationReport = () => {
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [storeId, setStoreId] = useState(''); // Optional: for filtering if user has multiple stores
    const [stores, setStores] = useState([]);

    useEffect(() => {
        loadStores();
    }, []);

    useEffect(() => {
        fetchReport();
    }, [storeId]);

    const loadStores = async () => {
        try {
            const data = await getStores();
            setStores(data);
        } catch (e) {
            console.error(e);
        }
    }

    const fetchReport = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getReconciliationReport(storeId);
            setReport(data);
        } catch (err) {
            console.error("Error fetching reconciliation report:", err);
            setError("Failed to load report data.");
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div>Loading Report...</div>;
    if (error) return <div style={{ color: 'red' }}>{error}</div>;

    const cardStyle = {
        background: 'white',
        padding: '20px',
        borderRadius: '8px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        flex: 1,
        minWidth: '200px',
        textAlign: 'center'
    };

    const valueStyle = {
        fontSize: '1.5rem',
        fontWeight: 'bold',
        color: '#2c3e50',
        marginTop: '10px'
    };

    const labelStyle = {
        color: '#7f8c8d',
        textTransform: 'uppercase',
        fontSize: '0.85rem',
        letterSpacing: '1px'
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ margin: 0 }}>Reconciliation Report</h2>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <label style={{ fontWeight: 'bold' }}>Store:</label>
                    <select
                        value={storeId}
                        onChange={(e) => setStoreId(e.target.value)}
                        style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                    >
                        <option value="">All Stores (Aggregate)</option>
                        {stores.map(store => (
                            <option key={store.id} value={store.id}>{store.name}</option>
                        ))}
                    </select>
                </div>
            </div>

            {!report ? <div>No data available</div> : (
                <>
                    <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', marginBottom: '30px' }}>
                        <div style={cardStyle}>
                            <div style={labelStyle}>Total Orders</div>
                            <div style={valueStyle}>{report.totalOrders}</div>
                        </div>
                        <div style={cardStyle}>
                            <div style={labelStyle}>Total Amount</div>
                            <div style={valueStyle}>₹{report.totalAmount?.toFixed(2)}</div>
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', marginBottom: '30px' }}>
                        <div style={cardStyle}>
                            <div style={labelStyle}>Reconciled Orders</div>
                            <div style={{ ...valueStyle, color: '#27ae60' }}>{report.reconciledOrders}</div>
                        </div>
                        <div style={cardStyle}>
                            <div style={labelStyle}>Reconciled Amount</div>
                            <div style={{ ...valueStyle, color: '#27ae60' }}>₹{report.reconciledAmount?.toFixed(2)}</div>
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                        <div style={cardStyle}>
                            <div style={labelStyle}>Unreconciled Orders</div>
                            <div style={{ ...valueStyle, color: '#c0392b' }}>{report.unreconciledOrders}</div>
                        </div>
                        <div style={cardStyle}>
                            <div style={labelStyle}>Unreconciled Amount</div>
                            <div style={{ ...valueStyle, color: '#c0392b' }}>₹{report.unreconciledAmount?.toFixed(2)}</div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default ReconciliationReport;

import { useState, useEffect } from 'react';
import { getStores, createStore, allocateStock, reconcileStore, getReconciliationHistory, getReconciliationReport } from '../services/api';

const StoreManager = () => {
    const [stores, setStores] = useState([]);
    const [newStoreName, setNewStoreName] = useState('');
    const [selectedStore, setSelectedStore] = useState(null);
    const [allocationSku, setAllocationSku] = useState('');
    const [allocationQty, setAllocationQty] = useState(1);

    // Reconcile Modal State
    const [reconcileModalOpen, setReconcileModalOpen] = useState(false);
    const [reconcileStoreId, setReconcileStoreId] = useState(null);
    const [returnStock, setReturnStock] = useState(false);

    // History Modal State
    const [historyModalOpen, setHistoryModalOpen] = useState(false);
    const [historyLogs, setHistoryLogs] = useState([]);
    const [selectedHistoryStoreName, setSelectedHistoryStoreName] = useState('');

    // Report Modal State
    const [reportModalOpen, setReportModalOpen] = useState(false);
    const [currentReport, setCurrentReport] = useState(null);
    const [reportLoading, setReportLoading] = useState(false);
    const [selectedReportStoreName, setSelectedReportStoreName] = useState('');

    // Details Modal State
    const [detailsModalOpen, setDetailsModalOpen] = useState(false);
    const [selectedLog, setSelectedLog] = useState(null);

    useEffect(() => {
        loadStores();
    }, []);

    const loadStores = async () => {
        try {
            const data = await getStores();
            setStores(data);
        } catch (e) {
            console.error(e);
        }
    }

    const handleCreate = async () => {
        if (!newStoreName) return;
        await createStore(newStoreName);
        setNewStoreName('');
        loadStores();
    }

    const handleAllocate = async () => {
        if (!selectedStore || !allocationSku) return;
        try {
            await allocateStock(selectedStore.id, {
                items: [{ sku: allocationSku, quantity: parseInt(allocationQty) }]
            });
            alert('Allocation Successful');
        } catch (e) {
            alert('Allocation Failed: ' + (e.response?.data?.message || e.message));
        }
    }

    const openReconcileModal = (storeId) => {
        setReconcileStoreId(storeId);
        setReturnStock(false);
        setReconcileModalOpen(true);
    }

    const confirmReconcile = async () => {
        try {
            await reconcileStore(reconcileStoreId, returnStock);
            alert('Reconciliation Successful');
            setReconcileModalOpen(false);
        } catch (e) {
            alert('Reconciliation Failed: ' + (e.response?.data?.message || e.message));
        }
    }

    const openHistory = async (store) => {
        try {
            const logs = await getReconciliationHistory(store.id);
            setHistoryLogs(logs);
            setSelectedHistoryStoreName(store.name);
            setHistoryModalOpen(true);
        } catch (e) {
            alert('Failed to load history');
        }
    }

    const openReport = async (store) => {
        setReportLoading(true);
        setSelectedReportStoreName(store.name);
        setReportModalOpen(true);
        try {
            const data = await getReconciliationReport(store.id);
            setCurrentReport(data);
        } catch (e) {
            alert('Failed to load report');
            setReportModalOpen(false);
        } finally {
            setReportLoading(false);
        }
    }

    const viewLogDetails = (log) => {
        try {
            const details = JSON.parse(log.detailsJson);
            setSelectedLog({ ...log, parsedDetails: details });
            setDetailsModalOpen(true);
        } catch (e) {
            alert('Error parsing report details');
        }
    }

    // Styles for Report Cards
    const cardStyle = {
        background: '#f8f9fa',
        padding: '15px',
        borderRadius: '8px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        flex: 1,
        minWidth: '150px',
        textAlign: 'center',
        margin: '5px'
    };

    const valueStyle = {
        fontSize: '1.2rem',
        fontWeight: 'bold',
        color: '#2c3e50',
        marginTop: '5px'
    };

    const labelStyle = {
        color: '#7f8c8d',
        textTransform: 'uppercase',
        fontSize: '0.75rem',
        letterSpacing: '1px'
    };

    return (
        <div>
            <h2>Virtual Store Management</h2>

            {/* Create Store */}
            <div style={{ marginBottom: '2rem', display: 'flex', gap: '10px' }}>
                <input
                    type="text"
                    placeholder="New Store Name"
                    value={newStoreName}
                    onChange={(e) => setNewStoreName(e.target.value)}
                    style={{ padding: '10px', borderRadius: '4px', border: '1px solid #ddd' }}
                />
                <button onClick={handleCreate} style={{ padding: '10px 20px', background: '#27ae60', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', whiteSpace: 'nowrap' }}>Create Store</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1rem' }}>
                {stores.map(store => (
                    <div key={store.id} style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)', color: '#333' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3>{store.name}</h3>
                            <div style={{ display: 'flex', gap: '5px' }}>
                                <button onClick={() => openReport(store)} style={{ padding: '5px 10px', fontSize: '0.8rem', background: '#9b59b6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Status</button>
                                <button onClick={() => openHistory(store)} style={{ padding: '5px 10px', fontSize: '0.8rem', background: '#34495e', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>History</button>
                            </div>
                        </div>
                        <span style={{ fontSize: '0.8rem', color: '#888' }}>{store.type}</span>

                        <div style={{ marginTop: '1rem', borderTop: '1px solid #eee', paddingTop: '1rem' }}>
                            <h4>Allocation</h4>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                <input
                                    type="text"
                                    placeholder="SKU (e.g. SKU-BOOK-1)"
                                    value={selectedStore?.id === store.id ? allocationSku : ''}
                                    onChange={(e) => {
                                        setSelectedStore(store);
                                        setAllocationSku(e.target.value);
                                    }}
                                    style={{ padding: '8px', border: '1px solid #ddd' }}
                                />
                                <input
                                    type="number"
                                    min="1"
                                    value={selectedStore?.id === store.id ? allocationQty : 1}
                                    onChange={(e) => {
                                        setSelectedStore(store);
                                        setAllocationQty(e.target.value);
                                    }}
                                    style={{ padding: '8px', border: '1px solid #ddd' }}
                                />
                                <button onClick={handleAllocate} style={{ padding: '8px 15px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginTop: '5px' }}>Transfer Stock</button>
                            </div>
                        </div>

                        <button
                            onClick={() => openReconcileModal(store.id)}
                            style={{ marginTop: '1rem', width: '100%', background: '#ff9f43', color: 'white' }}
                        >
                            Reconcile (Check-in)
                        </button>
                    </div>
                ))}
            </div>

            {/* Reconcile Modal */}
            {reconcileModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center'
                }}>
                    <div style={{ background: 'white', padding: '20px', borderRadius: '8px', width: '400px' }}>
                        <h3>Confirm Reconciliation</h3>
                        <p>This will calculate revenue for all unreconciled orders.</p>

                        <div style={{ margin: '20px 0', display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <input
                                type="checkbox"
                                id="returnStock"
                                checked={returnStock}
                                onChange={(e) => setReturnStock(e.target.checked)}
                            />
                            <label htmlFor="returnStock">Return unsold inventory to Master Store</label>
                        </div>

                        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                            <button onClick={() => setReconcileModalOpen(false)} style={{ background: '#95a5a6' }}>Cancel</button>
                            <button onClick={confirmReconcile} style={{ background: '#27ae60' }}>Confirm</button>
                        </div>
                    </div>
                </div>
            )}

            {/* History Modal */}
            {historyModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
                }}>
                    <div style={{ background: 'white', padding: '20px', borderRadius: '8px', width: '700px', maxHeight: '80vh', overflowY: 'auto' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h3>History: {selectedHistoryStoreName}</h3>
                            <button onClick={() => setHistoryModalOpen(false)} style={{ background: 'transparent', color: '#333', fontSize: '1.5rem', border: 'none', cursor: 'pointer' }}>×</button>
                        </div>

                        {historyLogs.length === 0 ? (
                            <p>No reconciliation history.</p>
                        ) : (
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                <thead>
                                    <tr style={{ background: '#f8f9fa' }}>
                                        <th style={{ padding: '10px', textAlign: 'left' }}>Date</th>
                                        <th style={{ padding: '10px', textAlign: 'left' }}>Admin</th>
                                        <th style={{ padding: '10px', textAlign: 'right' }}>Orders</th>
                                        <th style={{ padding: '10px', textAlign: 'right' }}>Revenue</th>
                                        <th style={{ padding: '10px', textAlign: 'center' }}>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {historyLogs.map(log => (
                                        <tr key={log.id} style={{ borderBottom: '1px solid #eee' }}>
                                            <td style={{ padding: '10px' }}>{new Date(log.reconciledAt).toLocaleString()}</td>
                                            <td style={{ padding: '10px' }}>{log.reconciledBy?.username}</td>
                                            <td style={{ padding: '10px', textAlign: 'right' }}>{log.totalItemsSold}</td>
                                            <td style={{ padding: '10px', textAlign: 'right' }}>₹{log.totalRevenue}</td>
                                            <td style={{ padding: '10px', textAlign: 'center' }}>
                                                <button onClick={() => viewLogDetails(log)} style={{ padding: '5px 10px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>View</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {/* Current Report Modal */}
            {reportModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
                }}>
                    <div style={{ background: 'white', padding: '20px', borderRadius: '8px', width: '600px', maxHeight: '80vh', overflowY: 'auto' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h3>Status: {selectedReportStoreName}</h3>
                            <button onClick={() => setReportModalOpen(false)} style={{ background: 'transparent', color: '#333', fontSize: '1.5rem', border: 'none', cursor: 'pointer' }}>×</button>
                        </div>

                        {reportLoading ? <div>Loading...</div> : currentReport ? (
                            <div>
                                <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                                    <div style={cardStyle}>
                                        <div style={labelStyle}>Total Orders</div>
                                        <div style={valueStyle}>{currentReport.totalOrders}</div>
                                    </div>
                                    <div style={cardStyle}>
                                        <div style={labelStyle}>Total Amount</div>
                                        <div style={valueStyle}>₹{currentReport.totalAmount?.toFixed(2)}</div>
                                    </div>
                                </div>

                                <div style={{ display: 'flex', flexWrap: 'wrap', marginTop: '10px' }}>
                                    <div style={cardStyle}>
                                        <div style={labelStyle}>Reconciled Orders</div>
                                        <div style={{ ...valueStyle, color: '#27ae60' }}>{currentReport.reconciledOrders}</div>
                                    </div>
                                    <div style={cardStyle}>
                                        <div style={labelStyle}>Reconciled Amount</div>
                                        <div style={{ ...valueStyle, color: '#27ae60' }}>₹{currentReport.reconciledAmount?.toFixed(2)}</div>
                                    </div>
                                </div>

                                <div style={{ display: 'flex', flexWrap: 'wrap', marginTop: '10px' }}>
                                    <div style={cardStyle}>
                                        <div style={labelStyle}>Unreconciled Orders</div>
                                        <div style={{ ...valueStyle, color: '#c0392b' }}>{currentReport.unreconciledOrders}</div>
                                    </div>
                                    <div style={cardStyle}>
                                        <div style={labelStyle}>Unreconciled Amount</div>
                                        <div style={{ ...valueStyle, color: '#c0392b' }}>₹{currentReport.unreconciledAmount?.toFixed(2)}</div>
                                    </div>
                                </div>
                            </div>
                        ) : <div>No report data.</div>}
                    </div>
                </div>
            )}

            {/* Details Modal */}
            {detailsModalOpen && selectedLog && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1001
                }}>
                    <div style={{ background: 'white', padding: '20px', borderRadius: '8px', width: '600px', maxHeight: '80vh', overflowY: 'auto' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid #eee', paddingBottom: '10px' }}>
                            <div>
                                <h3 style={{ margin: 0 }}>Reconciliation Report</h3>
                                <span style={{ fontSize: '0.8rem', color: '#888' }}>{new Date(selectedLog.reconciledAt).toLocaleString()}</span>
                            </div>
                            <button onClick={() => setDetailsModalOpen(false)} style={{ background: 'transparent', color: '#333', fontSize: '1.5rem', border: 'none', cursor: 'pointer' }}>×</button>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
                            <div style={{ background: '#f8f9fa', padding: '15px', borderRadius: '8px' }}>
                                <div style={{ fontSize: '0.9rem', color: '#666' }}>Total Revenue</div>
                                <div style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>₹{selectedLog.totalRevenue}</div>
                            </div>
                            <div style={{ background: '#f8f9fa', padding: '15px', borderRadius: '8px' }}>
                                <div style={{ fontSize: '0.9rem', color: '#666' }}>Inventory Returned</div>
                                <div style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{selectedLog.inventoryReturned ? 'Yes' : 'No'}</div>
                            </div>
                        </div>

                        <h4>Sold Items</h4>
                        <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px' }}>
                            <thead>
                                <tr style={{ borderBottom: '2px solid #eee' }}>
                                    <th style={{ textAlign: 'left', padding: '10px' }}>Product</th>
                                    <th style={{ textAlign: 'right', padding: '10px' }}>Qty</th>
                                    <th style={{ textAlign: 'right', padding: '10px' }}>Total</th>
                                </tr>
                            </thead>
                            <tbody>
                                {selectedLog.parsedDetails?.soldItems?.map((item, idx) => (
                                    <tr key={idx} style={{ borderBottom: '1px solid #f9f9f9' }}>
                                        <td style={{ padding: '10px' }}>
                                            <div>{item.productName}</div>
                                            <div style={{ fontSize: '0.8rem', color: '#888' }}>{item.productSku}</div>
                                        </td>
                                        <td style={{ padding: '10px', textAlign: 'right' }}>{item.quantity}</td>
                                        <td style={{ padding: '10px', textAlign: 'right' }}>₹{item.total.toFixed(2)}</td>
                                    </tr>
                                ))}
                                {(!selectedLog.parsedDetails?.soldItems || selectedLog.parsedDetails.soldItems.length === 0) && (
                                    <tr><td colSpan="3" style={{ padding: '10px', textAlign: 'center', color: '#888' }}>No items sold</td></tr>
                                )}
                            </tbody>
                        </table>

                        {selectedLog.parsedDetails?.returnedItems && selectedLog.parsedDetails.returnedItems.length > 0 && (
                            <>
                                <h4>Returned Inventory</h4>
                                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                    <thead>
                                        <tr style={{ borderBottom: '2px solid #eee' }}>
                                            <th style={{ textAlign: 'left', padding: '10px' }}>Product</th>
                                            <th style={{ textAlign: 'right', padding: '10px' }}>Qty</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {selectedLog.parsedDetails.returnedItems.map((item, idx) => (
                                            <tr key={idx} style={{ borderBottom: '1px solid #f9f9f9' }}>
                                                <td style={{ padding: '10px' }}>
                                                    <div>{item.productName}</div>
                                                    <div style={{ fontSize: '0.8rem', color: '#888' }}>{item.productSku}</div>
                                                </td>
                                                <td style={{ padding: '10px', textAlign: 'right' }}>{item.quantity}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default StoreManager;

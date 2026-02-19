const ReconciliationReportModal = ({ report, onClose }) => {
    if (!report) return null;

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1100
        }}>
            <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '600px', maxHeight: '90vh', overflowY: 'auto' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', borderBottom: '1px solid #eee', paddingBottom: '10px' }}>
                    <h3 style={{ margin: 0 }}>Reconciliation Report: {report.storeName}</h3>
                    <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
                    <div style={{ background: '#f8f9fa', padding: '15px', borderRadius: '8px' }}>
                        <h4 style={{ margin: '0 0 10px 0', color: '#2c3e50' }}>Financials</h4>
                        <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#27ae60' }}>
                            Total Revenue: ₹{report.totalRevenue}
                        </div>
                        <div>Items Sold: {report.totalItemsSold}</div>
                    </div>
                    <div style={{ background: '#f8f9fa', padding: '15px', borderRadius: '8px' }}>
                        <h4 style={{ margin: '0 0 10px 0', color: '#2c3e50' }}>Assigned Administrators</h4>
                        <ul style={{ paddingLeft: '20px', margin: 0 }}>
                            {report.assignedAdmins.map(admin => (
                                <li key={admin}>{admin}</li>
                            ))}
                        </ul>
                    </div>
                </div>

                <h4 style={{ borderBottom: '1px solid #eee', paddingBottom: '5px' }}>Items Sold</h4>
                <table style={{ width: '100%', marginBottom: '20px', fontSize: '0.9rem' }}>
                    <thead>
                        <tr style={{ textAlign: 'left', background: '#eee' }}>
                            <th style={{ padding: '8px' }}>SKU</th>
                            <th style={{ padding: '8px' }}>Name</th>
                            <th style={{ padding: '8px' }}>Qty</th>
                            <th style={{ padding: '8px' }}>Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        {report.soldItems.map(item => (
                            <tr key={item.sku} style={{ borderBottom: '1px solid #eee' }}>
                                <td style={{ padding: '8px' }}>{item.sku}</td>
                                <td style={{ padding: '8px' }}>{item.name}</td>
                                <td style={{ padding: '8px' }}>{item.quantity}</td>
                                <td style={{ padding: '8px' }}>₹{item.total}</td>
                            </tr>
                        ))}
                        {report.soldItems.length === 0 && <tr><td colSpan="4" style={{ padding: '10px', textAlign: 'center' }}>No items sold</td></tr>}
                    </tbody>
                </table>

                <h4 style={{ borderBottom: '1px solid #eee', paddingBottom: '5px' }}>Stock Returned to HQ</h4>
                <table style={{ width: '100%', marginBottom: '20px', fontSize: '0.9rem' }}>
                    <thead>
                        <tr style={{ textAlign: 'left', background: '#eee' }}>
                            <th style={{ padding: '8px' }}>SKU</th>
                            <th style={{ padding: '8px' }}>Name</th>
                            <th style={{ padding: '8px' }}>Qty</th>
                        </tr>
                    </thead>
                    <tbody>
                        {report.returnedItems.map(item => (
                            <tr key={item.sku} style={{ borderBottom: '1px solid #eee' }}>
                                <td style={{ padding: '8px' }}>{item.sku}</td>
                                <td style={{ padding: '8px' }}>{item.name}</td>
                                <td style={{ padding: '8px' }}>{item.quantity}</td>
                            </tr>
                        ))}
                        {report.returnedItems.length === 0 && <tr><td colSpan="3" style={{ padding: '10px', textAlign: 'center' }}>No items returned</td></tr>}
                    </tbody>
                </table>

                <button
                    onClick={() => {
                        onClose();
                        window.print();
                    }}
                    style={{ width: '100%', padding: '10px', background: '#2c3e50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                >
                    Close & Print
                </button>
            </div>
        </div>
    );
};

export default ReconciliationReportModal;

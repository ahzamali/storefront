const TransferStockModal = ({ isOpen, selectedItems, stores, transferTargetStore, setTransferTargetStore, transferQty, setTransferQty, onTransfer, onClose }) => {
    if (!isOpen) return null;

    return (
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
                        <button onClick={onTransfer} style={{ flex: 1, height: '40px', padding: '0 10px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Transfer</button>
                        <button onClick={onClose} style={{ flex: 1, height: '40px', padding: '0 10px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Cancel</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TransferStockModal;

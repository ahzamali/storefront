const BundleCreationModal = ({ isOpen, selectedItems, products, newBundle, setNewBundle, onSubmit, onClose }) => {
    if (!isOpen) return null;

    const selectedProductObjects = products.filter(p => selectedItems.has(p.sku));
    const totalPrice = selectedProductObjects.reduce((sum, p) => sum + (parseFloat(p.basePrice || p.price) || 0), 0);

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
        }}>
            <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '400px' }}>
                <h3>Create Bundle</h3>
                <p>{selectedItems.size} items selected</p>
                <div style={{ marginBottom: '10px', fontSize: '0.9rem', color: '#555' }}>
                    <strong>Total Item Price: </strong> ₹{totalPrice.toFixed(2)}
                </div>

                <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
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
                        <button type="button" onClick={onClose} style={{ flex: 1, height: '40px', padding: '0 10px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default BundleCreationModal;

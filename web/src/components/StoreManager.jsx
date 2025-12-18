import { useState, useEffect } from 'react';
import { getStores, createStore, allocateStock, reconcileStore } from '../services/api';

const StoreManager = () => {
    const [stores, setStores] = useState([]);
    const [newStoreName, setNewStoreName] = useState('');
    const [selectedStore, setSelectedStore] = useState(null);
    const [allocationSku, setAllocationSku] = useState('');
    const [allocationQty, setAllocationQty] = useState(1);

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

    const handleReconcile = async (storeId) => {
        if (window.confirm('Are you sure you want to return all stock to HQ?')) {
            try {
                await reconcileStore(storeId);
                alert('Reconciliation Successful');
            } catch (e) {
                alert('Reconciliation Failed');
            }
        }
    }

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
                <button onClick={handleCreate}>Create Store</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1rem' }}>
                {stores.map(store => (
                    <div key={store.id} style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
                        <h3>{store.name}</h3>
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
                                <button onClick={handleAllocate}>Transfer Stock</button>
                            </div>
                        </div>

                        <button
                            onClick={() => handleReconcile(store.id)}
                            style={{ marginTop: '1rem', width: '100%', background: '#ff9f43', color: 'white' }}
                        >
                            Reconcile (Check-in)
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default StoreManager;

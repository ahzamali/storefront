import { useState } from 'react';
import InventoryManager from '../components/InventoryManager';
import StoreManager from '../components/StoreManager';

const Dashboard = ({ token, setToken }) => {
    const [view, setView] = useState('inventory');

    const logout = () => {
        setToken(null);
    };

    return (
        <div style={{ display: 'flex', minHeight: '100vh', background: '#f5f6fa' }}>
            {/* Sidebar */}
            <div style={{ width: '250px', background: '#2c3e50', color: 'white', padding: '1rem' }}>
                <h3>Admin Panel</h3>
                <ul style={{ listStyle: 'none', padding: 0, marginTop: '2rem' }}>
                    <li
                        onClick={() => setView('inventory')}
                        style={{ padding: '10px', cursor: 'pointer', background: view === 'inventory' ? '#34495e' : 'transparent' }}
                    >Inventory</li>
                    <li
                        onClick={() => setView('stores')}
                        style={{ padding: '10px', cursor: 'pointer', background: view === 'stores' ? '#34495e' : 'transparent' }}
                    >Virtual Stores</li>
                </ul>
                <button onClick={logout} style={{ marginTop: 'auto', width: '100%', background: '#c0392b' }}>Logout</button>
            </div>

            {/* Content */}
            <div style={{ flex: 1, padding: '2rem', color: '#2c3e50' }}>
                {view === 'inventory' && <InventoryManager />}
                {view === 'stores' && <StoreManager />}
            </div>
        </div>
    );
};

export default Dashboard;

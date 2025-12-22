import React, { useState } from 'react';
import InventoryManager from '../components/InventoryManager';
import StoreManager from '../components/StoreManager';
import UserManager from '../components/UserManager';
import PointOfSale from '../components/PointOfSale';

const Dashboard = ({ token, setToken }) => {
    const [view, setView] = useState('inventory');

    // Extract user info from localStorage
    const userId = localStorage.getItem('userId');
    const userRole = localStorage.getItem('userRole');
    const userStoreIdsStr = localStorage.getItem('storeIds');
    const userStoreIds = userStoreIdsStr ? JSON.parse(userStoreIdsStr) : [];

    const logout = () => {
        setToken(null);
        localStorage.removeItem('userId');
        localStorage.removeItem('userRole');
        localStorage.removeItem('storeIds');
        localStorage.removeItem('token');
    };

    return (
        <div style={{ display: 'flex', minHeight: '100vh', background: '#f5f6fa' }}>
            {/* Sidebar */}
            <div style={{ width: '250px', background: '#2c3e50', color: 'white', padding: '1rem' }}>
                <h3>Admin Panel</h3>
                <ul style={{ listStyle: 'none', padding: 0, marginTop: '2rem' }}>
                    <li
                        onClick={() => setView('pos')}
                        style={{ padding: '10px', cursor: 'pointer', background: view === 'pos' ? '#34495e' : 'transparent' }}
                    >Point of Sale</li>
                    <li
                        onClick={() => setView('inventory')}
                        style={{ padding: '10px', cursor: 'pointer', background: view === 'inventory' ? '#34495e' : 'transparent' }}
                    >Inventory</li>
                    <li
                        onClick={() => setView('stores')}
                        style={{ padding: '10px', cursor: 'pointer', background: view === 'stores' ? '#34495e' : 'transparent' }}
                    >Virtual Stores</li>
                    <li
                        onClick={() => setView('users')}
                        style={{ padding: '10px', cursor: 'pointer', background: view === 'users' ? '#34495e' : 'transparent' }}
                    >Users</li>
                </ul>
                <button onClick={logout} style={{ marginTop: 'auto', width: '100%', background: '#c0392b' }}>Logout</button>
            </div>

            {/* Content */}
            <div style={{ flex: 1, padding: '2rem', color: '#2c3e50' }}>
                {view === 'pos' && <PointOfSale userId={userId} userRole={userRole} userStoreIds={userStoreIds} />}
                {view === 'inventory' && <InventoryManager />}
                {view === 'stores' && <StoreManager />}
                {view === 'users' && <UserManager />}
            </div>
        </div>
    );
};

export default Dashboard;

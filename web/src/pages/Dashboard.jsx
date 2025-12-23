import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link, Routes, Route } from 'react-router-dom';
// Components
import InventoryManager from '../components/InventoryManager';
import StoreManager from '../components/StoreManager';
import UserManager from '../components/UserManager';
import PointOfSale from '../components/PointOfSale';
import OrderManager from '../components/OrderManager'; // New Import

const Dashboard = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [userRole, setUserRole] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('userRole');
        if (!token) {
            navigate('/login');
        } else {
            setUserRole(role);
        }
    }, [navigate]);

    return (
        <div style={{ display: 'flex', minHeight: '100vh', background: '#f4f6f9' }}>
            {/* Sidebar Navigation */}
            <div style={{
                width: '250px',
                background: '#2c3e50',
                color: 'white',
                display: 'flex',
                flexDirection: 'column',
                padding: '20px'
            }}>
                <h2 style={{ marginBottom: '2rem', textAlign: 'center', borderBottom: '1px solid #34495e', paddingBottom: '1rem' }}>StoreFront</h2>

                <nav style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    <Link to="/" style={{ padding: '10px', color: 'white', textDecoration: 'none', background: location.pathname === '/' ? '#34495e' : 'transparent', borderRadius: '4px' }}>Inventory</Link>
                    <Link to="/pos" style={{ padding: '10px', color: 'white', textDecoration: 'none', background: location.pathname === '/pos' ? '#34495e' : 'transparent', borderRadius: '4px' }}>Point of Sale</Link>
                    <Link to="/orders" style={{ padding: '10px', color: 'white', textDecoration: 'none', background: location.pathname === '/orders' ? '#34495e' : 'transparent', borderRadius: '4px' }}>Orders</Link>

                    {['SUPER_ADMIN', 'ADMIN'].includes(userRole) && (
                        <>
                            <Link to="/stores" style={{ padding: '10px', color: 'white', textDecoration: 'none', background: location.pathname === '/stores' ? '#34495e' : 'transparent', borderRadius: '4px' }}>Stores</Link>
                            <Link to="/users" style={{ padding: '10px', color: 'white', textDecoration: 'none', background: location.pathname === '/users' ? '#34495e' : 'transparent', borderRadius: '4px' }}>Users</Link>
                        </>
                    )}
                </nav>

                <div style={{ marginTop: 'auto' }}>
                    <button
                        onClick={() => {
                            localStorage.clear();
                            navigate('/login');
                        }}
                        style={{ width: '100%', padding: '10px', background: '#c0392b', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                    >
                        Logout
                    </button>
                    <div style={{ marginTop: '10px', fontSize: '0.8rem', textAlign: 'center', opacity: 0.7 }}>
                        Role: {userRole}
                    </div>
                </div>
            </div>

            {/* Main Content Area */}
            <div style={{ flex: 1, padding: '20px', overflowY: 'auto' }}>
                <Routes>
                    <Route path="/" element={<InventoryManager />} />
                    <Route path="/pos" element={<PointOfSale userRole={userRole} />} />
                    <Route path="/orders" element={<OrderManager />} />
                    <Route path="/stores" element={<StoreManager />} />
                    <Route path="/users" element={<UserManager />} />
                </Routes>
            </div>
        </div>
    );
};

export default Dashboard;

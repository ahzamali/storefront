import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link, Routes, Route } from 'react-router-dom';
// Components
import InventoryManager from '../components/InventoryManager';
import StoreManager from '../components/StoreManager';
import UserManager from '../components/UserManager';
import PointOfSale from '../components/PointOfSale';
import OrderManager from '../components/OrderManager'; // New Import


import { updateUser } from '../services/api';

const Dashboard = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [userRole, setUserRole] = useState('');

    // Change Password State
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('userRole');
        if (!token) {
            navigate('/login');
        } else {
            setUserRole(role);
        }
    }, [navigate]);

    const handleChangePassword = async (e) => {
        e.preventDefault();

        if (newPassword !== confirmPassword) {
            alert("Passwords do not match!");
            return;
        }

        const userId = localStorage.getItem('userId');
        if (!userId) {
            alert("User ID not found. Please log in again.");
            return;
        }

        try {
            await updateUser(userId, { password: newPassword });
            alert('Password changed successfully. Please log in again.');
            localStorage.clear();
            navigate('/login');
        } catch (err) {
            alert(err.response?.data?.error || 'Failed to change password');
        }
    };

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
                    <Link to="/pos" style={{ padding: '10px', color: 'white', textDecoration: 'none', background: location.pathname === '/pos' ? '#34495e' : 'transparent', borderRadius: '4px' }}>Sale</Link>
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
                        onClick={() => setShowPasswordModal(true)}
                        style={{ width: '100%', padding: '10px', background: 'transparent', color: '#bdc3c7', border: '1px solid #bdc3c7', borderRadius: '4px', cursor: 'pointer', marginBottom: '10px' }}
                    >
                        Change Password
                    </button>
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

            {/* Change Password Modal */}
            {showPasswordModal && (
                <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 }}>
                    <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '350px' }}>
                        <h3>Change Password</h3>
                        <form onSubmit={handleChangePassword}>
                            <div style={{ marginBottom: '1rem' }}>
                                <label style={{ display: 'block', marginBottom: '5px' }}>New Password</label>
                                <input
                                    type="password"
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    required
                                    style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                                />
                            </div>
                            <div style={{ marginBottom: '1rem' }}>
                                <label style={{ display: 'block', marginBottom: '5px' }}>Confirm Password</label>
                                <input
                                    type="password"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    required
                                    style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                                />
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                                <button type="button" onClick={() => setShowPasswordModal(false)} style={{ padding: '8px 16px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Cancel</button>
                                <button type="submit" style={{ padding: '8px 16px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Update</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;

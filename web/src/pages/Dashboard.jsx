import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link, Routes, Route } from 'react-router-dom';
import './Dashboard.css';

// Components
import InventoryManager from '../components/InventoryManager';
import StoreManager from '../components/StoreManager';
import UserManager from '../components/UserManager';
import PointOfSale from '../components/PointOfSale';
import OrderManager from '../components/OrderManager';

import { updateUser } from '../services/api';

const Dashboard = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [userRole, setUserRole] = useState('');
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

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

    const toggleSidebar = () => {
        setIsSidebarOpen(!isSidebarOpen);
    };

    const closeSidebar = () => {
        setIsSidebarOpen(false);
    };

    return (
        <div className="dashboard-container">
            {/* Mobile Menu Toggle */}
            <button className="mobile-menu-toggle" onClick={toggleSidebar}>
                ☰ Menu
            </button>

            {/* Sidebar Overlay */}
            <div
                className={`sidebar-overlay ${isSidebarOpen ? 'open' : ''}`}
                onClick={closeSidebar}
            ></div>

            {/* Sidebar Navigation */}
            <div className={`dashboard-sidebar ${isSidebarOpen ? 'open' : ''}`}>
                <h2 className="sidebar-header">StoreFront</h2>

                <nav className="sidebar-nav">
                    <Link
                        to="/"
                        className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
                        onClick={closeSidebar}
                    >
                        Inventory
                    </Link>
                    <Link
                        to="/pos"
                        className={`nav-link ${location.pathname === '/pos' ? 'active' : ''}`}
                        onClick={closeSidebar}
                    >
                        Sale
                    </Link>
                    <Link
                        to="/orders"
                        className={`nav-link ${location.pathname === '/orders' ? 'active' : ''}`}
                        onClick={closeSidebar}
                    >
                        Orders
                    </Link>


                    {['SUPER_ADMIN', 'ADMIN'].includes(userRole) && (
                        <>
                            <Link
                                to="/stores"
                                className={`nav-link ${location.pathname === '/stores' ? 'active' : ''}`}
                                onClick={closeSidebar}
                            >
                                Stores
                            </Link>
                            <Link
                                to="/users"
                                className={`nav-link ${location.pathname === '/users' ? 'active' : ''}`}
                                onClick={closeSidebar}
                            >
                                Users
                            </Link>
                        </>
                    )}
                </nav>

                <div className="sidebar-footer">
                    <button
                        onClick={() => setShowPasswordModal(true)}
                        className="btn-change-password"
                    >
                        Change Password
                    </button>
                    <button
                        onClick={() => {
                            localStorage.clear();
                            navigate('/login');
                        }}
                        className="btn-logout"
                    >
                        Logout
                    </button>
                    <div className="user-role-display">
                        Role: {userRole}
                    </div>
                </div>
            </div>

            {/* Main Content Area */}
            <div className="dashboard-content">
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
                <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 2000 }}>
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

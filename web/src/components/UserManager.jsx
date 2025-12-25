import { useState, useEffect } from 'react';
import { getUsers, register, getStores, deleteUser, updateUser } from '../services/api';

const UserManager = () => {
    const [users, setUsers] = useState([]);
    const [stores, setStores] = useState([]);
    const [form, setForm] = useState({ username: '', password: '', role: 'EMPLOYEE', storeId: '' });
    const [error, setError] = useState('');

    // Edit State
    const [showEditModal, setShowEditModal] = useState(false);
    const [editingUser, setEditingUser] = useState(null);
    const [editForm, setEditForm] = useState({ password: '', confirmPassword: '', storeIds: [] });

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [usersData, storesData] = await Promise.all([getUsers(), getStores()]);
            setUsers(usersData);
            setStores(storesData);
        } catch (err) {
            console.error(err);
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            // Logic to clear storeId if role is SUPER_ADMIN
            const submission = { ...form };
            if (submission.role === 'SUPER_ADMIN') {
                submission.storeId = null;
            }

            await register(submission);
            setForm({ username: '', password: '', role: 'EMPLOYEE', storeId: '' });

            // Reload users
            const usersData = await getUsers();
            setUsers(usersData);
            setError('');
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to create user');
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this user?')) return;
        try {
            await deleteUser(id);
            const usersData = await getUsers();
            setUsers(usersData);
        } catch (err) {
            alert('Failed to delete user');
        }
    };

    const handleEditClick = (user) => {
        setEditingUser(user);
        setEditForm({
            password: '',
            confirmPassword: '',
            storeIds: user.stores ? user.stores.map(s => s.id) : []
        });
        setShowEditModal(true);
    };

    const handleUpdateUser = async (e) => {
        e.preventDefault();
        if (editForm.password && editForm.password !== editForm.confirmPassword) {
            alert('Passwords do not match');
            return;
        }
        try {
            const payload = {
                password: editForm.password,
                storeIds: editForm.storeIds
            };
            await updateUser(editingUser.id, payload);
            alert('User updated successfully');
            setShowEditModal(false);
            setEditingUser(null);
            loadData(); // Reload
        } catch (err) {
            alert(err.response?.data?.error || 'Failed to update user');
        }
    };

    const userRole = localStorage.getItem('userRole');

    return (
        <div>
            <h2>User Management</h2>
            {error && <div style={{ padding: '10px', background: '#fadbd8', color: '#c0392b', borderRadius: '5px', marginBottom: '1rem' }}>{error}</div>}

            <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)', marginBottom: '2rem' }}>
                <h3>Add New User</h3>
                <form onSubmit={handleCreate} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginTop: '1rem' }}>

                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <label style={{ fontSize: '0.9rem', marginBottom: '5px', fontWeight: 'bold' }}>Username</label>
                        <input
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                            value={form.username}
                            onChange={e => setForm({ ...form, username: e.target.value })}
                            required
                        />
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <label style={{ fontSize: '0.9rem', marginBottom: '5px', fontWeight: 'bold' }}>Password</label>
                        <input
                            type="password"
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                            value={form.password}
                            onChange={e => setForm({ ...form, password: e.target.value })}
                            required
                        />
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <label style={{ fontSize: '0.9rem', marginBottom: '5px', fontWeight: 'bold' }}>Role</label>
                        <select
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                            value={form.role}
                            onChange={e => setForm({ ...form, role: e.target.value })}
                        >
                            <option value="EMPLOYEE">Employee</option>
                            <option value="STORE_ADMIN">Store Admin</option>
                            <option value="SUPER_ADMIN">Super Admin</option>
                        </select>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <label style={{ fontSize: '0.9rem', marginBottom: '5px', fontWeight: 'bold' }}>Store Scope</label>
                        <select
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                            value={form.storeId}
                            onChange={e => setForm({ ...form, storeId: e.target.value })}
                            disabled={form.role === 'SUPER_ADMIN'}
                        >
                            <option value="">-- None (Global) --</option>
                            {stores.map(s => <option key={s.id} value={s.id}>{s.name} ({s.type})</option>)}
                        </select>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                        <button type="submit" style={{ padding: '10px 20px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', height: '38px', fontWeight: 'bold' }}>Add User</button>
                    </div>
                </form>
            </div>

            <div style={{ background: 'white', padding: '1rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', color: '#333' }}>
                    <thead>
                        <tr style={{ background: '#f8f9fa', textAlign: 'left' }}>
                            <th style={{ padding: '12px' }}>ID</th>
                            <th style={{ padding: '12px' }}>Username</th>
                            <th style={{ padding: '12px' }}>Role</th>
                            <th style={{ padding: '12px' }}>Stores</th>
                            <th style={{ padding: '12px' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map(u => (
                            <tr key={u.id} style={{ borderBottom: '1px solid #eee' }}>
                                <td style={{ padding: '12px' }}>{u.id}</td>
                                <td style={{ padding: '12px', fontWeight: 'bold' }}>{u.username}</td>
                                <td style={{ padding: '12px' }}>{u.role}</td>
                                <td style={{ padding: '12px', color: '#7f8c8d' }}>
                                    {u.stores && u.stores.length > 0 ? u.stores.map(s => s.name).join(', ') : 'Global/None'}
                                </td>
                                <td style={{ padding: '12px' }}>
                                    {userRole === 'SUPER_ADMIN' && (
                                        <button
                                            onClick={() => handleEditClick(u)}
                                            style={{ padding: '5px 10px', background: '#f39c12', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '5px' }}
                                        >
                                            Edit
                                        </button>
                                    )}
                                    <button
                                        onClick={() => handleDelete(u.id)}
                                        style={{ padding: '5px 10px', background: '#e74c3c', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Edit User Modal */}
            {showEditModal && editingUser && (
                <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                    <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '400px' }}>
                        <h3>Edit User: {editingUser.username}</h3>
                        <form onSubmit={handleUpdateUser} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>New Password</label>
                                <input
                                    type="password"
                                    placeholder="Leave blank to keep unchanged"
                                    style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                                    value={editForm.password}
                                    onChange={e => setEditForm({ ...editForm, password: e.target.value })}
                                />
                            </div>

                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Confirm New Password</label>
                                <input
                                    type="password"
                                    placeholder="Confirm new password"
                                    style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}
                                    value={editForm.confirmPassword}
                                    onChange={e => setEditForm({ ...editForm, confirmPassword: e.target.value })}
                                />
                            </div>

                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>Store Assignment (Multi-select)</label>
                                <select
                                    multiple
                                    style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ddd', height: '100px' }}
                                    value={editForm.storeIds}
                                    onChange={e => {
                                        const selected = Array.from(e.target.selectedOptions, option => parseInt(option.value));
                                        setEditForm({ ...editForm, storeIds: selected });
                                    }}
                                >
                                    {stores.map(s => <option key={s.id} value={s.id}>{s.name} ({s.type})</option>)}
                                </select>
                                <small style={{ color: '#666' }}>Hold Ctrl/Cmd to select multiple</small>
                            </div>

                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '1rem' }}>
                                <button type="button" onClick={() => setShowEditModal(false)} style={{ padding: '8px 16px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Cancel</button>
                                <button type="submit" style={{ padding: '8px 16px', background: '#2ecc71', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Save Changes</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default UserManager;

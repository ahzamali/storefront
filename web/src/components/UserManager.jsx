import { useState, useEffect } from 'react';
import { getUsers, register, getStores, deleteUser } from '../services/api';

const UserManager = () => {
    const [users, setUsers] = useState([]);
    const [stores, setStores] = useState([]);
    const [form, setForm] = useState({ username: '', password: '', role: 'EMPLOYEE', storeId: '' });
    const [error, setError] = useState('');

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
                            <th style={{ padding: '12px' }}>Store</th>
                            <th style={{ padding: '12px' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map(u => (
                            <tr key={u.id} style={{ borderBottom: '1px solid #eee' }}>
                                <td style={{ padding: '12px' }}>{u.id}</td>
                                <td style={{ padding: '12px', fontWeight: 'bold' }}>{u.username}</td>
                                <td style={{ padding: '12px' }}>{u.role}</td>
                                <td style={{ padding: '12px', color: '#7f8c8d' }}>{u.store ? u.store.name : 'Global'}</td>
                                <td style={{ padding: '12px' }}>
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
        </div>
    );
};

export default UserManager;

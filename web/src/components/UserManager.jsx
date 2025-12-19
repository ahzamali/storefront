import { useState, useEffect } from 'react';
import { getUsers, register, getStores } from '../services/api';

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
            // Depending on role, one might fail.
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            await register(form);
            setForm({ username: '', password: '', role: 'EMPLOYEE', storeId: '' });
            // Reload users
            const usersData = await getUsers();
            setUsers(usersData);
            setError('');
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to create user');
        }
    };

    return (
        <div className="p-4 border rounded shadow-sm bg-white mt-6">
            <h2 className="text-xl font-bold mb-4">User Management</h2>
            {error && <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{error}</div>}

            <form onSubmit={handleCreate} className="mb-6 bg-gray-50 p-6 rounded border">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Username</label>
                        <input className="border p-2 rounded w-full h-10" value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} required />
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Password</label>
                        <input className="border p-2 rounded w-full h-10" type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required />
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Role</label>
                        <select className="border p-2 rounded w-full h-10" value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>
                            <option value="EMPLOYEE">Employee</option>
                            <option value="STORE_ADMIN">Store Admin</option>
                            <option value="SUPER_ADMIN">Super Admin</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Store Scope</label>
                        <select className="border p-2 rounded w-full h-10" value={form.storeId} onChange={e => setForm({ ...form, storeId: e.target.value })}>
                            <option value="">-- None (Global) --</option>
                            {stores.map(s => <option key={s.id} value={s.id}>{s.name} ({s.type})</option>)}
                        </select>
                    </div>
                </div>
                <div className="flex justify-start">
                    <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded font-bold hover:bg-blue-700 h-10">Add User</button>
                </div>
            </form>

            <table className="w-full text-left border-collapse">
                <thead>
                    <tr className="bg-gray-100 border-b">
                        <th className="p-2">ID</th>
                        <th className="p-2">Username</th>
                        <th className="p-2">Role</th>
                        <th className="p-2">Store</th>
                    </tr>
                </thead>
                <tbody>
                    {users.map(u => (
                        <tr key={u.id} className="border-b">
                            <td className="p-2">{u.id}</td>
                            <td className="p-2 font-bold">{u.username}</td>
                            <td className="p-2 text-sm">{u.role}</td>
                            <td className="p-2 text-sm text-gray-500">{u.store ? u.store.name : 'Global'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default UserManager;

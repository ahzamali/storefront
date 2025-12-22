import axios from 'axios';

const api = axios.create();

api.interceptors.request.use((config) => {
    const host = localStorage.getItem('targetHost') || 'http://localhost:8080';
    config.baseURL = `${host}/api/v1`;

    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export const login = async (username, password) => {
    const response = await api.post('/auth/login', { username, password });
    return response.data;
};

export const getProducts = async () => (await api.get('/inventory/products')).data;
export const getAllProducts = getProducts; // Alias for consistency
export const getInventoryView = async () => (await api.get('/inventory/view')).data;
export const addProduct = async (product) => (await api.post('/inventory/products', product)).data;
export const addStock = async (sku, quantity) => (await api.post('/inventory/stock', { sku, quantity })).data;
// Store management
export const getAllStores = async () => (await api.get('/stores')).data;
export const getStores = getAllStores; // Alias for backward compatibility
export const getStoreInventory = async (storeId, searchQuery = '') => {
    const params = searchQuery ? { search: searchQuery } : {};
    return (await api.get(`/stores/${storeId}/inventory`, { params })).data;
};

export const createStore = async (name, type) => (await api.post('/stores', { name, type })).data;
export const allocateStock = async (storeId, items) => await api.post(`/stores/${storeId}/allocate`, { items });
export const reconcileStore = async (storeId) => await api.post(`/stores/${storeId}/reconcile`);

export const getUsers = async () => (await api.get('/auth/users')).data;
export const register = async (userData) => (await api.post('/auth/register', userData)).data;
export const deleteUser = async (id) => await api.delete(`/auth/users/${id}`);

export const getBundles = async () => (await api.get('/inventory/bundles')).data;
export const createOrder = async (orderData) => (await api.post('/orders', orderData)).data;

export default api;

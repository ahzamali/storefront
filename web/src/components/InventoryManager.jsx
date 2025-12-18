import { useState, useEffect } from 'react';
import { getInventoryView, addProduct, addStock } from '../services/api';

const InventoryManager = () => {
    const [products, setProducts] = useState([]);
    const [newProduct, setNewProduct] = useState({ sku: '', name: '', type: 'BOOK', basePrice: '' });
    const [stock, setStock] = useState({ sku: '', quantity: '' });
    const [message, setMessage] = useState('');

    useEffect(() => {
        loadProducts();
    }, []);

    const loadProducts = async () => {
        try {
            const data = await getInventoryView();
            setProducts(data);
        } catch (e) {
            console.error("Failed to load products");
        }
    }

    const handleAddProduct = async (e) => {
        e.preventDefault();
        try {
            await addProduct(newProduct);
            setMessage('Product created successfully');
            setNewProduct({ sku: '', name: '', type: 'BOOK', basePrice: '' });
            loadProducts();
        } catch (err) {
            setMessage('Failed to create product');
        }
    };

    const handleAddStock = async (e) => {
        e.preventDefault();
        try {
            await addStock(stock.sku, parseInt(stock.quantity));
            setMessage(`Stock added for ${stock.sku}`);
            setStock({ sku: '', quantity: '' });
            loadProducts();
        } catch (err) {
            setMessage('Failed to add stock');
        }
    };

    return (
        <div>
            <h2>Inventory Management (Headquarters)</h2>
            {message && <p style={{ padding: '10px', background: '#dff9fb', color: '#27ae60', borderRadius: '5px' }}>{message}</p>}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2rem' }}>
                {/* Add Product Form */}
                <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                    <h3>Add New Product</h3>
                    <form onSubmit={handleAddProduct} style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1rem' }}>
                        <input type="text" placeholder="SKU" value={newProduct.sku} onChange={e => setNewProduct({ ...newProduct, sku: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <input type="text" placeholder="Name" value={newProduct.name} onChange={e => setNewProduct({ ...newProduct, name: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <select value={newProduct.type} onChange={e => setNewProduct({ ...newProduct, type: e.target.value })} style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }}>
                            <option value="BOOK">Book</option>
                            <option value="STATIONERY">Stationery</option>
                        </select>
                        <input type="number" placeholder="Base Price" value={newProduct.basePrice} onChange={e => setNewProduct({ ...newProduct, basePrice: e.target.value })} required step="0.01" style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <button type="submit" style={{ padding: '10px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Create Product</button>
                    </form>
                </div>

                {/* Add Stock Form */}
                <div style={{ background: 'white', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                    <h3>Add Stock</h3>
                    <form onSubmit={handleAddStock} style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '1rem' }}>
                        <input type="text" placeholder="SKU" value={stock.sku} onChange={e => setStock({ ...stock, sku: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <input type="number" placeholder="Quantity" value={stock.quantity} onChange={e => setStock({ ...stock, quantity: e.target.value })} required style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ddd' }} />
                        <button type="submit" style={{ padding: '10px', background: '#2ecc71', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Add Stock</button>
                    </form>
                </div>
            </div>

            <div style={{ marginTop: '1rem', background: 'white', padding: '1rem', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                        <tr style={{ background: '#f8f9fa', userSelect: 'none' }}>
                            <th style={{ padding: '12px', textAlign: 'left' }}>SKU</th>
                            <th style={{ padding: '12px', textAlign: 'left' }}>Name</th>
                            <th style={{ padding: '12px', textAlign: 'left' }}>Price</th>
                            <th style={{ padding: '12px', textAlign: 'left' }}>Type</th>
                            <th style={{ padding: '12px', textAlign: 'left' }}>Stock</th>
                        </tr>
                    </thead>
                    <tbody>
                        {products.map(p => (
                            <tr key={p.id} style={{ borderBottom: '1px solid #eee' }}>
                                <td style={{ padding: '12px' }}>{p.sku}</td>
                                <td style={{ padding: '12px' }}>{p.name}</td>
                                <td style={{ padding: '12px' }}>${p.basePrice || p.price}</td>
                                <td style={{ padding: '12px' }}>{p.type || 'BUNDLE'}</td>
                                <td style={{ padding: '12px', fontWeight: 'bold', color: p.quantity > 0 ? '#27ae60' : '#e74c3c' }}>
                                    {p.quantity}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default InventoryManager;

import React from 'react';

const ProductDetailModal = ({ product, onClose }) => {
    if (!product) return null;

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
        }}>
            <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', width: '500px', maxHeight: '80vh', overflowY: 'auto', textAlign: 'left' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', borderBottom: '1px solid #eee', paddingBottom: '0.5rem' }}>
                    <h3 style={{ margin: 0 }}>Product Details</h3>
                    <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                    <div>
                        <strong style={{ display: 'block', color: '#7f8c8d', fontSize: '0.9rem' }}>Name</strong>
                        <div style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>{product.name}</div>
                    </div>
                    <div>
                        <strong style={{ display: 'block', color: '#7f8c8d', fontSize: '0.9rem' }}>SKU</strong>
                        <div style={{ marginBottom: '1rem', fontFamily: 'monospace' }}>{product.sku}</div>
                    </div>
                    <div>
                        <strong style={{ display: 'block', color: '#7f8c8d', fontSize: '0.9rem' }}>Type</strong>
                        <div style={{ marginBottom: '1rem' }}>{product.type}</div>
                    </div>
                    <div>
                        <strong style={{ display: 'block', color: '#7f8c8d', fontSize: '0.9rem' }}>Price</strong>
                        <div style={{ marginBottom: '1rem', fontWeight: 'bold', color: '#2ecc71' }}>₹{product.basePrice || product.price}</div>
                    </div>
                    <div>
                        <strong style={{ display: 'block', color: '#7f8c8d', fontSize: '0.9rem' }}>Current Stock</strong>
                        <div style={{ marginBottom: '1rem' }}>{product.quantity !== undefined ? product.quantity : 'N/A'}</div>
                    </div>
                </div>

                {product.attributes && Object.keys(product.attributes).length > 0 && (
                    <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid #eee' }}>
                        <h4 style={{ marginBottom: '0.5rem', color: '#2c3e50' }}>Attributes</h4>
                        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(100px, auto) 1fr', gap: '0.5rem' }}>
                            {Object.entries(product.attributes).map(([key, value]) => (
                                <div key={key} style={{ display: 'contents' }}>
                                    <div style={{ fontWeight: 'bold', color: '#555', textTransform: 'capitalize' }}>{key}:</div>
                                    <div>{value}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                <div style={{ marginTop: '2rem', textAlign: 'right' }}>
                    <button onClick={onClose} style={{ padding: '8px 16px', background: '#95a5a6', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Close</button>
                </div>
            </div>
        </div>
    );
};

export default ProductDetailModal;

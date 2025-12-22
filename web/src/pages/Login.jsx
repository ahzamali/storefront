import { useState } from 'react';
import { login } from '../services/api';
import { useNavigate } from 'react-router-dom';

const Login = ({ setToken }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [targetHost, setTargetHost] = useState(localStorage.getItem('targetHost') || 'http://localhost:8080');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        localStorage.setItem('targetHost', targetHost);
        try {
            const data = await login(username, password);
            setToken(data.token);
            if (data.storeId) localStorage.setItem('storeId', data.storeId);
            else localStorage.removeItem('storeId');
            navigate('/');
        } catch (err) {
            setError('Login failed. Please check credentials.');
        }
    };

    return (
        <div style={{
            height: '100vh',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        }}>
            <div style={{
                background: 'rgba(255, 255, 255, 0.25)',
                boxShadow: '0 8px 32px 0 rgba(31, 38, 135, 0.37)',
                backdropFilter: 'blur(4px)',
                borderRadius: '10px',
                border: '1px solid rgba(255, 255, 255, 0.18)',
                padding: '3rem',
                width: '350px',
                color: 'white',
            }}>
                <h2 style={{ textAlign: 'center', marginBottom: '2rem' }}>StoreFront Admin</h2>
                {error && <p style={{ color: '#ff6b6b', textAlign: 'center' }}>{error}</p>}
                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <input
                        type="text"
                        placeholder="Username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        style={{ padding: '0.8rem', borderRadius: '5px', border: 'none', background: 'rgba(255,255,255,0.9)', color: 'black' }}
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        style={{ padding: '0.8rem', borderRadius: '5px', border: 'none', background: 'rgba(255,255,255,0.9)', color: 'black' }}
                    />
                    <input
                        type="text"
                        placeholder="Target Host (http://localhost:8080)"
                        value={targetHost}
                        onChange={(e) => setTargetHost(e.target.value)}
                        style={{ padding: '0.8rem', borderRadius: '5px', border: 'none', background: 'rgba(255,255,255,0.9)', color: 'black' }}
                    />
                    <button type="submit" style={{
                        marginTop: '1rem',
                        padding: '0.8rem',
                        background: '#35495e',
                        color: 'white',
                        border: 'none',
                        cursor: 'pointer',
                        fontWeight: 'bold'
                    }}>LOGIN</button>
                    {/* HINT: Use admin_store/pass */}
                </form>
            </div>
        </div>
    );
};

export default Login;

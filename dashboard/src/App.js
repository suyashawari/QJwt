import React, { useState, useEffect, useCallback } from 'react';
import FuelGauge from './components/FuelGauge';
import Heatmap from './components/Heatmap';
import './App.css';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

function App() {
    const [status, setStatus] = useState({
        poolSize: 0,
        status: 'LOADING',
        totalKeysGenerated: 0,
        quantumKeys: 0,
        classicalKeys: 0,
        honeyTokensInjected: 0,
        lastGeneration: null,
        healthy: false
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [lastUpdated, setLastUpdated] = useState(null);

    const fetchStatus = useCallback(async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/status`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            setStatus(data);
            setError(null);
            setLastUpdated(new Date());
        } catch (err) {
            console.error('Failed to fetch status:', err);
            setError(err.message);
            // Set demo data for development
            setStatus(prev => ({
                ...prev,
                status: 'DEMO_MODE',
                poolSize: Math.floor(Math.random() * 500) + 500,
                quantumKeys: Math.floor(Math.random() * 10000),
                classicalKeys: Math.floor(Math.random() * 1000),
                honeyTokensInjected: Math.floor(Math.random() * 50)
            }));
        } finally {
            setLoading(false);
        }
    }, []);

    // Initial fetch and polling
    useEffect(() => {
        fetchStatus();

        // Poll every 5 seconds
        const interval = setInterval(fetchStatus, 5000);

        return () => clearInterval(interval);
    }, [fetchStatus]);

    if (loading) {
        return (
            <div className="app loading-screen">
                <div className="loader">
                    <div className="quantum-spinner"></div>
                    <p>Connecting to Quantum Services...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="app">
            {/* Header */}
            <header className="app-header">
                <div className="header-content">
                    <div className="logo">
                        <span className="logo-icon">⚛️</span>
                        <h1>Quantum Auth</h1>
                        <span className="version-badge">v1.0.0</span>
                    </div>

                    <div className="header-status">
                        {error ? (
                            <span className="connection-status offline">
                                <span className="status-indicator"></span>
                                Demo Mode
                            </span>
                        ) : (
                            <span className="connection-status online">
                                <span className="status-indicator"></span>
                                Connected
                            </span>
                        )}
                        {lastUpdated && (
                            <span className="last-updated mono">
                                Updated: {lastUpdated.toLocaleTimeString()}
                            </span>
                        )}
                    </div>
                </div>
            </header>

            {/* Error Banner */}
            {error && (
                <div className="error-banner">
                    <span className="error-icon">⚠️</span>
                    <span>API Connection Failed: {error}. Showing demo data.</span>
                </div>
            )}

            {/* Main Dashboard */}
            <main className="dashboard">
                <div className="dashboard-grid">
                    {/* Fuel Gauge */}
                    <div className="dashboard-item gauge-section">
                        <FuelGauge
                            poolSize={status.poolSize}
                            maxSize={2000}
                            status={status.status}
                        />
                    </div>

                    {/* Heatmap */}
                    <div className="dashboard-item heatmap-section">
                        <Heatmap
                            quantumKeys={status.quantumKeys}
                            classicalKeys={status.classicalKeys}
                            honeyTokens={status.honeyTokensInjected}
                            lastGeneration={status.lastGeneration}
                        />
                    </div>

                    {/* Quick Stats */}
                    <div className="dashboard-item stats-section">
                        <div className="quick-stats">
                            <h3>System Overview</h3>

                            <div className="stat-row">
                                <span className="stat-name">Total Keys Generated</span>
                                <span className="stat-value mono">{status.totalKeysGenerated?.toLocaleString() || '0'}</span>
                            </div>

                            <div className="stat-row">
                                <span className="stat-name">Key Source</span>
                                <span className="stat-value">
                                    {status.quantumKeys > status.classicalKeys ? (
                                        <span className="source-quantum">⚛️ Quantum Primary</span>
                                    ) : (
                                        <span className="source-classical">🔐 Classical Fallback</span>
                                    )}
                                </span>
                            </div>

                            <div className="stat-row">
                                <span className="stat-name">Security Status</span>
                                <span className="stat-value">
                                    <span className="security-status secure">🛡️ Secure</span>
                                </span>
                            </div>

                            <div className="stat-row">
                                <span className="stat-name">Honey Traps Active</span>
                                <span className="stat-value mono">{status.honeyTokensInjected || 0}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="app-footer">
                <p>
                    Quantum-Seeded Authentication Framework •
                    Powered by <span className="text-gradient">IBM Quantum</span> + Spring Boot
                </p>
            </footer>
        </div>
    );
}

export default App;

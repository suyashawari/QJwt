import React, { useState, useEffect } from 'react';
import EntropyGauge from './components/EntropyGauge';
import TokenLifecycleChart from './components/TokenLifecycleChart';
import AlertFeed from './components/AlertFeed';
import ThreatMap from './components/ThreatMap';
import { fetchMetrics } from './services/metricsService';

function App() {
    const [metrics, setMetrics] = useState({
        poolSize: 0,
        maxPoolSize: 1000,
        keysFromQuantum: 0,
        keysFromClassical: 0,
        tokensGenerated: 0,
        tokensValidated: 0,
        tokensFailed: 0,
        alerts: [],
        minerUptime: '—',
        lastMineTime: '—',
    });

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadMetrics = async () => {
            try {
                const data = await fetchMetrics();
                setMetrics(data);
                setError(null);
            } catch (err) {
                setError('Failed to connect to metrics API');
                // Use demo data so the dashboard still looks good
                setMetrics({
                    poolSize: 347,
                    maxPoolSize: 1000,
                    keysFromQuantum: 1240,
                    keysFromClassical: 89,
                    tokensGenerated: 5621,
                    tokensValidated: 5413,
                    tokensFailed: 12,
                    alerts: [
                        { id: 1, level: 'WARNING', message: 'Entropy pool below 30%', time: '14:32:01' },
                        { id: 2, level: 'CRITICAL', message: 'Honey token triggered — kid: a3f2…', time: '14:28:55' },
                        { id: 3, level: 'INFO', message: 'Quantum source reconnected to ibm_brisbane', time: '14:15:22' },
                    ],
                    minerUptime: '12h 34m',
                    lastMineTime: '2s ago',
                });
            } finally {
                setLoading(false);
            }
        };

        loadMetrics();
        const interval = setInterval(loadMetrics, 5000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="dashboard">
            {/* Header */}
            <header className="dashboard-header">
                <div className="header-brand">
                    <span className="header-icon">⚛️</span>
                    <h1>Quantum JWT Dashboard</h1>
                </div>
                <div className="header-meta">
                    <span className={`status-dot ${error ? 'offline' : 'online'}`}></span>
                    <span>{error ? 'Offline (Demo Data)' : 'Live'}</span>
                </div>
            </header>

            {/* Stats Row */}
            <div className="stats-row">
                <div className="stat-card quantum">
                    <div className="stat-value">{metrics.keysFromQuantum.toLocaleString()}</div>
                    <div className="stat-label">Quantum Keys Mined</div>
                </div>
                <div className="stat-card classical">
                    <div className="stat-value">{metrics.keysFromClassical.toLocaleString()}</div>
                    <div className="stat-label">Classical Keys (Fallback)</div>
                </div>
                <div className="stat-card tokens">
                    <div className="stat-value">{metrics.tokensGenerated.toLocaleString()}</div>
                    <div className="stat-label">Tokens Generated</div>
                </div>
                <div className="stat-card validated">
                    <div className="stat-value">{metrics.tokensValidated.toLocaleString()}</div>
                    <div className="stat-label">Tokens Validated</div>
                </div>
                <div className="stat-card failed">
                    <div className="stat-value">{metrics.tokensFailed}</div>
                    <div className="stat-label">Validation Failures</div>
                </div>
            </div>

            {/* Main Grid */}
            <div className="dashboard-grid">
                <div className="grid-item gauge-section">
                    <h2>Entropy Pool</h2>
                    <EntropyGauge current={metrics.poolSize} max={metrics.maxPoolSize} />
                    <div className="gauge-footer">
                        <span>Miner Uptime: {metrics.minerUptime}</span>
                        <span>Last Mine: {metrics.lastMineTime}</span>
                    </div>
                </div>

                <div className="grid-item chart-section">
                    <h2>Token Lifecycle</h2>
                    <TokenLifecycleChart
                        generated={metrics.tokensGenerated}
                        validated={metrics.tokensValidated}
                        failed={metrics.tokensFailed}
                    />
                </div>

                <div className="grid-item alerts-section">
                    <h2>Security Alerts</h2>
                    <AlertFeed alerts={metrics.alerts} />
                </div>

                <div className="grid-item threat-section">
                    <h2>Key Source Distribution</h2>
                    <ThreatMap
                        quantum={metrics.keysFromQuantum}
                        classical={metrics.keysFromClassical}
                    />
                </div>
            </div>
        </div>
    );
}

export default App;

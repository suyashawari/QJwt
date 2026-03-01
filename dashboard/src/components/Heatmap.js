import React, { useMemo } from 'react';
import './Heatmap.css';

/**
 * Heatmap Component
 * 
 * Visualizes security events and key generation activity.
 * Shows distribution of quantum vs classical keys and alerts.
 */
const Heatmap = ({
    quantumKeys = 0,
    classicalKeys = 0,
    honeyTokens = 0,
    lastGeneration = null
}) => {
    // Generate heatmap data (simulated activity grid)
    const heatmapData = useMemo(() => {
        const total = quantumKeys + classicalKeys;
        const quantumRatio = total > 0 ? quantumKeys / total : 0;

        // Create 7x24 grid (7 days x 24 hours)
        const grid = [];
        for (let day = 0; day < 7; day++) {
            const row = [];
            for (let hour = 0; hour < 24; hour++) {
                // Simulate activity based on actual data
                const baseActivity = Math.random() * 0.3;
                const quantum = quantumRatio > 0 ? Math.random() * quantumRatio : 0;
                const intensity = Math.min(baseActivity + quantum + Math.random() * 0.2, 1);
                row.push({
                    intensity,
                    isQuantum: Math.random() < quantumRatio,
                    hour,
                    day
                });
            }
            grid.push(row);
        }
        return grid;
    }, [quantumKeys, classicalKeys]);

    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const hours = Array.from({ length: 24 }, (_, i) => i);

    const totalKeys = quantumKeys + classicalKeys;
    const quantumPercentage = totalKeys > 0 ? Math.round((quantumKeys / totalKeys) * 100) : 0;

    const getIntensityColor = (intensity, isQuantum) => {
        if (intensity < 0.1) return 'rgba(255, 255, 255, 0.02)';

        if (isQuantum) {
            // Quantum: Cyan gradient
            const alpha = 0.2 + (intensity * 0.8);
            return `rgba(0, 245, 212, ${alpha})`;
        } else {
            // Classical: Purple gradient
            const alpha = 0.2 + (intensity * 0.8);
            return `rgba(123, 44, 191, ${alpha})`;
        }
    };

    const formatLastGeneration = (timestamp) => {
        if (!timestamp) return 'Never';
        try {
            const date = new Date(timestamp);
            return date.toLocaleString();
        } catch {
            return timestamp;
        }
    };

    return (
        <div className="heatmap">
            <div className="heatmap-header">
                <span className="heatmap-icon">🔥</span>
                <h2>Activity Heatmap</h2>
            </div>

            <div className="heatmap-stats">
                <div className="stat-card stat-quantum">
                    <div className="stat-value mono">{quantumKeys.toLocaleString()}</div>
                    <div className="stat-label">Quantum Keys</div>
                    <div className="stat-bar">
                        <div
                            className="stat-bar-fill quantum"
                            style={{ width: `${quantumPercentage}%` }}
                        ></div>
                    </div>
                </div>

                <div className="stat-card stat-classical">
                    <div className="stat-value mono">{classicalKeys.toLocaleString()}</div>
                    <div className="stat-label">Classical Keys</div>
                    <div className="stat-bar">
                        <div
                            className="stat-bar-fill classical"
                            style={{ width: `${100 - quantumPercentage}%` }}
                        ></div>
                    </div>
                </div>

                <div className="stat-card stat-honey">
                    <div className="stat-value mono">{honeyTokens.toLocaleString()}</div>
                    <div className="stat-label">🍯 Honey Tokens</div>
                </div>
            </div>

            <div className="heatmap-grid-container">
                <div className="heatmap-hours">
                    {hours.filter(h => h % 4 === 0).map(hour => (
                        <span key={hour} className="hour-label">{hour}:00</span>
                    ))}
                </div>

                <div className="heatmap-grid">
                    {heatmapData.map((row, dayIndex) => (
                        <div key={dayIndex} className="heatmap-row">
                            <span className="day-label">{days[dayIndex]}</span>
                            <div className="heatmap-cells">
                                {row.map((cell, hourIndex) => (
                                    <div
                                        key={hourIndex}
                                        className="heatmap-cell"
                                        style={{
                                            backgroundColor: getIntensityColor(cell.intensity, cell.isQuantum),
                                            boxShadow: cell.intensity > 0.7
                                                ? `0 0 8px ${cell.isQuantum ? 'rgba(0, 245, 212, 0.5)' : 'rgba(123, 44, 191, 0.5)'}`
                                                : 'none'
                                        }}
                                        title={`${days[dayIndex]} ${hourIndex}:00 - ${cell.isQuantum ? 'Quantum' : 'Classical'}`}
                                    />
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <div className="heatmap-legend">
                <div className="legend-item">
                    <span className="legend-color quantum"></span>
                    <span>Quantum</span>
                </div>
                <div className="legend-item">
                    <span className="legend-color classical"></span>
                    <span>Classical</span>
                </div>
                <div className="legend-item">
                    <span className="legend-color low"></span>
                    <span>Low Activity</span>
                </div>
            </div>

            <div className="heatmap-footer">
                <span className="last-update">
                    Last generation: {formatLastGeneration(lastGeneration)}
                </span>
            </div>
        </div>
    );
};

export default Heatmap;

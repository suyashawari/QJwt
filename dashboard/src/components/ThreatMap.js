import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';

/**
 * ThreatMap — pie chart showing key source distribution (quantum vs classical).
 */
export default function ThreatMap({ quantum, classical }) {
    const total = quantum + classical;
    const data = [
        { name: 'Quantum', value: quantum },
        { name: 'Classical', value: classical },
    ];
    const COLORS = ['#8b5cf6', '#3b82f6'];

    const pctQuantum = total > 0 ? Math.round((quantum / total) * 100) : 0;
    const pctClassical = total > 0 ? Math.round((classical / total) * 100) : 0;

    return (
        <div className="pie-container">
            <ResponsiveContainer width={180} height={180}>
                <PieChart>
                    <Pie
                        data={data}
                        cx="50%"
                        cy="50%"
                        innerRadius={50}
                        outerRadius={75}
                        paddingAngle={4}
                        dataKey="value"
                    >
                        {data.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index]} />
                        ))}
                    </Pie>
                    <Tooltip
                        contentStyle={{
                            background: '#1a1f2e',
                            border: '1px solid #2d3548',
                            borderRadius: 8,
                            color: '#e2e8f0',
                        }}
                    />
                </PieChart>
            </ResponsiveContainer>

            <div className="pie-legend">
                <div className="legend-item">
                    <div className="legend-dot" style={{ background: COLORS[0] }}></div>
                    <span className="legend-value">{pctQuantum}%</span>
                    <span className="legend-label">Quantum ({quantum.toLocaleString()})</span>
                </div>
                <div className="legend-item">
                    <div className="legend-dot" style={{ background: COLORS[1] }}></div>
                    <span className="legend-value">{pctClassical}%</span>
                    <span className="legend-label">Classical ({classical.toLocaleString()})</span>
                </div>
            </div>
        </div>
    );
}

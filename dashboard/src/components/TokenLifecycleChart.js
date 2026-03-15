import React from 'react';
import { BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

/**
 * TokenLifecycleChart — bar chart comparing generated, validated, and failed tokens.
 */
export default function TokenLifecycleChart({ generated, validated, failed }) {
    const data = [
        { name: 'Generated', value: generated, fill: '#22c55e' },
        { name: 'Validated', value: validated, fill: '#06b6d4' },
        { name: 'Failed', value: failed, fill: '#ef4444' },
    ];

    return (
        <ResponsiveContainer width="100%" height={250}>
            <BarChart data={data} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2d3548" />
                <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 13 }} />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 13 }} />
                <Tooltip
                    contentStyle={{
                        background: '#1a1f2e',
                        border: '1px solid #2d3548',
                        borderRadius: 8,
                        color: '#e2e8f0',
                    }}
                />
                <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                    {data.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.fill} />
                    ))}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}

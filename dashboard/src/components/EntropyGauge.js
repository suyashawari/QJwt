import React from 'react';

/**
 * EntropyGauge — semi-circular gauge showing pool fill level.
 */
export default function EntropyGauge({ current, max }) {
    const pct = max > 0 ? Math.min(current / max, 1) : 0;
    const displayPct = Math.round(pct * 100);

    // SVG arc math — semicircle from 180° to 0°
    const radius = 80;
    const circumference = Math.PI * radius; // half-circle
    const offset = circumference * (1 - pct);

    // Color based on fill level
    let strokeColor;
    if (pct > 0.5) strokeColor = '#22c55e';
    else if (pct > 0.2) strokeColor = '#eab308';
    else strokeColor = '#ef4444';

    return (
        <div className="gauge-container">
            <svg className="gauge-svg" viewBox="0 0 200 120">
                {/* Background track */}
                <path
                    className="gauge-track"
                    d="M 20 110 A 80 80 0 0 1 180 110"
                />
                {/* Filled arc */}
                <path
                    className="gauge-fill"
                    d="M 20 110 A 80 80 0 0 1 180 110"
                    stroke={strokeColor}
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                />
                {/* Percentage text */}
                <text className="gauge-text" x="100" y="95">
                    {displayPct}%
                </text>
                <text className="gauge-label" x="100" y="115">
                    {current.toLocaleString()} / {max.toLocaleString()} keys
                </text>
            </svg>
        </div>
    );
}

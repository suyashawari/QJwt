import React, { useState, useEffect, useRef } from 'react';
import './FuelGauge.css';

/**
 * FuelGauge Component
 * 
 * Displays the current quantum entropy pool level as an animated gauge.
 * Color changes based on pool health:
 * - Green (>500): Healthy
 * - Yellow (100-500): Low
 * - Red (<100): Critical
 */
const FuelGauge = ({ poolSize = 0, maxSize = 2000, status = 'UNKNOWN' }) => {
    const [animatedValue, setAnimatedValue] = useState(0);
    const canvasRef = useRef(null);

    // Animate the value change
    useEffect(() => {
        const duration = 1000;
        const startValue = animatedValue;
        const endValue = poolSize;
        const startTime = performance.now();

        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            // Easing function
            const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);
            const easedProgress = easeOutCubic(progress);

            setAnimatedValue(Math.round(startValue + (endValue - startValue) * easedProgress));

            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }, [poolSize]);

    // Draw the gauge
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const centerX = canvas.width / 2;
        const centerY = canvas.height / 2;
        const radius = Math.min(centerX, centerY) - 20;

        // Clear canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Calculate percentage
        const percentage = Math.min((animatedValue / maxSize) * 100, 100);
        const angle = (percentage / 100) * 270; // 270 degrees arc

        // Determine color based on status
        let gradient;
        if (percentage > 50) {
            gradient = ctx.createLinearGradient(0, canvas.height, canvas.width, 0);
            gradient.addColorStop(0, '#00f5d4');
            gradient.addColorStop(1, '#00b890');
        } else if (percentage > 10) {
            gradient = ctx.createLinearGradient(0, canvas.height, canvas.width, 0);
            gradient.addColorStop(0, '#ffd60a');
            gradient.addColorStop(1, '#ff9500');
        } else {
            gradient = ctx.createLinearGradient(0, canvas.height, canvas.width, 0);
            gradient.addColorStop(0, '#ff006e');
            gradient.addColorStop(1, '#ff4444');
        }

        // Draw background arc
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, Math.PI * 0.75, Math.PI * 2.25, false);
        ctx.strokeStyle = '#1a1a25';
        ctx.lineWidth = 20;
        ctx.lineCap = 'round';
        ctx.stroke();

        // Draw progress arc
        if (angle > 0) {
            ctx.beginPath();
            const startAngle = Math.PI * 0.75;
            const endAngle = startAngle + (angle * Math.PI / 180);
            ctx.arc(centerX, centerY, radius, startAngle, endAngle, false);
            ctx.strokeStyle = gradient;
            ctx.lineWidth = 20;
            ctx.lineCap = 'round';
            ctx.stroke();

            // Add glow effect
            ctx.shadowColor = percentage > 50 ? '#00f5d4' : percentage > 10 ? '#ffd60a' : '#ff006e';
            ctx.shadowBlur = 20;
            ctx.stroke();
            ctx.shadowBlur = 0;
        }

    }, [animatedValue, maxSize]);

    const percentage = Math.round((animatedValue / maxSize) * 100);

    const getStatusColor = () => {
        switch (status) {
            case 'HEALTHY': return '#00f5d4';
            case 'LOW': return '#ffd60a';
            case 'CRITICAL': return '#ff006e';
            case 'EXHAUSTED': return '#ff4444';
            default: return '#606080';
        }
    };

    return (
        <div className="fuel-gauge">
            <div className="gauge-header">
                <span className="gauge-icon">⚛️</span>
                <h2>Entropy Pool</h2>
            </div>

            <div className="gauge-container">
                <canvas
                    ref={canvasRef}
                    width={280}
                    height={280}
                    className="gauge-canvas"
                />

                <div className="gauge-center">
                    <div className="gauge-value mono">{animatedValue.toLocaleString()}</div>
                    <div className="gauge-label">keys available</div>
                    <div className="gauge-percentage">{percentage}%</div>
                </div>
            </div>

            <div className="gauge-status" style={{ '--status-color': getStatusColor() }}>
                <span className="status-dot"></span>
                <span className="status-text">{status}</span>
            </div>

            <div className="gauge-thresholds">
                <div className="threshold threshold-low">
                    <span className="threshold-marker" style={{ backgroundColor: '#ff006e' }}></span>
                    <span>&lt;100 Critical</span>
                </div>
                <div className="threshold threshold-medium">
                    <span className="threshold-marker" style={{ backgroundColor: '#ffd60a' }}></span>
                    <span>100-500 Low</span>
                </div>
                <div className="threshold threshold-high">
                    <span className="threshold-marker" style={{ backgroundColor: '#00f5d4' }}></span>
                    <span>&gt;500 Healthy</span>
                </div>
            </div>
        </div>
    );
};

export default FuelGauge;

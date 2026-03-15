import React from 'react';

/**
 * AlertFeed — scrollable list of security events.
 */
export default function AlertFeed({ alerts }) {
    if (!alerts || alerts.length === 0) {
        return <div className="alert-empty">No security alerts — all clear ✅</div>;
    }

    return (
        <div className="alert-feed">
            {alerts.map((alert) => (
                <div className="alert-item" key={alert.id}>
                    <span className={`alert-badge ${alert.level}`}>{alert.level}</span>
                    <span className="alert-message">{alert.message}</span>
                    <span className="alert-time">{alert.time}</span>
                </div>
            ))}
        </div>
    );
}

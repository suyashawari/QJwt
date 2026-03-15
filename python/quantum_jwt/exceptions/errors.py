"""
Convenience re-exports and error helper utilities.

All exception classes are defined in ``__init__.py`` of this package.
This module provides additional helpers for structured error reporting.
"""

from . import (
    QuantumJWTException,
    EntropyExhaustedError,
    InvalidTokenError,
    HoneyTokenError,
)

__all__ = [
    "QuantumJWTException",
    "EntropyExhaustedError",
    "InvalidTokenError",
    "HoneyTokenError",
    "security_alert",
]


def security_alert(error: QuantumJWTException, *, kid: str = "", ip: str = "") -> dict:
    """
    Build a structured alert dict suitable for logging / metrics.

    >>> security_alert(HoneyTokenError("breach"), kid="abc-123", ip="10.0.0.1")
    {'level': 'CRITICAL', 'type': 'HoneyTokenError', 'message': 'breach', 'kid': 'abc-123', 'ip': '10.0.0.1'}
    """
    level = "CRITICAL" if isinstance(error, HoneyTokenError) else "WARNING"
    return {
        "level": level,
        "type": type(error).__name__,
        "message": str(error),
        "kid": kid,
        "ip": ip,
    }

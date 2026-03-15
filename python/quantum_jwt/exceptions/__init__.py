"""Custom exceptions for the Quantum JWT client."""

class QuantumJWTException(Exception):
    """Base exception for all Quantum JWT errors."""
    pass

class EntropyExhaustedError(QuantumJWTException):
    """Raised when the entropy pool is empty."""
    pass

class InvalidTokenError(QuantumJWTException):
    """Raised when token validation fails."""
    pass

class HoneyTokenError(InvalidTokenError):
    """Raised when a honey token is detected."""
    pass